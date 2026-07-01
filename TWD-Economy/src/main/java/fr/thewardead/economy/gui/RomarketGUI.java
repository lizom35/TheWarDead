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
 * /romarket — Admin-shop : items de mods à prix fixe, stock infini.
 *
 * Layout 45 slots :
 *   Slots 0-35  : annonces (4 rangées)
 *   Slots 36-44 : navigation
 */
public class RomarketGUI {

    public static final String TITLE = "§4§lMarché des Armes";

    private static final int SLOTS_PER_PAGE = 36;

    private final EconomyPlugin plugin;
    private final Map<UUID, Integer> pageMap = new HashMap<>();

    public RomarketGUI(EconomyPlugin plugin) { this.plugin = plugin; }

    // ── Ouverture ─────────────────────────────────────────────────────────────

    public void open(Player player, int page) {
        List<AdminListing> all = plugin.getAdminShopManager().getAll();
        int maxPage = Math.max(0, (all.size() - 1) / SLOTS_PER_PAGE);
        page = Math.min(Math.max(page, 0), maxPage);

        Inventory inv = Bukkit.createInventory(null, 45, TITLE);

        // Annonces
        int start = page * SLOTS_PER_PAGE;
        for (int i = 0; i < SLOTS_PER_PAGE && (start + i) < all.size(); i++) {
            inv.setItem(i, buildItem(all.get(start + i)));
        }

        // Rangée navigation
        ItemStack filler = make(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 36; i < 45; i++) inv.setItem(i, filler);

        if (page > 0)
            inv.setItem(36, make(Material.ARROW, "§e← Page précédente",
                "§7Page " + page + "/" + (maxPage + 1)));

        inv.setItem(38, make(Material.BOOK,
            "§7Page §e" + (page + 1) + "§7/§e" + (maxPage + 1),
            "§7Total articles : §e" + all.size()));

        if (player.hasPermission("twd.economy.admin") || player.isOp())
            inv.setItem(40, make(Material.COMMAND_BLOCK, "§c§l⚙ Gérer le Marché",
                "§7Ajouter / retirer des articles.", "§8(Admins uniquement)"));

        if (page < maxPage)
            inv.setItem(44, make(Material.ARROW, "§ePage suivante →",
                "§7Page " + (page + 2) + "/" + (maxPage + 1)));

        pageMap.put(player.getUniqueId(), page);
        player.openInventory(inv);
    }

    public int  getPage(UUID uuid) { return pageMap.getOrDefault(uuid, 0); }
    public void clearPage(UUID uuid) { pageMap.remove(uuid); }

    // ── Item builder ──────────────────────────────────────────────────────────

    private static ItemStack buildItem(AdminListing listing) {
        ItemStack item = listing.build(1);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;

        List<String> lore = new ArrayList<>();
        if (meta.hasLore()) lore.addAll(meta.getLore());
        lore.add("");
        lore.add("§7Prix : §6" + listing.getPricePerUnit() + "₩ §7/ unité");
        lore.add("§7Stock : §aIllimité");
        lore.add("");
        lore.add("§aClic gauche §7→ Acheter §e1");
        lore.add("§aShift+Clic §7→ Acheter §e64");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
