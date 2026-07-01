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

/**
 * GUI de vente d'item.
 *
 * Layout 45 slots (5 lignes × 9) :
 *
 *   Col:  0        1  2  3     4       5  6  7     8
 *   R0: [+1QTY]  [f][f][f]  [ITEM]  [f][f][f]  [+5PRI]
 *   R1: [-1QTY]  [f][f][f]  [QTY]   [f][f][f]  [-5PRI]
 *   R2: [MAX]    [f][f][f]  [PRIX]  [f][f][f]  [CHATPRI]
 *   R3: [CHATQTY][f][f][f]  [TOTAL] [f][f][f]  [f]
 *   R4: [CANCEL] [f][f][f]  [CONF]  [f][f][f]  [f]
 *
 * L'inventaire du joueur est affiché en bas (Bukkit standard).
 * Le joueur clique sur un item de son propre inventaire pour le sélectionner.
 */
public class ShopSellGUI {

    public static final String TITLE = "§6§lVendre un item";

    // ── Slots ────────────────────────────────────────────────────────────────
    private static final int SLOT_QTY_P1   = 0;
    private static final int SLOT_QTY_M1   = 9;
    private static final int SLOT_QTY_MAX  = 18;
    private static final int SLOT_QTY_CHAT = 27;

    private static final int SLOT_ITEM     = 4;
    private static final int SLOT_QTY_DISP = 13;
    private static final int SLOT_PRI_DISP = 22;
    private static final int SLOT_TOTAL    = 31;

    private static final int SLOT_PRI_P5   = 8;
    private static final int SLOT_PRI_M5   = 17;
    private static final int SLOT_PRI_CHAT = 26;

    private static final int SLOT_CANCEL   = 36;
    private static final int SLOT_CONFIRM  = 40;

    // ── État ─────────────────────────────────────────────────────────────────

    public static class SellState {
        public ItemStack selectedItem = null; // template 1-unité
        public int maxQty  = 0;
        public int quantity = 1;
        public long pricePerUnit = 0L;
        public boolean awaitingChatInput = false;
        /** Type de listing (PERSONAL ou KINGDOM). */
        public ShopListing.ListingType type = ShopListing.ListingType.PERSONAL;
        public UUID kingdomId = null;
    }

    private final Map<UUID, SellState> states = new HashMap<>();
    private final EconomyPlugin plugin;

    public ShopSellGUI(EconomyPlugin plugin) { this.plugin = plugin; }

    // ── Ouverture ────────────────────────────────────────────────────────────

    /** Ouverture normale (personal). */
    public void open(Player player) { open(player, ShopListing.ListingType.PERSONAL, null); }

    /** Ouverture pour un shop royaume. */
    public void openKingdom(Player player, UUID kingdomId) {
        open(player, ShopListing.ListingType.KINGDOM, kingdomId);
    }

    private void open(Player player, ShopListing.ListingType type, UUID kingdomId) {
        SellState state = new SellState();
        state.type      = type;
        state.kingdomId = kingdomId;
        states.put(player.getUniqueId(), state);
        player.openInventory(build(state));
    }

    /** Rouvre le GUI après une saisie chat (préserve le state). */
    public void reopen(Player player) {
        SellState state = states.get(player.getUniqueId());
        if (state == null) { open(player); return; }
        state.awaitingChatInput = false;
        player.openInventory(build(state));
    }

    // ── Sélection d'item depuis l'inventaire joueur ───────────────────────────

    /**
     * Appelé quand le joueur clique sur un item de SON inventaire
     * (rawSlot >= 45 pour un GUI 45-slots).
     */
    public void selectItem(Player player, ItemStack clicked) {
        SellState state = states.get(player.getUniqueId());
        if (state == null) return;

        if (clicked == null || clicked.getType().isAir()) return;

        ItemStack template = clicked.clone();
        template.setAmount(1);
        state.selectedItem = template;
        state.maxQty  = countInInventory(player, template);
        state.quantity = Math.min(Math.max(state.quantity, 1), state.maxQty);

        refreshTopInventory(player, state);
    }

    // ── Gestion des clics sur le GUI du haut ─────────────────────────────────

    public void handleClick(Player player, int rawSlot) {
        SellState state = states.get(player.getUniqueId());
        if (state == null) return;

        switch (rawSlot) {
            case SLOT_QTY_P1   -> adjustQty(player, state, +1);
            case SLOT_QTY_M1   -> adjustQty(player, state, -1);
            case SLOT_QTY_MAX  -> setQtyMax(player, state);
            case SLOT_QTY_CHAT -> askQtyInChat(player, state);
            case SLOT_PRI_P5   -> adjustPrice(player, state, +5);
            case SLOT_PRI_M5   -> adjustPrice(player, state, -5);
            case SLOT_PRI_CHAT -> askPriceInChat(player, state);
            case SLOT_CANCEL   -> { clearState(player.getUniqueId()); player.closeInventory(); }
            case SLOT_CONFIRM  -> confirm(player, state);
        }
    }

    // ── Ajustements ──────────────────────────────────────────────────────────

    private void adjustQty(Player player, SellState state, int delta) {
        if (state.selectedItem == null) {
            player.sendMessage(c(pre() + "&cChoisissez d'abord un item en cliquant dessus en bas."));
            return;
        }
        state.quantity = Math.min(Math.max(state.quantity + delta, 1), state.maxQty);
        refreshTopInventory(player, state);
    }

    private void setQtyMax(Player player, SellState state) {
        if (state.selectedItem == null) {
            player.sendMessage(c(pre() + "&cChoisissez d'abord un item en cliquant dessus en bas."));
            return;
        }
        state.quantity = state.maxQty;
        refreshTopInventory(player, state);
    }

    private void askQtyInChat(Player player, SellState state) {
        if (state.selectedItem == null) {
            player.sendMessage(c(pre() + "&cChoisissez d'abord un item en cliquant dessus en bas."));
            return;
        }
        state.awaitingChatInput = true;
        plugin.getKingdomPlugin().getChatInputManager().request(player,
            "Entrez la quantité (1 – " + state.maxQty + ") :",
            input -> {
                try {
                    int val = Integer.parseInt(input.trim());
                    state.quantity = Math.min(Math.max(val, 1), state.maxQty);
                } catch (NumberFormatException e) {
                    player.sendMessage(c(pre() + "&cNombre invalide, quantité inchangée."));
                }
                reopen(player);
            });
    }

    private void adjustPrice(Player player, SellState state, long delta) {
        state.pricePerUnit = Math.max(0L, state.pricePerUnit + delta);
        refreshTopInventory(player, state);
    }

    private void askPriceInChat(Player player, SellState state) {
        state.awaitingChatInput = true;
        plugin.getKingdomPlugin().getChatInputManager().request(player,
            "Entrez le prix par unité (en ₩) :",
            input -> {
                try {
                    long val = Long.parseLong(input.trim());
                    if (val < 0) throw new NumberFormatException();
                    state.pricePerUnit = val;
                } catch (NumberFormatException e) {
                    player.sendMessage(c(pre() + "&cNombre invalide, prix inchangé."));
                }
                reopen(player);
            });
    }

    // ── Confirmation ─────────────────────────────────────────────────────────

    private void confirm(Player player, SellState state) {
        if (state.selectedItem == null) {
            player.sendMessage(c(pre() + "&cSélectionnez d'abord un item."));
            return;
        }
        if (state.quantity <= 0) {
            player.sendMessage(c(pre() + "&cQuantité invalide."));
            return;
        }
        if (state.pricePerUnit <= 0) {
            player.sendMessage(c(pre() + "&cDéfinissez un prix supérieur à 0₩."));
            return;
        }

        int maxListings = plugin.getConfig().getInt("shop.max-listings-per-player", 10);
        if (plugin.getShopManager().countActiveByPlayer(player.getUniqueId()) >= maxListings) {
            player.sendMessage(c(pre() + "&cQuota atteint (" + maxListings + " annonces max)."));
            clearState(player.getUniqueId());
            player.closeInventory();
            return;
        }

        // Retirer exactement state.quantity de l'inventaire
        int remaining = state.quantity;
        for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot != null && slot.isSimilar(state.selectedItem)) {
                int take = Math.min(slot.getAmount(), remaining);
                remaining -= take;
                int newAmt = slot.getAmount() - take;
                if (newAmt <= 0) player.getInventory().setItem(i, null);
                else             slot.setAmount(newAmt);
            }
        }

        if (remaining > 0) {
            player.sendMessage(c(pre() + "&cPas assez d'items en inventaire."));
            clearState(player.getUniqueId());
            player.closeInventory();
            return;
        }

        plugin.getShopManager().createListing(
            player.getUniqueId(), player.getName(),
            state.selectedItem, state.quantity, state.pricePerUnit,
            state.type, state.kingdomId
        );

        player.sendMessage(c(pre() + "&aAnnonce créée : §e" + state.quantity + "x "
            + itemName(state.selectedItem) + " §aà §6" + state.pricePerUnit + "₩/u."));

        clearState(player.getUniqueId());
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin,
            () -> plugin.getShopBrowserGUI().openPersonal(player, 0), 2L);
    }

    // ── Rendu ────────────────────────────────────────────────────────────────

    /** Construit un inventaire complet depuis l'état. */
    private Inventory build(SellState state) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE);

        ItemStack filler = glass(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++) inv.setItem(i, filler);

        // ── Gauche : quantité ────────────────────────────────────────────────
        inv.setItem(SLOT_QTY_P1,   glass(Material.LIME_STAINED_GLASS_PANE,
            "§a+1 Quantité", "§7Augmenter de 1 la quantité"));
        inv.setItem(SLOT_QTY_M1,   glass(Material.RED_STAINED_GLASS_PANE,
            "§c-1 Quantité", "§7Diminuer de 1 la quantité"));
        inv.setItem(SLOT_QTY_MAX,  glass(Material.BLACK_STAINED_GLASS_PANE,
            "§fQuantité maximale",
            "§7Mettre la quantité max disponible",
            "§7dans votre inventaire"));
        inv.setItem(SLOT_QTY_CHAT, glass(Material.PURPLE_STAINED_GLASS_PANE,
            "§dEntrer la quantité",
            "§7Définir la quantité via le tchat"));

        // ── Centre : sélection & infos ────────────────────────────────────────
        if (state.selectedItem != null) {
            ItemStack display = state.selectedItem.clone();
            ItemMeta m = display.getItemMeta();
            if (m != null) {
                m.setLore(List.of(
                    "§7Item sélectionné",
                    "§7Stock dispo : §e" + state.maxQty
                ));
                display.setItemMeta(m);
            }
            inv.setItem(SLOT_ITEM, display);
        } else {
            inv.setItem(SLOT_ITEM, glass(Material.LIME_STAINED_GLASS_PANE,
                "§a§lCliquez sur un item",
                "§7Cliquez sur un item de votre",
                "§7inventaire en bas pour le",
                "§7sélectionner."));
        }

        inv.setItem(SLOT_QTY_DISP, quantity(state));
        inv.setItem(SLOT_PRI_DISP, prix(state));
        inv.setItem(SLOT_TOTAL,    total(state));

        // ── Droite : prix ─────────────────────────────────────────────────────
        inv.setItem(SLOT_PRI_P5,   glass(Material.LIME_STAINED_GLASS_PANE,
            "§a+5₩ Prix", "§7Augmenter le prix de 5₩"));
        inv.setItem(SLOT_PRI_M5,   glass(Material.RED_STAINED_GLASS_PANE,
            "§c-5₩ Prix", "§7Diminuer le prix de 5₩"));
        inv.setItem(SLOT_PRI_CHAT, glass(Material.BLACK_STAINED_GLASS_PANE,
            "§fEntrer le prix",
            "§7Définir le prix via le tchat"));

        // ── Bas : annuler / confirmer ─────────────────────────────────────────
        inv.setItem(SLOT_CANCEL, glass(Material.RED_WOOL,
            "§c§lAnnuler", "§7Ferme sans créer d'annonce."));

        boolean ready = state.selectedItem != null && state.quantity > 0 && state.pricePerUnit > 0;
        inv.setItem(SLOT_CONFIRM, glass(
            ready ? Material.LIME_WOOL : Material.GRAY_WOOL,
            ready ? "§a§lConfirmer la vente" : "§7Confirmez (item + prix requis)",
            ready
                ? "§7Quantité : §e" + state.quantity
                : "§cSélectionnez un item et définissez",
            ready
                ? "§7Prix : §6" + state.pricePerUnit + "₩/u"
                : "§cun prix avant de vendre."
        ));

        return inv;
    }

    /** Met à jour l'inventaire déjà ouvert sans fermer. */
    private void refreshTopInventory(Player player, SellState state) {
        Inventory open = player.getOpenInventory().getTopInventory();
        if (!TITLE.equals(player.getOpenInventory().getTitle())) return;
        Inventory fresh = build(state);
        for (int i = 0; i < 45; i++) open.setItem(i, fresh.getItem(i));
    }

    // ── Accesseurs ───────────────────────────────────────────────────────────

    public SellState getState(UUID uuid) { return states.get(uuid); }
    public void clearState(UUID uuid)    { states.remove(uuid); }
    public boolean hasState(UUID uuid)   { return states.containsKey(uuid); }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private int countInInventory(Player player, ItemStack template) {
        int count = 0;
        for (ItemStack s : player.getInventory().getContents()) {
            if (s != null && s.isSimilar(template)) count += s.getAmount();
        }
        return count;
    }

    private static ItemStack quantity(SellState state) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta m = item.getItemMeta();
        if (m != null) {
            m.setDisplayName(state.selectedItem != null
                ? "§eQuantité : §b" + state.quantity
                : "§7Quantité : §8(aucun item)");
            m.setLore(state.selectedItem != null
                ? List.of("§7Max disponible : §e" + state.maxQty)
                : List.of("§7Cliquez sur un item en bas."));
            item.setItemMeta(m);
        }
        return item;
    }

    private static ItemStack prix(SellState state) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta m = item.getItemMeta();
        if (m != null) {
            m.setDisplayName("§6Prix : §e" + state.pricePerUnit + "₩ §7/ unité");
            m.setLore(List.of("§7Utilisez les boutons à droite", "§7ou le bouton tchat noir."));
            item.setItemMeta(m);
        }
        return item;
    }

    private static ItemStack total(SellState state) {
        long t = state.quantity * state.pricePerUnit;
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta m = item.getItemMeta();
        if (m != null) {
            m.setDisplayName("§aBilan");
            m.setLore(List.of(
                "§7Quantité : §e" + state.quantity,
                "§7Prix/u   : §6" + state.pricePerUnit + "₩",
                "§7Total    : §a" + t + "₩",
                "",
                state.selectedItem != null && state.pricePerUnit > 0
                    ? "§a✔ Prêt à vendre !"
                    : "§cItem et prix requis."
            ));
            item.setItemMeta(m);
        }
        return item;
    }

    /** Construit un pane/wool coloré avec un nom et un lore optionnel. */
    private static ItemStack glass(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            if (lore.length > 0) m.setLore(Arrays.asList(lore));
            item.setItemMeta(m);
        }
        return item;
    }

    private static String itemName(ItemStack item) {
        if (item == null) return "?";
        ItemMeta m = item.getItemMeta();
        if (m != null && m.hasDisplayName()) return m.getDisplayName();
        return item.getType().name().replace('_', ' ').toLowerCase();
    }

    private String pre() { return plugin.getConfig().getString("messages.prefix", "&8[&6₩&8] &r"); }
    private String c(String s) { return org.bukkit.ChatColor.translateAlternateColorCodes('&', s); }
}
