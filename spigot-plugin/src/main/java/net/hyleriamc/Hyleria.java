package net.hyleriamc;

import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import net.hyleriamc.commons.util.StartParallel;
import net.hyleriamc.commons.inject.ConfigurationProvider;
import net.hyleriamc.util.ShutdownHook;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;


/**
 * Handles the setup & initialization
 * of this plugin
 *
 * @author Ben (OutdatedVersion)
 * @since Dec/11/2016 (5:48 PM)
 */
public class Hyleria extends JavaPlugin
{

    private Injector injector;
    private List<Method> shutdownHooks;

    @Override
    public void onEnable()
    {
        shutdownHooks = Lists.newArrayList();
        // TODO(Ben): handle shutdown hooks

        // our primary injector - what we base a whole
        // lot of this plugin around
        injector = Guice.createInjector(Stage.DEVELOPMENT, binder ->
        {
            // this plugin
            binder.bind(Hyleria.class).toInstance(this);
            binder.bind(Server.class).toInstance(Bukkit.getServer());
            binder.bind(BukkitScheduler.class).toInstance(Bukkit.getServer().getScheduler());

            binder.install(new ConfigurationProvider.ConfigurationModule());
        });


        System.out.println("Scanning class path for auto-start modules; Filter match: " + getClass().getPackage().getName());

        new FastClasspathScanner(getClass().getPackage().getName())
                .matchClassesWithAnnotation(StartParallel.class, clazz -> injector.getInstance(clazz)).scan();
    }

    @Override
    public void onDisable()
    {
        System.out.println("Executing shutdown hooks..");

        shutdownHooks.forEach(method ->
        {
            try
            {
                method.invoke(this);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                System.err.println("\nIssue handling the executing of a shutdown hook");
                System.err.println("Origin: [" + method.getDeclaringClass().getName() + "]");
            }
        });
    }

    /**
     * @return grabs the one & only instance
     *         of our plugin for this server
     */
    public static Hyleria get()
    {
        return JavaPlugin.getPlugin(Hyleria.class);
    }

    /**
     * @return the injector bound to this plugin
     */
    public Injector injector()
    {
        return injector;
    }

    /**
     * Scans through the provided {@link Class}
     * and collects all of the methods annotated
     * with {@link ShutdownHook}. These methods
     * are to be executed when the plugin (in
     * most cases the server will be turning
     * off when this occurs) disables. Invalid
     * shutdown hooks, i.e. ones with any
     * non-zero number of parameters
     * will silently fail.
     *
     * @param clazz the class to look through
     * @return this plugin
     */
    public Hyleria registerHook(Class<?> clazz)
    {
        // consider satisfying parameters with our injector?

        Stream.of(clazz.getMethods())
              .filter(method -> method.isAnnotationPresent(ShutdownHook.class))
              .filter(method -> method.getParameterCount() == 0)
              .forEach(shutdownHooks::add);

        return this;
    }

    /**
     * Lets Bukkit's event system know about
     * a listener bound to a certain class.
     *
     * <br>
     * We do verify that the required {@link EventHandler}
     * annotation is included in that class somewhere.
     *
     * <br>
     * In the case that it is we'll hit up our injector
     * to create a new instance of that item.
     *
     * @param classSet the things we'd like to register
     * @return our plugin instance
     */
    public Hyleria registerListeners(Class<? extends Listener>... classSet)
    {
        Stream.of(classSet)
                .filter(clazz -> Stream.of(clazz.getMethods()).anyMatch(method -> method.isAnnotationPresent(EventHandler.class)))
                .forEach(clazz -> Bukkit.getServer().getPluginManager().registerEvents(injector.getInstance(clazz), this));

        return this;
    }

}