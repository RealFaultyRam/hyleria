package com.hyleria.command.api.data;

import com.hyleria.common.account.Account;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * @author Ben (OutdatedVersion)
 * @since Mar/31/2017 (10:38 PM)
 */
public class OnlineOfflinePlayer
{

    /** we'll store this value */
    private Player onlinePlayer;

    /** the data of our player */
    private Account account;

    /**
     * @param onlinePlayer the player
     * @param account an account
     */
    public OnlineOfflinePlayer(Player onlinePlayer, Account account)
    {
        this.onlinePlayer = onlinePlayer;
        this.account = account;
    }

    /**
     * @return the player
     */
    public Player online()
    {
        if (onlinePlayer == null)
            onlinePlayer = Bukkit.getPlayer(account.uuid());

        return onlinePlayer;
    }

    /**
     * @return our account (always here)
     */
    public Account offline()
    {
        return account;
    }

    /**
     * @return whether or not the specified
     *         player is on this server
     */
    public boolean isOnline()
    {
        return online() != null;
    }

}
