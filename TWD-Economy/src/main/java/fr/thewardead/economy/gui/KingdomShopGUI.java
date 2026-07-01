package fr.thewardead.economy.gui;

import fr.thewardead.economy.EconomyPlugin;
import fr.thewardead.economy.model.ShopListing;
import fr.thewardead.kingdoms.model.Kingdom;
import fr.thewardead.kingdoms.model.KingdomPlayer;
import fr.thewardead.kingdoms.model.KingdomRole;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * /roshop — shop officiel du royaume.
 * Les membres du royaume voient et achètent ; les officiers/chef peuvent aussi vendre.
 */
public class KingdomShopGUI {

    public static final String TITLE = "§5§lShop Royaume";

    private final EconomyPlugin plugin;
    private final Map<UUID, Map<Integer, UUID>> slotMap = new HashMap<>();

    public KingdomShopGUI(EconomyPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Kingdom k = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
        if (k == null) {
            player.sendMessage(c(pre() + "&cVous n'appartenez à aucun royaume."));
            return;
        }

        List<ShopListing> listings = plugin.getShopManager().getActiveByKingdom(k.getId());
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        Map<Integer, UUID> slots = new HashMap<>();
        for (int i = 0; i < listings.size() && i < 45; i++) {
            inv.setItem(i, ShopBrowserGUI.buildListingItem(listings.get(i)));
            slots.put(i, listings.get(i).getId());
        }
        slotMap.put(player.getUniqueId(), slots);

        // Rangée bas
        ItemStack filler = make(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);

        // Officiers/chef : bouton pour ajouter une annonce royaume
        KingdomPlayer kp = plugin.getKingdomManager().getPlayer(player.getUniqueId());
        KingdomRole role = (kp != null) ? k.getRole(player.getUniqueId()) : KingdomRole.MEMBRE;
        boolean canManage = role == KingdomRole.CHEF || role == KingdomRole.OFFICIER;

        if (canManage) {
            inv.setItem(49, make(Material.CHEST, "§b§l+ Ajouter annonce Royaume",
                List.of("§7Cliquez pour ouvrir le menu de vente.",
                        "§7Réservé aux Officiers et au Chef.")));
        }

        inv.setItem(53, make(Material.ARROW, "§e← Retour", List.of()));
        player.openInventory(inv);
    }

    public UUID getListingAt(UUID playerUUID, int slot) {
        Map<Integer, UUID> slots = slotMap.get(playerUUID);
        return slots != null ? slots.get(slot) : null;
    }

    public void clearTracking(UUID uuid) {
        slotMap.remove(uuid);
    }

    private String pre() {
        return plugin.getConfig().getString("messages.prefix", "&8[&6₩&8] &r");
    }

    private String c(String s) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }

    private static ItemStack make(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); meta.setLore(lore); item.setItemMeta(meta); }
        return item;
    }
}
