package fr.thewardead.kingdoms;

import fr.thewardead.kingdoms.command.KingdomCommand;
import fr.thewardead.kingdoms.command.MenuCommand;
import fr.thewardead.kingdoms.gui.ChatInputManager;
import fr.thewardead.kingdoms.gui.KingdomMenuGUI;
import fr.thewardead.kingdoms.gui.PlayerMenuGUI;
import fr.thewardead.kingdoms.listener.ChatListener;
import fr.thewardead.kingdoms.listener.GUIListener;
import fr.thewardead.kingdoms.listener.PlayerListener;
import fr.thewardead.kingdoms.listener.TerritoryMoveListener;
import fr.thewardead.kingdoms.manager.KingdomManager;
import fr.thewardead.kingdoms.task.HudTask;
import fr.thewardead.kingdoms.task.IncomeTask;
import org.bukkit.plugin.java.JavaPlugin;

public class KingdomPlugin extends JavaPlugin {

    private KingdomManager    kingdomManager;
    private ChatInputManager  chatInputManager;
    private PlayerMenuGUI     playerMenuGUI;
    private KingdomMenuGUI    kingdomMenuGUI;
    private HudTask           hudTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        kingdomManager   = new KingdomManager(this);
        kingdomManager.load();

        chatInputManager = new ChatInputManager(this);
        playerMenuGUI    = new PlayerMenuGUI(this);
        kingdomMenuGUI   = new KingdomMenuGUI(this);

        // Commandes
        KingdomCommand kingdomCommand = new KingdomCommand(this);
        getCommand("kingdom").setExecutor(kingdomCommand);
        getCommand("kingdom").setTabCompleter(kingdomCommand);
        getCommand("menu").setExecutor(new MenuCommand(this));

        // Listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(chatInputManager, this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new TerritoryMoveListener(this), this);

        // Tâches
        IncomeTask.start(this);
        hudTask = new HudTask(this);
        hudTask.runTaskTimer(this, 20L, 20L);

        getLogger().info("TWD-Kingdoms activé !");
    }

    @Override
    public void onDisable() {
        if (kingdomManager != null) kingdomManager.save();
        getLogger().info("TWD-Kingdoms désactivé — données sauvegardées.");
    }

    public KingdomManager   getKingdomManager()  { return kingdomManager; }
    public ChatInputManager getChatInputManager() { return chatInputManager; }
    public PlayerMenuGUI    getPlayerMenuGUI()    { return playerMenuGUI; }
    public KingdomMenuGUI   getKingdomMenuGUI()   { return kingdomMenuGUI; }
    public HudTask          getHudTask()          { return hudTask; }
}
