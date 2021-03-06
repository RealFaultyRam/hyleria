package com.hyleria.common.account;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;
import com.hyleria.common.mongo.Database;
import com.hyleria.common.mongo.document.DocumentBuilder;
import com.hyleria.common.mongo.document.DocumentCompatible;
import com.hyleria.common.reference.Role;
import org.bson.Document;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;


/**
 * @author Ben (OutdatedVersion)
 * @since Dec/08/2016 (8:15 PM)
 */
@SuppressWarnings ( "unchecked" )
public class Account implements DocumentCompatible
{

    private Document raw;
    private Map<String, Object> customData = Maps.newHashMap();

    private UUID uuid;

    private String name;

    @SerializedName ( "previous_names" )
    private List<String> previousUsernames = Lists.newArrayList();

    private Role role = Role.PLAYER;

    // TODO(Ben): currency & XP?

    @SerializedName ( "current_address" )
    private String currentIP;

    @SerializedName ( "previous_addresses" )
    private List<PreviousAddress> previousAddresses = Lists.newArrayList();

    /**
     * @return {@link #uuid}
     */
    public UUID uuid()
    {
        return uuid;
    }


    /**
     * @return {@link #name}
     */
    public String username()
    {
        return name;
    }

    /**
     * @return {@link #role}
     */
    public Role role()
    {
        return role;
    }

    /**
     * Grab a value from the raw document
     *
     * @param key where it was stored
     * @param type the type of said thing
     * @param <T> the type of value we're looking for
     * @return the thing
     */
    public <T> T val(String key, Class<T> type)
    {
        return raw.get(key, type);
    }

    /**
     * Add a custom value into our account document
     *
     * @param key key of the value
     * @param val custom thingy
     * @return this account
     */
    public Account addVal(String key, Object val)
    {
        customData.put(key, val);
        return this;
    }

    /**
     * Checks whether or not the provided
     * item is contained in this account
     *
     * @param key where the data is stored
     * @return yes or no
     */
    public boolean isPresent(String key)
    {
        return raw.containsKey(key);
    }

    /**
     * Update this player's role in the database
     *
     * @param newRole the new role
     * @param database database instance
     * @return this account
     */
    public Account role(Role newRole, Database database)
    {
        this.role = newRole;

        database.submitTask(() -> database.accounts.updateOne(eq("uuid", this.uuid.toString()), set("role", newRole.name())));

        return this;
    }

    /**
     * Represents some other IP that
     * someone logged in from.
     */
    public static class PreviousAddress
    {
        /** the IP */
        public String value;

        /** the UNIX epoch timestamp we last saw this on */
        public long lastUsedOn;

        public PreviousAddress(String value, long lastUsedOn)
        {
            this.value = value;
            this.lastUsedOn = lastUsedOn;
        }
    }

    @Override
    public Document asDocument()
    {
        return DocumentBuilder.create()
                .withFreshDoc()
                .skipOver("uuid", "customData", "raw")  // we want to handle the uuid on our own
                .appendAllFields(this)
                .append("uuid", this.uuid.toString())
                .append("name_lower", this.name.toLowerCase())
                .append(doc -> doc.putAll(customData))
                .finished();
    }

    @Override
    public Account populateFromDocument(Document document)
    {
        this.uuid = UUID.fromString(document.getString("uuid"));
        this.name = document.getString("name");
        this.role = Role.valueOf(document.getString("role"));
        this.previousUsernames = document.get("previous_names", List.class);
        this.currentIP = document.getString("current_address");
        this.previousAddresses = document.get("previous_addresses", List.class);
        this.raw = document;

        return this;
    }

    /**
     * Turns the provided data, retrieved
     * from a client when they login, into
     * a basic player {@link Account}.
     *
     * @param uuid the player's UUID
     * @param name the player's username
     * @param ip the player's IP address
     * @return the new account
     */
    public static Account fromLoginData(UUID uuid, String name, String ip)
    {
        final Account _fresh = new Account();

        _fresh.uuid = uuid;
        _fresh.name = name;
        _fresh.currentIP = ip;

        return _fresh;
    }

}
