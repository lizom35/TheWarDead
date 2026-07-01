package fr.thewardead.kingdoms.gui;

import fr.thewardead.kingdoms.KingdomPlugin;
import fr.thewardead.kingdoms.model.Kingdom;
import fr.thewardead.kingdoms.model.KingdomPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class PlayerMenuGUI {

    public static final String TITLE = "§c§lMenu Joueur";

    private final KingdomPlugin plugin;

    public PlayerMenuGUI(KingdomPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        KingdomPlayer kp = plugin.getKingdomManager().getOrCreatePlayer(player);
        Kingdom k = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());

        // Slot 4 : tête du joueur avec ses infos
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.setOwningPlayer(player);
        skullMeta.setDisplayName("§e§l" + player.getName());
        skullMeta.setLore(List.of(
            "§7Solde : §e" + kp.getBalance() + " ₩",
            "§7Royaume : §e" + (k != null
                ? k.getName() + " §7(§e" + k.getLevel().getDisplayName() + "§7)"
                : "Aucun"),
            "§7Rôle : §e" + (k != null && k.getRole(player.getUniqueId()) != null
                ? k.getRole(player.getUniqueId()).getDisplayName()
                : "—")
        ));
        head.setItemMeta(skullMeta);
        inv.setItem(4, head);

        if (k == null) {
            // Slot 11 : Créer un royaume
            inv.setItem(11, FirstConnectGUI.makeItem(Material.NETHER_STAR,
                "§e§lCréer un Royaume",
                List.of("§7Fondez votre propre territoire.", "§72 joueurs minimum.", "", "§aCliquez pour créer")));

            // Slot 15 : Rejoindre un royaume
            inv.setItem(15, FirstConnectGUI.makeItem(Material.MAP,
                "§b§lRejoindre un Royaume",
                List.of("§7Rejoignez un royaume existant.", "§7(Invitation requise)", "", "§aCliquez pour voir la liste")));
        } else {
            // Slot 11 : Gérer mon royaume
            inv.setItem(11, FirstConnectGUI.makeItem(Material.NETHER_STAR,
                "§e§lMon Royaume : §c" + k.getName(),
                List.of(
                    "§7Niveau : §e" + k.getLevel().getDisplayName(),
                    "§7Membres : §e" + k.getMemberCount(),
                    "§7Trésor : §e" + k.getBank() + " ₩",
                    "§7Territoire : §e" + k.getClaimedChunks() + "§7/§e" + k.getMaxChunks() + " §7chunks",
                    "",
                    "§aCliquez pour gérer le royaume"
                )));

            // Slot 15 : Aller au spawn
            inv.setItem(15, FirstConnectGUI.makeItem(Material.COMPASS,
                "§f§lAller au Spawn",
                List.of("§7Vous téléporte au point de départ.", "", "§aCliquez pour vous téléporter")));
        }

        // Décoration
        ItemStack filler = FirstConnectGUI.makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }

        player.openInventory(inv);
    }
}
