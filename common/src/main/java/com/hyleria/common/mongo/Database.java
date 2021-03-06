package com.hyleria.common.mongo;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hyleria.common.account.Account;
import com.hyleria.common.config.ConfigurationProvider;
import com.hyleria.common.inject.StartParallel;
import com.hyleria.common.mongo.codec.ExtraCodecs;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.eq;


/**
 * In charge of managing the connections,
 * and caching of our persistent account.
 *
 * @author Ben (OutdatedVersion)
 * @since Dec/08/2016 (8:07 PM)
 */
@Singleton
@StartParallel
public class Database
{

    /** our one and only Mongo client */
    private final MongoClient client;

    /** the only Mongo account we're using */
    public final MongoDatabase mongo;

    /** the collection accounts are stored in (used for most things) */
    public final MongoCollection<Document> accounts;

    /** local cache for accounts | it is crucial that you properly handle the invalidation of items here. */
    private Cache<UUID, Account> accountCache;

    /** run all account requests async */
    private ExecutorService executor;

    @Inject
    public Database(ConfigurationProvider provider)
    {
        final DatabaseConfig _config = provider.read("database/{env}", DatabaseConfig.class);
        final MongoClientOptions.Builder _builder = new MongoClientOptions.Builder();


        // setup custom BSON codecs for the mongo driver
        final CodecRegistry _default = MongoClient.getDefaultCodecRegistry();
        final CodecRegistry _fresh = CodecRegistries.fromCodecs(ExtraCodecs.HYLERIA_CODECS);

        _builder.codecRegistry(CodecRegistries.fromRegistries(_default, _fresh));


        client = new MongoClient(new ServerAddress(_config.connection.host, _config.connection.port),
                                 Collections.singletonList(MongoCredential.createCredential(_config.auth.username, _config.database, _config.auth.password.toCharArray())),
                                 _builder.build());

        mongo = client.getDatabase(_config.database);
        accounts = mongo.getCollection(_config.collection);

        executor = Executors.newCachedThreadPool();

        if (_config.cacheSpecification != null)
            accountCache = CacheBuilder.from(_config.cacheSpecification).build();
        else
            accountCache = CacheBuilder.newBuilder().build();
    }

    /**
     * Unbind the allocated resources for
     * this database instance.
     *
     * Note: Our {@link #executor} will finish executing
     * the set of tasks it currently has queued.
     */
    public void releaseResources()
    {
        client.close();
        executor.shutdown();
    }

    /**
     * @param runnable the task to run
     * @return a future for this task
     */
    public Future submitTask(Runnable runnable)
    {
        return executor.submit(runnable);
    }

    /**
     * @return our local cache containing loaded accounts
     */
    public Cache<UUID, Account> cache()
    {
        return accountCache;
    }

    /**
     * Checks is a player is in our cache
     *
     * @param username the username bound to the account
     *                 that we're looking for
     *
     * @return yes ({@code true} or no ({@code false})
     */
    public boolean cacheContains(String username)
    {
        return accountCache.asMap().entrySet().stream().anyMatch(entry -> entry.getValue().username().equals(username));
    }

    /**
     * Check if a player (by UUID)
     * is in our cache
     *
     * @param uuid the UUID bound to the account
     *             that we're looking for
     *
     * @return yes or no
     */
    public boolean cacheContains(UUID uuid)
    {
        return accountCache.asMap().containsKey(uuid);
    }

    /**
     * Removes the specifies account (found by UUID)
     * from our cache. Assumes they're in it.
     *
     * @param uuid the player's UUID
     * @return the
     */
    public Database cacheInvalidate(UUID uuid)
    {
        accountCache.invalidate(uuid);
        return this;
    }

    /**
     * Inserts an account into our cache
     *
     * @param account the account
     * @return the account that was just inserted
     */
    public Account cacheCommit(Account account)
    {
        accountCache.put(account.uuid(), account);
        return account;
    }

    /**
     * Grabs an account from our cache wrapped
     * in an {@link Optional}.
     *
     * @param username the username
     * @return the account or an empty Optional
     */
    public Optional<Account> cacheFetch(String username)
    {
        return accountCache.asMap().entrySet()
                                   .stream()
                                   .filter(entry -> entry.getValue().username().equalsIgnoreCase(username))
                                   .findFirst()
                                   .flatMap(entry -> Optional.of(entry.getValue()));
    }

    /**
     * Grabs an account from out cache based
     * on a player's UUID.
     *
     * @param uuid the UUID
     * @return an account wrapped in an {@link Optional}
     */
    public Optional<Account> cacheFetch(UUID uuid)
    {
        return Optional.ofNullable(accountCache.getIfPresent(uuid));
    }

    /**
     * Grab an account from our account via
     * a username.
     *
     * @param username the username
     * @return The account we're requesting
     *         wrapped in an {@link Optional}
     */
    public Future<Optional<Account>> fetchAccount(String username)
    {
        return fetchAccount(null, username, true, true);
    }

    /**
     * Grab an account from our account via
     * the UUID provided.
     *
     * @param uuid the UUID
     * @param callback our account
     * @return The account we're working with
     *         wrapped in an {@link Optional}
     */
    public Future<Optional<Account>> fetchAccount(UUID uuid, Consumer<Optional<Account>> callback)
    {
        return fetchAccount(uuid, null, true, true);
    }

    /**
     * Grab an account from our account
     * synchronously. Probably only ever
     * going to be used when someone is
     * logging into a server.
     *
     * @param uuid the UUID of the player
     * @return The account we've requested
     *         wrapped in an {@link Optional}
     */
    public Optional<Account> fetchAccountSync(UUID uuid)
    {
        return fetchAccount(uuid, null, true, false);
    }

    /**
     * Internal Method
     *
     * <p>
     * Loads an account from our main Mongo
     * account & wraps it in an {@linkplain Optional}
     * or returns an empty one if the player
     * doesn't exist within the account collection.
     *
     * <p>
     * This method takes both a username & UUID.
     * Saves on some duplicated code.
     * Use the overloaded methods instead.
     *
     * @param uuid if we're looking someone up by UUID..
     * @param username if we're looking someone up by username..
     * @param useCache do we want to check our cache?
     * @return what we were looking for. absolutely no
     *         real verification goes into the return value for
     *         this method. purely up to proper implementation.
     */
    @SuppressWarnings ( "unchecked" )
    private <R> R fetchAccount(UUID uuid, String username, boolean useCache, boolean async)
    {
        boolean _useUsername = uuid == null && username != null;

        final Callable<Optional<Account>> _transaction = () ->
        {
            // if we request to we'll attempt to grab this
            // account from our local cache before making
            // a lengthy request to our account
            if (useCache)
            {
                Optional<Account> _cacheHit;

                if (_useUsername)
                    _cacheHit = cacheFetch(username);
                else
                    _cacheHit = cacheFetch(uuid);

                if (_cacheHit.isPresent())
                    return _cacheHit;
            }


            // hit up mongo
            Document _document = accounts.find(!_useUsername
                                               ? eq("uuid", uuid.toString())
                                               : eq("name_lower", username.toLowerCase())).limit(1).first();

            return _document == null ? Optional.empty()
                                     : Optional.of(new Account().populateFromDocument(_document));
        };


        try
        {
            if (async)
                return (R) executor.submit(_transaction);
            else
                return (R) Futures.immediateFuture(_transaction.call());
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        throw new RuntimeException("Something went seriously wrong whilst processing that request.");
    }

}
