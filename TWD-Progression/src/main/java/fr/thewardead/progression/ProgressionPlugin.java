package fr.thewardead.progression;

import fr.thewardead.progression.command.ClassCommand;
import fr.thewardead.progression.listener.FarmerListener;
import fr.thewardead.progression.listener.JoinListener;
import fr.thewardead.progression.listener.MilitaireListener;
import fr.thewardead.progression.manager.ProgressionManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ProgressionPlugin extends JavaPlugin {

    private ProgressionManager progressionManager;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();

        progressionManager = new ProgressionManager(this);
        progressionManager.load();

        var pm = getServer().getPluginManager();
        pm.registerEvents(new JoinListener(this),      this);
        pm.registerEvents(new FarmerListener(this),    this);
        pm.registerEvents(new MilitaireListener(this), this);

        ClassCommand classCmd = new ClassCommand(this);
        getCommand("class").setExecutor(classCmd);
        getCommand("class").setTabCompleter(classCmd);

        getLogger().info("TWD-Progression activé.");
    }

    @Override
    public void onDisable() {
        if (progressionManager != null) progressionManager.save();
        getLogger().info("TWD-Progression désactivé.");
    }

    public ProgressionManager getProgressionManager() {
        return progressionManager;
    }
}
