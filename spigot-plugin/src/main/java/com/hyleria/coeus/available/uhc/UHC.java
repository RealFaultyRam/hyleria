package com.hyleria.coeus.available.uhc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hyleria.Hyleria;
import com.hyleria.coeus.Game;
import com.hyleria.coeus.available.uhc.world.Border;
import com.hyleria.coeus.damage.CombatEvent;
import com.hyleria.coeus.scoreboard.PlayerScoreboard;
import com.hyleria.common.reflect.ReflectionUtil;
import com.hyleria.common.time.Tick;
import com.hyleria.util.MessageUtil;
import com.hyleria.util.PlayerUtil;
import com.hyleria.util.Scheduler;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Optional;

import static com.hyleria.util.Colors.bold;
import static org.bukkit.ChatColor.GREEN;

/**
 * @author Ben (OutdatedVersion)
 * @since Mar/08/2017 (5:42 PM)
 */
@Singleton
public class UHC extends Game
{

    /** */
    @Inject private Hyleria plugin;

    /**  */
    private UHCConfig config;

    /**  */
    private World world;

    private boolean pvpEnabled = false;

    // handle stats engine side

    @Override
    public void init()
    {
        config = loadConfig("uhc", UHCConfig.class);
        ReflectionUtil.printOut(config);

        world = WorldCreator.name("uhc").createWorld();

        plugin.registerListeners(new Border().init(config.apothem).generatePhysicalBorder());
    }

    @Override
    public void begin()
    {
        MessageUtil.everyone(bold(GREEN) + "nifty starting message!!");

        // load scenarios
        config.enabledScenarios.stream()
                .map(name -> ReflectionUtil.classForName(getClass().getPackage().getName() + ".scenario." + name))
                .forEach(plugin::boundInjection);


        // reset player's health after the provided time
        Scheduler.delayed(() ->
        {
            PlayerUtil.everyone().forEach(PlayerUtil::fullHealth);
            MessageUtil.everyone(bold(ChatColor.GOLD) + "You've all been healed.");
        }, Tick.MINUTES.toTicks(config.healTime));

        // allow PvP
        Scheduler.delayed(() -> this.pvpEnabled = true, Tick.MINUTES.toTicks(config.pvpTime));
    }

    @Override
    public void end()
    {

    }

    @Override
    public void updateScoreboard(PlayerScoreboard scoreboard)
    {
        scoreboard.purge();
        scoreboard.space();
        scoreboard.writeHead("Today");
        scoreboard.write(ChatColor.RED + "Mar/15/17");
        scoreboard.space();
        scoreboard.writeHead("Players");
        scoreboard.write(ChatColor.RED + "1");
        scoreboard.writeURL();
        scoreboard.draw();
    }

    @EventHandler
    public void handlePvP(CombatEvent event)
    {
        if (!pvpEnabled)
            event.setCancelled(true);
    }

    @EventHandler
    public void disallowConsumeOfStrength(PlayerItemConsumeEvent event)
    {
        // probably a better way to do all of this?
        // may need to listen for the linger one too at some point

        if (event.getItem().getType() == Material.POTION)
        {
            final PotionMeta _meta = (PotionMeta) event.getItem().getItemMeta();
            final Optional<PotionEffect> _effect = _meta.getCustomEffects().stream().filter(effect -> effect.getType() == PotionEffectType.INCREASE_DAMAGE).findFirst();

            if (_effect.isPresent())
            {
                if (!config.allowStrengthOne && _effect.get().getDuration() == 1)
                {
                    event.setCancelled(true);
                    event.setItem(null);
                }

                if (!config.allowStrengthTwo && _effect.get().getDuration() == 2)
                {
                    event.setCancelled(true);
                    event.setItem(null);
                }
            }
        }
    }

}