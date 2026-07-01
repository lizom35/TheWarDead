package fr.thewardead.economy.gui;

import fr.thewardead.economy.EconomyPlugin;
import fr.thewardead.economy.model.AdminListing;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI de gestion du Marché Admin (ajout/suppression/prix).
 *
 * Layout 54 slots :
 *   Slots 0-44 : articles existants (5 rangées)
 *   Slots 45-53 : rangée admin
 *     Slot 45 : Ajouter (item en main)
 *     Slot 49 : infos
 *     Slot 53 : Retour au marché
 */
public class RomarketAdminGUI {

    public static final String TITLE = "§4§l⚙ Gestion Marché Admin";

    private final EconomyPlugin plugin;
    /** slot GUI → listingId */
    private final Map<UUID, Map<Integer, UUID>> slotMap = new HashMap<>();
    /** UUID joueur → mode "setprix" en attente */
    private final Map<UUID, UUID> awaitingPrice = new HashMap<>();

    public RomarketAdminGUI(EconomyPlugin plugin) { this.plugin = plugin; }

    // ── Ouverture ─────────────────────────────────────────────────────────────

    public void open(Player player) {
        List<AdminListing> all = plugin.getAdminShopManager().getAll();
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        Map<Integer, UUID> slots = new HashMap<>();
        for (int i = 0; i < all.size() && i < 45; i++) {
            inv.setItem(i, buildAdminItem(all.get(i)));
            slots.put(i, all.get(i).getId());
        }
        slotMap.put(player.getUniqueId(), slots);

        // Rangée admin
        ItemStack filler = make(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);

        inv.setItem(45, make(Material.LIME_STAINED_GLASS_PANE, "§a§l+ Ajouter un article",
            "§7Tenez l'item à vendre en main,",
            "§7puis cliquez ici.",
            "",
            "§7Le prix sera demandé dans le tchat."));

        inv.setItem(49, make(Material.EMERALD,
            "§e" + all.size() + " §7article(s) dans le marché",
            "§7Max affiché : 45 articles"));

        inv.setItem(53, make(Material.ARROW, "§e← Retour au Marché",
            "§7Retourner à la vue joueur."));

        player.openInventory(inv);
    }

    // ── Accesseurs ────────────────────────────────────────────────────────────

    public UUID getListingAt(UUID playerUUID, int slot) {
        Map<Integer, UUID> slots = slotMap.get(playerUUID);
        return slots != null ? slots.get(slot) : null;
    }

    public void clearTracking(UUID uuid) {
        slotMap.remove(uuid);
        awaitingPrice.remove(uuid);
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    private static ItemStack buildAdminItem(AdminListing listing) {
        ItemStack item = listing.build(1);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;

        List<String> lore = new ArrayList<>();
        lore.add("§7Prix : §6" + listing.getPricePerUnit() + "₩ §7/ unité");
        lore.add("§7Stock : §aIllimité");
        lore.add("§8ID : " + listing.getId().toString().substring(0, 8) + "...");
        lore.add("");
        lore.add("§cClic gauche §7→ §cSupprimer");
        lore.add("§eClic droit §7→ §eChanger le prix (tchat)");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack make(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
