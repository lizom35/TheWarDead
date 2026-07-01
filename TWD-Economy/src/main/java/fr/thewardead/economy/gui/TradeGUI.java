package fr.thewardead.economy.gui;

import fr.thewardead.economy.EconomyPlugin;
import fr.thewardead.economy.model.TradeSession;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class TradeGUI {

    public static final String TITLE = "§d§lTroc";

    private final EconomyPlugin plugin;

    public TradeGUI(EconomyPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Construit et ouvre l'inventaire de troc pour les deux joueurs.
     * À appeler depuis le thread principal.
     */
    public void open(TradeSession session) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE);
        session.setInventory(inv);

        refresh(session);

        Player pA = Bukkit.getPlayer(session.getPlayerA());
        Player pB = Bukkit.getPlayer(session.getPlayerB());
        if (pA != null) pA.openInventory(inv);
        if (pB != null) pB.openInventory(inv);
    }

    /**
     * Rafraîchit les slots décoratifs/statut sans toucher aux slots items.
     * Appeler après tout changement d'état.
     */
    public void refresh(TradeSession session) {
        Inventory inv = session.getInventory();
        if (inv == null) return;

        // ── Colonne séparatrice (colonnes 3, slots 3,12,21,30,39) ──────────
        ItemStack sep = make(Material.WHITE_STAINED_GLASS_PANE, "§7│", List.of());
        for (int row = 0; row < 5; row++) inv.setItem(row * 9 + 4, sep);

        // ── Or / mise (slots 27 A / 35 B) ────────────────────────────────
        // Ces slots sont gérés par le listener (joueur y pose des items gold)
        // On n'écrase pas s'ils ont déjà un item, on met juste le placeholder
        if (inv.getItem(TradeSession.SLOT_GOLD_A) == null) {
            inv.setItem(TradeSession.SLOT_GOLD_A, make(Material.GOLD_INGOT,
                "§6Mise A", List.of("§7Déposez de l'or ici")));
        }
        if (inv.getItem(TradeSession.SLOT_GOLD_B) == null) {
            inv.setItem(TradeSession.SLOT_GOLD_B, make(Material.GOLD_INGOT,
                "§6Mise B", List.of("§7Déposez de l'or ici")));
        }

        // ── Statut central (slot 31) ──────────────────────────────────────
        inv.setItem(TradeSession.SLOT_STATUS, buildStatusItem(session));

        // ── Boutons Confirmer / Annuler ───────────────────────────────────
        boolean confirmedA = session.getState() == TradeSession.State.CONFIRMED_A
            || session.getState() == TradeSession.State.DONE;
        boolean confirmedB = session.getState() == TradeSession.State.CONFIRMED_B
            || session.getState() == TradeSession.State.DONE;

        Player pA = Bukkit.getPlayer(session.getPlayerA());
        Player pB = Bukkit.getPlayer(session.getPlayerB());

        inv.setItem(TradeSession.SLOT_CONFIRM_A, make(
            confirmedA ? Material.LIME_WOOL : Material.GRAY_WOOL,
            confirmedA ? "§a✔ Confirmé" : "§aConfirmer",
            List.of("§7" + (pA != null ? pA.getName() : "?"))
        ));
        inv.setItem(TradeSession.SLOT_CANCEL_A, make(Material.RED_WOOL,
            "§cAnnuler", List.of("§7Annule le troc")));

        inv.setItem(TradeSession.SLOT_CONFIRM_B, make(
            confirmedB ? Material.LIME_WOOL : Material.GRAY_WOOL,
            confirmedB ? "§a✔ Confirmé" : "§aConfirmer",
            List.of("§7" + (pB != null ? pB.getName() : "?"))
        ));
        inv.setItem(TradeSession.SLOT_CANCEL_B, make(Material.RED_WOOL,
            "§cAnnuler", List.of("§7Annule le troc")));
    }

    private static ItemStack buildStatusItem(TradeSession session) {
        return switch (session.getState()) {
            case OPEN         -> make(Material.YELLOW_WOOL, "§eEn attente…",
                List.of("§7Les deux joueurs doivent", "§7confirmer pour échanger."));
            case CONFIRMED_A  -> make(Material.LIME_WOOL, "§aJoueur A a confirmé",
                List.of("§7En attente de B…"));
            case CONFIRMED_B  -> make(Material.LIME_WOOL, "§aJoueur B a confirmé",
                List.of("§7En attente de A…"));
            case DONE         -> make(Material.EMERALD, "§a§lÉchange validé !",
                List.of());
            default           -> make(Material.RED_WOOL, "§cAnnulé", List.of());
        };
    }

    private static ItemStack make(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); meta.setLore(lore); item.setItemMeta(meta); }
        return item;
    }
}
