package fr.thewardead.economy;

import fr.thewardead.economy.command.*;
import fr.thewardead.economy.gui.*;
import fr.thewardead.economy.listener.*;
import fr.thewardead.economy.manager.*;
import fr.thewardead.kingdoms.KingdomPlugin;
import fr.thewardead.kingdoms.manager.KingdomManager;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyPlugin extends JavaPlugin {

    private KingdomPlugin      kingdomPlugin;
    private KingdomManager     kingdomManager;
    private EconomyManager     economyManager;
    private ShopManager        shopManager;
    private AdminShopManager   adminShopManager;
    private TradeManager       tradeManager;

    private ShopBrowserGUI     shopBrowserGUI;
    private ShopOwnerGUI       shopOwnerGUI;
    private ShopSellGUI        shopSellGUI;
    private KingdomShopGUI     kingdomShopGUI;
    private RomarketGUI        romarketGUI;
    private RomarketAdminGUI   romarketAdminGUI;
    private TradeGUI           tradeGUI;

    private ShopCommand        shopCommand;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("romarket-seed.yml", false); // extrait le catalogue Ballistix si absent

        // Récupère KingdomManager depuis TWD-Kingdoms
        KingdomPlugin kingdoms = (KingdomPlugin) getServer().getPluginManager().getPlugin("TWD-Kingdoms");
        if (kingdoms == null) {
            getLogger().severe("TWD-Kingdoms introuvable ! TWD-Economy désactivé.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.kingdomPlugin  = kingdoms;
        this.kingdomManager = kingdoms.getKingdomManager();

        // Managers
        this.economyManager = new EconomyManager(this, kingdomManager);
        this.shopManager    = new ShopManager(this);
        this.shopManager.load();
        this.adminShopManager = new AdminShopManager(this);
        this.adminShopManager.load();
        // Auto-peuple le marché Ballistix au premier démarrage
        if (adminShopManager.getAll().isEmpty()) {
            getLogger().info("Marché Admin vide — import automatique du catalogue Ballistix...");
            int n = adminShopManager.importSeed(false);
            getLogger().info("Marché Admin : " + n + " article(s) importé(s).");
        }
        this.tradeManager   = new TradeManager(this);

        // GUIs
        this.shopBrowserGUI = new ShopBrowserGUI(this);
        this.shopOwnerGUI   = new ShopOwnerGUI(this);
        this.shopSellGUI    = new ShopSellGUI(this);
        this.kingdomShopGUI  = new KingdomShopGUI(this);
        this.romarketGUI     = new RomarketGUI(this);
        this.romarketAdminGUI = new RomarketAdminGUI(this);
        this.tradeGUI        = new TradeGUI(this);

        // Commands
        shopCommand = new ShopCommand(this);
        BalanceCommand balCmd = new BalanceCommand(this);

        registerCmd("balance",  balCmd);
        registerCmd("pay",      balCmd);
        registerCmd("baltop",   balCmd);
        registerCmd("shop",     shopCommand);
        registerCmd("roshop",   new RoshopCommand(this));
        registerCmd("romarket", new RomarketCommand(this));
        registerCmd("trade",    new TradeCommand(this));

        // Listeners
        getServer().getPluginManager().registerEvents(new ShopListener(this),    this);
        getServer().getPluginManager().registerEvents(new TradeListener(this),   this);
        getServer().getPluginManager().registerEvents(new RomarketListener(this), this);

        getLogger().info("TWD-Economy v" + getDescription().getVersion() + " activé.");
    }

    @Override
    public void onDisable() {
        if (shopManager != null)      shopManager.save();
        if (adminShopManager != null) adminShopManager.save();
        getLogger().info("TWD-Economy désactivé.");
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void registerCmd(String name, org.bukkit.command.CommandExecutor exec) {
        var cmd = getCommand(name);
        if (cmd != null) cmd.setExecutor(exec);
        else getLogger().warning("Commande /" + name + " absente du plugin.yml !");
    }

    // ── Getters ────────────────────────────────────────────────────────────

    public KingdomPlugin   getKingdomPlugin()   { return kingdomPlugin;   }
    public KingdomManager  getKingdomManager()  { return kingdomManager;  }
    public EconomyManager  getEconomyManager()  { return economyManager;  }
    public ShopManager     getShopManager()     { return shopManager;     }
    public TradeManager    getTradeManager()    { return tradeManager;    }
    public ShopBrowserGUI  getShopBrowserGUI()  { return shopBrowserGUI;  }
    public ShopOwnerGUI    getShopOwnerGUI()    { return shopOwnerGUI;    }
    public ShopSellGUI     getShopSellGUI()     { return shopSellGUI;     }
    public AdminShopManager  getAdminShopManager()  { return adminShopManager;  }
    public KingdomShopGUI    getKingdomShopGUI()    { return kingdomShopGUI;    }
    public RomarketGUI       getRomarketGUI()       { return romarketGUI;       }
    public RomarketAdminGUI  getRomarketAdminGUI()  { return romarketAdminGUI;  }
    public TradeGUI          getTradeGUI()          { return tradeGUI;          }
    public ShopCommand     getShopCommand()     { return shopCommand;     }
}
