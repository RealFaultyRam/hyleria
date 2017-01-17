package net.revellionmc.network;

import com.google.inject.Inject;
import net.revellionmc.network.database.Account;
import net.revellionmc.network.database.Database;
import net.revellionmc.util.Issues;
import net.revellionmc.util.Module;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;


/**
 * In charge of loading & removing
 * accounts.
 *
 * @author Ben (OutdatedVersion)
 * @since Dec/11/2016 (6:54 PM)
 */
public class AccountManager extends Module
{

    /** allows us to grab info from our mongo instance */
    @Inject
    private Database database;

    /**
     * Attempts to grab an account by
     * a Bukkit {@link Player}.
     *
     * @param player the player
     * @return an account for that player
     */
    public Account grab(Player player)
    {
        return grab(player.getUniqueId());
    }

    /**
     * Attempts to find the account of
     * a player via a username
     *
     * @param name the name of the player
     * @return the account bound to that name
     */
    public Account grab(String name)
    {
        return database.cacheFetch(name).orElseThrow(() -> new RuntimeException("Missing account for [" + name + "]"));
    }

    /**
     * Attempts to grab the account
     * for the provided player. When
     * we fail to do so an exception
     * will be thrown.
     *
     * @param uuid UUID of the player's account
     *             that we're looking for
     * @return the account
     */
    public Account grab(UUID uuid)
    {
        return database.cacheFetch(uuid).orElseThrow(() -> new RuntimeException("Missing account for [" + uuid.toString() + "]"));
    }

    @EventHandler
    public void handleLogin(AsyncPlayerPreLoginEvent event)
    {
        try
        {
            database.fetchAccountSync(event.getUniqueId(), callback ->
            {
                // they've been on before..
                if (callback.isPresent())
                {
                    final Account _account = callback.get();

                    database.cacheCommit(_account);
                    // TODO(Ben): update account
                }
                // insert into database
                else
                {
                    // idk
                }
            });
        }
        catch (Exception ex)
        {
            Issues.handle("Player Login", ex);
        }
    }

    @EventHandler
    public void cleanupCache(PlayerQuitEvent event)
    {
        database.cacheInvalidate(event.getPlayer().getUniqueId());
    }

}