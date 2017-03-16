package com.hyleria.coeus;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hyleria.Hyleria;
import com.hyleria.coeus.damage.DamageEventFactory;
import com.hyleria.coeus.event.StatusChangeEvent;
import com.hyleria.coeus.scoreboard.ScoreboardHandler;
import com.hyleria.common.backend.ServerConfig;
import com.hyleria.network.AccountManager;
import com.hyleria.util.Module;
import com.hyleria.util.PlayerUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * @author Ben (OutdatedVersion)
 * @since Mar/08/2017 (5:40 PM)
 */
@Singleton
public class Coeus extends Module
{

    /** the type of the game we're playing */
    private GameChoice gameChoice;

    /** the only game that's running */
    private Game game;

    /** the status of the game */
    private Status status;

    @Inject
    public Coeus(Hyleria plugin, ServerConfig config, AccountManager accountManager)
    {
        status = Status.INIT;  // we don't need to call the event quite yet

        gameChoice = GameChoice.valueOf(config.forcedGame.toUpperCase());
        game = plugin.boundInjection(gameChoice.clazz);

        // load up the basic game
        game.init();

        // ready for players
        // the above operation is intended to be fully blocking
        updateStatus(Status.IDLE);


        new DamageEventFactory(game).init(plugin);
        new ScoreboardHandler().game(game).init(plugin, this).initNametags(plugin, accountManager);
    }

    /**
     * Updates the status of the current
     * match to the provided status. We
     * also send out a notification-only
     * event to let us interact with it.
     *
     * @param to the new status
     * @return this
     */
    public Coeus updateStatus(Status to)
    {
        new StatusChangeEvent(status, to).call();

        this.status = to;

        return this;
    }

    @EventHandler
    public void watchJoin(PlayerJoinEvent event)
    {
        if (status == Status.IDLE)
        {
            if (PlayerUtil.onlineCount() >= game.requiredPlayerCount)
            {
                // updateStatus(Status.COUNTDOWN);
                // for now we'll just start it
                game.begin();
            }
        }
    }

}