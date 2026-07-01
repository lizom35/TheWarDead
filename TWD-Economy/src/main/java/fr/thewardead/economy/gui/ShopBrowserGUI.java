package fr.thewardead.economy.gui;

import fr.thewardead.economy.EconomyPlugin;
import fr.thewardead.economy.model.ShopListing;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShopBrowserGUI {

    public static final String TITLE_PERSONAL = "§6§lMarché — Toutes les annonces";
    public static final String TITLE_KINGDOM  = "§6§lShop du Royaume";
    public static final String TITLE_MARKET   = "§6§lMarché des Armes & Mods";

    private static final int ROWS = 4;              // 4 rangées d'annonces
    private static final int SLOTS_PER_PAGE = 36;   // 4×9

    private final EconomyPlugin plugin;

    public ShopBrowserGUI(EconomyPlugin plugin) {
        this.plugin = plugin;
    }

    public void openPersonal(Player player, int page) {
        open(player, plugin.getShopManager().getActive(), TITLE_PERSONAL, page, null);
    }

    public void openKingdom(Player player, UUID kingdomId, int page) {
        open(player, plugin.getShopManager().getActiveByKingdom(kingdomId), TITLE_KINGDOM, page, kingdomId);
    }

    public void openMarket(Player player, int page) {
        open(player, plugin.getShopManager().getActiveModItems(), TITLE_MARKET, page, null);
    }

    private void open(Player player, List<ShopListing> allListings, String title, int page, UUID kingdomId) {
        int maxPage = Math.max(0, (allListings.size() - 1) / SLOTS_PER_PAGE);
        page = Math.min(Math.max(page, 0), maxPage);

        Inventory inv = Bukkit.createInventory(null, 45, title);

        // ── Annonces (slots 0-35) ─────────────────────────────────────────
        int start = page * SLOTS_PER_PAGE;
        for (int i = 0; i < SLOTS_PER_PAGE && (start + i) < allListings.size(); i++) {
            ShopListing listing = allListings.get(start + i);
            inv.setItem(i, buildListingItem(listing));
        }

        // ── Rangée navigation (slots 36-44) ──────────────────────────────
        // Slot 36 : page précédente
        if (page > 0) {
            inv.setItem(36, make(Material.ARROW, "§e← Page précédente",
                List.of("§7Page §e" + page + "§7/§e" + (maxPage + 1))));
        }

        // Slot 38 : infos page
        inv.setItem(38, make(Material.BOOK,
            "§7Page §e" + (page + 1) + "§7/§e" + (maxPage + 1),
            List.of("§7Total annonces : §e" + allListings.size())));

        // Slot 40 : Mon shop (si browsing PERSONAL)
        if (title.equals(TITLE_PERSONAL)) {
            inv.setItem(40, make(Material.CHEST, "§a§lMon Shop",
                List.of("§7Gérer vos propres annonces.", "", "§aCliquez pour ouvrir")));
        }

        // Slot 44 : page suivante
        if (page < maxPage) {
            inv.setItem(44, make(Material.ARROW, "§ePage suivante →",
                List.of("§7Page §e" + (page + 2) + "§7/§e" + (maxPage + 1))));
        }

        // Décoration
        ItemStack filler = make(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 36; i < 45; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }

        // Stocker page + kingdomId dans le titre (workaround: on encode la page dans le titre)
        // → On utilise un Map dans ShopBrowserGUI pour tracker les pages par joueur
        pageMap.put(player.getUniqueId(), page);
        titleMap.put(player.getUniqueId(), title);
        if (kingdomId != null) kingdomMap.put(player.getUniqueId(), kingdomId);

        player.openInventory(inv);
    }

    // ── Page tracking ──────────────────────────────────────────────────────
    private final java.util.Map<UUID, Integer> pageMap    = new java.util.HashMap<>();
    private final java.util.Map<UUID, String>  titleMap   = new java.util.HashMap<>();
    private final java.util.Map<UUID, UUID>    kingdomMap = new java.util.HashMap<>();

    public int getPage(UUID uuid) {
        return pageMap.getOrDefault(uuid, 0);
    }

    public void clearTracking(UUID uuid) {
        pageMap.remove(uuid); titleMap.remove(uuid); kingdomMap.remove(uuid);
    }

    public UUID getKingdomId(UUID uuid) { return kingdomMap.get(uuid); }

    /** Recharge la même page avec la même catégorie. */
    public void refresh(Player player) {
        String title = titleMap.getOrDefault(player.getUniqueId(), TITLE_PERSONAL);
        int page     = pageMap.getOrDefault(player.getUniqueId(), 0);
        if (title.equals(TITLE_KINGDOM)) {
            openKingdom(player, kingdomMap.get(player.getUniqueId()), page);
        } else if (title.equals(TITLE_MARKET)) {
            openMarket(player, page);
        } else {
            openPersonal(player, page);
        }
    }

    // ── Item builder ───────────────────────────────────────────────────────

    public static ItemStack buildListingItem(ShopListing listing) {
        ItemStack display = listing.getTemplate();
        ItemMeta meta = display.getItemMeta();

        String itemName = meta != null && meta.hasDisplayName()
            ? meta.getDisplayName()
            : display.getType().name().replace('_', ' ').toLowerCase();

        List<String> lore = new ArrayList<>();
        lore.add("§7Vendeur : §e" + listing.getSellerName());
        lore.add("§7Prix : §6" + listing.getPricePerUnit() + "₩ §7/ unité");
        lore.add("§7Stock : §e" + listing.getStock() + " §7unités");
        if (listing.getType() == ShopListing.ListingType.KINGDOM) {
            lore.add("§7Type : §bShop Royaume");
        }
        lore.add("");
        lore.add("§aClic gauche §7→ Acheter §e1");
        lore.add("§aShift+Clic §7→ Acheter §emax");

        ItemStack result = listing.getTemplate().clone();
        result.setAmount(Math.min(listing.getStock(), 64));
        ItemMeta rm = result.getItemMeta();
        if (rm != null) {
            if (!rm.hasDisplayName()) rm.setDisplayName("§f" + itemName);
            rm.setLore(lore);
            result.setItemMeta(rm);
        }
        return result;
    }

    private static ItemStack make(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); meta.setLore(lore); item.setItemMeta(meta); }
        return item;
    }
}
