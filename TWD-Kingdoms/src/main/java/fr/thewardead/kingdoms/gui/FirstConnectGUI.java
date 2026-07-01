package fr.thewardead.kingdoms.gui;

import fr.thewardead.kingdoms.KingdomPlugin;
import fr.thewardead.kingdoms.manager.KingdomManager;
import fr.thewardead.kingdoms.model.Kingdom;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class FirstConnectGUI {

    public static final String TITLE = "§c§lBienvenue — Que voulez-vous faire ?";

    private final KingdomPlugin plugin;

    public FirstConnectGUI(KingdomPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // Slot 11 : Créer un royaume
        inv.setItem(11, makeItem(Material.NETHER_STAR,
            "§e§lCréer un Royaume",
            List.of("§7Fondez votre propre royaume.", "§7(2 joueurs minimum)", "", "§aCliquez pour créer")));

        // Slot 13 : Rejoindre un royaume
        KingdomManager km = plugin.getKingdomManager();
        ItemStack joinItem = makeItem(Material.MAP,
            "§b§lRejoindre un Royaume",
            List.of("§7Rejoignez un royaume existant.", "§7(Invitation requise)", "", "§aCliquez pour voir la liste"));
        inv.setItem(13, joinItem);

        // Slot 15 : Aller au Spawn
        inv.setItem(15, makeItem(Material.COMPASS,
            "§f§lRejoindre la Map",
            List.of("§7Explorez le monde librement.", "§7Vous pouvez rejoindre un royaume", "§7plus tard via /kingdom.", "", "§aCliquez pour aller au Spawn")));

        // Remplissage décoratif
        ItemStack filler = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }

        player.openInventory(inv);
    }

    // ---- Utilitaire ----

    public static ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
