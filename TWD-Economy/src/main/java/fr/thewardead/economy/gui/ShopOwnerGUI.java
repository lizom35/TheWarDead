package fr.thewardead.economy.gui;

import fr.thewardead.economy.EconomyPlugin;
import fr.thewardead.economy.model.ShopListing;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ShopOwnerGUI {

    public static final String TITLE = "§6§lMon Shop";

    private final EconomyPlugin plugin;
    // Slot → listingId pour le joueur actif
    private final Map<UUID, Map<Integer, UUID>> slotMap = new HashMap<>();

    public ShopOwnerGUI(EconomyPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        List<ShopListing> mine = plugin.getShopManager().getActiveBySeller(player.getUniqueId());
        int maxListings = plugin.getConfig().getInt("shop.max-listings-per-player", 10);

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        Map<Integer, UUID> slots = new HashMap<>();
        for (int i = 0; i < mine.size() && i < 45; i++) {
            ShopListing listing = mine.get(i);
            inv.setItem(i, buildOwnerItem(listing));
            slots.put(i, listing.getId());
        }
        slotMap.put(player.getUniqueId(), slots);

        // Slot 45 : Nouvelle annonce (si quota ok)
        boolean canAdd = mine.size() < maxListings;
        inv.setItem(45, make(
            canAdd ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
            canAdd ? "§a§l+ Nouvelle annonce" : "§c§lQuota atteint (" + maxListings + "/" + maxListings + ")",
            canAdd
                ? List.of("§7Déposez un item dans votre", "§7main principale et exécutez", "§7/shop sell <prix>")
                : List.of("§7Retirez une annonce existante", "§7pour en créer une nouvelle.")
        ));

        // Déco
        ItemStack filler = make(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 46; i < 54; i++) inv.setItem(i, filler);

        // Slot 53 : Retour marché
        inv.setItem(53, make(Material.ARROW, "§e← Retour au Marché", List.of("§7Parcourir toutes les annonces")));

        player.openInventory(inv);
    }

    public UUID getListingAt(UUID playerUUID, int slot) {
        Map<Integer, UUID> slots = slotMap.get(playerUUID);
        return slots != null ? slots.get(slot) : null;
    }

    public void clearTracking(UUID uuid) {
        slotMap.remove(uuid);
    }

    private static ItemStack buildOwnerItem(ShopListing listing) {
        ItemStack base = listing.getTemplate().clone();
        base.setAmount(Math.min(listing.getStock(), 64));
        ItemMeta meta = base.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add("§7Prix : §6" + listing.getPricePerUnit() + "₩ §7/ unité");
            lore.add("§7Stock : §e" + listing.getStock() + " §7unités");
            lore.add("§7Type : §b" + listing.getType().name());
            lore.add("");
            lore.add("§cClic gauche §7→ Retirer l'annonce");
            lore.add("§cShift+Clic §7→ Retirer & récupérer stock");
            meta.setLore(lore);
            base.setItemMeta(meta);
        }
        return base;
    }

    private static ItemStack make(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); meta.setLore(lore); item.setItemMeta(meta); }
        return item;
    }
}
