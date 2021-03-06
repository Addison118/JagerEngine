package me.exerosis.jager.engine.implementation.components.worlds;

import me.exerosis.jager.engine.core.utilites.FileUtilities;
import me.exerosis.jager.engine.implementation.components.printer.printers.ConsolePrinter;
import me.exerosis.jager.engine.implementation.components.printer.PrintLevel;
import me.exerosis.jager.engine.implementation.components.scheduler.SchedulerComponent;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.world.WorldLoadEvent;

import java.io.*;

/**
 * Created by Exerosis.
 */
public class WorldComponent extends EventComponent {
    private final ConsolePrinter consolePrinter;
    private final SchedulerComponent schedulerComponent;
    private final File worldFile;
    private String kickMessage;
    private File backupFile;

    public WorldComponent(ConsolePrinter consolePrinter, SchedulerComponent schedulerComponent, File worldFile, String kickMessage) {
        this.consolePrinter = consolePrinter;
        this.schedulerComponent = schedulerComponent;
        this.worldFile = worldFile;
        this.kickMessage = kickMessage;
        backupFile = new File(worldFile.getPath() + "/backup.tmp");
    }

    public void load() {
        print("Trying to load world.");

        if (!worldFile.getPath().startsWith(Bukkit.getWorldContainer().getParent()))
            print("Failed to load world! '" + worldFile.getPath() + "' is not in the server folder.");
        if (!worldFile.exists())
            print("Failed to load world! No such file or directory: '" + worldFile.getPath() + "'.");
        if (!worldFile.isDirectory())
            print("Failed to load world! Could not find world at '" + worldFile.getPath() + "'.");

        File worldData = new File(worldFile.getPath() + "/level.dat");

        if (!worldData.exists())
            print("Failed to load world! Could not find expected 'level.dat' in directory '" + worldFile.getPath() + "'.");

        WorldCreator worldCreator = new WorldCreator("");
        worldCreator.type(WorldType.FLAT);
        Bukkit.createWorld(worldCreator);

        print("World can be loaded. Loading world.");
    }

    @EventHandler
    public void onWorldLoadEvent(WorldLoadEvent event){
        if(event.getWorld().getWorldFolder().equals(worldFile))
            onLoad();
    }

    public void backup() {
        try {
            backupFile = File.createTempFile("backup", ".tmp", worldFile);
            FileUtilities.createZip(worldFile, backupFile);
            onBackup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void revert() {
        if (!backupFile.isFile())
            return;
        try {
            FileUtils.deleteDirectory(worldFile);
            FileUtilities.unzip(backupFile, worldFile);
            onRevert();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void unload() {
        print("Trying to unload world!");
        World world = Bukkit.getWorld(worldFile.getName());

        if (world == null)
            return;

        //Remove players
        for (Player player : world.getPlayers())
            player.kickPlayer(kickMessage);

        //Wait 1 second before unloading the world
        schedulerComponent.registerTask(() -> {
            if (!Bukkit.unloadWorld(world, false))
                throw new RuntimeException("[WorldComponent] Unable to unload world, please fix the problem!");
            onUnload();
        }, 20D);
    }

    private void print(Object message){
        consolePrinter.print("[WorldComponent] " + message, PrintLevel.HIGH);
    }

    protected void onUnload(){

    }

    protected void onLoad(){

    }

    protected void onRevert(){

    }

    protected void onBackup(){

    }
}
