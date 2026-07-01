package fr.thewardead.kingdoms.gui;

import fr.thewardead.kingdoms.KingdomPlugin;
import fr.thewardead.kingdoms.model.Kingdom;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ReconnectGUI {

    public static final String TITLE = "§c§lOù voulez-vous aller ?";

    private final KingdomPlugin plugin;

    public ReconnectGUI(KingdomPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Kingdom k = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // Slot 11 : TP au royaume (seulement si membre d'un royaume avec home défini)
        if (k != null && k.getHome() != null) {
            inv.setItem(11, FirstConnectGUI.makeItem(Material.NETHER_STAR,
                "§e§lRoyaume : " + k.getName(),
                List.of("§7Niveau : §a" + k.getLevel().getDisplayName(),
                    "§7Membres : §e" + k.getMemberCount(),
                    "§7Trésor : §e" + k.getBank() + "₩",
                    "", "§aCliquez pour vous téléporter")));
        } else if (k != null) {
            inv.setItem(11, FirstConnectGUI.makeItem(Material.NETHER_STAR,
                "§e§lRoyaume : " + k.getName(),
                List.of("§cAucun point de retour défini.", "§7Demandez au chef de faire /kingdom sethome.")));
        }

        // Slot 15 : Rejoindre la map
        inv.setItem(15, FirstConnectGUI.makeItem(Material.COMPASS,
            "§f§lRejoindre la Map",
            List.of("§7Apparaître au Spawn.", "", "§aCliquez pour continuer")));

        // Remplissage décoratif
        ItemStack filler = FirstConnectGUI.makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }

        player.openInventory(inv);
    }
}
