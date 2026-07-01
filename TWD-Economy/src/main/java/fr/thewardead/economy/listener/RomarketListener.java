package fr.thewardead.economy.listener;

import fr.thewardead.economy.EconomyPlugin;
import fr.thewardead.economy.gui.RomarketAdminGUI;
import fr.thewardead.economy.gui.RomarketGUI;
import fr.thewardead.economy.model.AdminListing;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class RomarketListener implements Listener {

    private final EconomyPlugin plugin;

    public RomarketListener(EconomyPlugin plugin) { this.plugin = plugin; }

    // ── Clics ────────────────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();

        if (title.equals(RomarketGUI.TITLE)) {
            e.setCancelled(true);
            handleBrowserClick(p, e.getRawSlot(), e.isShiftClick());
            return;
        }

        if (title.equals(RomarketAdminGUI.TITLE)) {
            e.setCancelled(true);
            handleAdminClick(p, e.getRawSlot(), e.isRightClick());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        String title = e.getView().getTitle();
        // Pas de clearPage pour RomarketGUI : open() l'écrase lui-même, et
        // player.openInventory() déclenche ce close avant l'ouverture de la page
        // suivante — clearPage ici cassait la navigation (retour à 0 à chaque clic).
        if (title.equals(RomarketAdminGUI.TITLE))
            plugin.getRomarketAdminGUI().clearTracking(p.getUniqueId());
    }

    // ── Browser (vue joueur) ──────────────────────────────────────────────────

    private void handleBrowserClick(Player p, int slot, boolean shift) {
        var gui  = plugin.getRomarketGUI();
        int page = gui.getPage(p.getUniqueId());

        // Navigation
        if (slot == 36 && page > 0) { gui.open(p, page - 1); return; }
        if (slot == 44)             { gui.open(p, page + 1); return; }

        // Bouton admin (slot 40)
        if (slot == 40 && (p.hasPermission("twd.economy.admin") || p.isOp())) {
            plugin.getRomarketAdminGUI().open(p);
            return;
        }

        // Clic sur une annonce (slots 0-35)
        if (slot < 0 || slot >= 36) return;

        var all  = plugin.getAdminShopManager().getAll();
        int idx  = page * 36 + slot;
        if (idx >= all.size()) return;

        AdminListing listing = all.get(idx);
        int qty = shift ? 64 : 1;
        purchase(p, listing, qty);
    }

    private void purchase(Player buyer, AdminListing listing, int qty) {
        long total = listing.getPricePerUnit() * qty;

        if (!plugin.getEconomyManager().has(buyer.getUniqueId(), total)) {
            buyer.sendMessage(c(pre() + "&cSolde insuffisant (besoin : §6" + total + "₩&c)."));
            return;
        }

        // Retirer l'argent (admin-shop = argent détruit, pas de vendeur)
        plugin.getEconomyManager().withdraw(buyer.getUniqueId(), total);

        // Donner les items
        ItemStack items = listing.build(qty);
        buyer.getInventory().addItem(items).values()
            .forEach(drop -> buyer.getWorld().dropItemNaturally(buyer.getLocation(), drop));

        buyer.sendMessage(c(pre() + "&aAchat Marché : §e" + qty + "x §fitem §apour §6" + total + "₩."));

        // Rafraîchir la page
        plugin.getRomarketGUI().open(buyer, plugin.getRomarketGUI().getPage(buyer.getUniqueId()));
    }

    // ── GUI Admin ─────────────────────────────────────────────────────────────

    private void handleAdminClick(Player p, int slot, boolean rightClick) {
        if (!p.hasPermission("twd.economy.admin") && !p.isOp()) return;

        // Slot 45 : Ajouter l'item tenu en main
        if (slot == 45) {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand.getType().isAir()) {
                p.sendMessage(c(pre() + "&cTenez l'item à ajouter en main principale."));
                return;
            }
            // Demander le prix via tchat
            plugin.getKingdomPlugin().getChatInputManager().request(p,
                "Prix par unité (en ₩) pour §f" + itemName(hand) + "§e :",
                input -> {
                    long price;
                    try { price = Long.parseLong(input.trim()); }
                    catch (NumberFormatException ex) {
                        p.sendMessage(c(pre() + "&cNombre invalide. Annonce non créée.")); return;
                    }
                    if (price <= 0) {
                        p.sendMessage(c(pre() + "&cLe prix doit être > 0.")); return;
                    }
                    var listing = plugin.getAdminShopManager().add(hand, price);
                    p.sendMessage(c(pre() + "&aArticle ajouté : §6" + price + "₩/u §a(ID : §8"
                        + listing.getId().toString().substring(0, 8) + "&a)."));
                    plugin.getRomarketAdminGUI().open(p);
                });
            return;
        }

        // Slot 53 : Retour au marché
        if (slot == 53) { plugin.getRomarketGUI().open(p, 0); return; }

        // Slots 0-44 : article existant
        if (slot >= 45) return;

        UUID listingId = plugin.getRomarketAdminGUI().getListingAt(p.getUniqueId(), slot);
        if (listingId == null) return;

        AdminListing listing = plugin.getAdminShopManager().get(listingId);
        if (listing == null) { plugin.getRomarketAdminGUI().open(p); return; }

        if (rightClick) {
            // Clic droit → changer le prix via tchat
            plugin.getKingdomPlugin().getChatInputManager().request(p,
                "Nouveau prix pour §f" + itemName(listing.getTemplate()) + "§e (actuel : §6"
                    + listing.getPricePerUnit() + "₩&e) :",
                input -> {
                    long price;
                    try { price = Long.parseLong(input.trim()); }
                    catch (NumberFormatException ex) {
                        p.sendMessage(c(pre() + "&cNombre invalide.")); return;
                    }
                    if (price <= 0) { p.sendMessage(c(pre() + "&cLe prix doit être > 0.")); return; }
                    plugin.getAdminShopManager().setPrice(listingId, price);
                    p.sendMessage(c(pre() + "&aPrix mis à jour : §6" + price + "₩/u."));
                    plugin.getRomarketAdminGUI().open(p);
                });
        } else {
            // Clic gauche → supprimer
            plugin.getAdminShopManager().remove(listingId);
            p.sendMessage(c(pre() + "&aArticle supprimé du Marché Admin."));
            plugin.getRomarketAdminGUI().open(p);
        }
    }

    // ── Util ─────────────────────────────────────────────────────────────────

    private String itemName(ItemStack item) {
        if (item == null) return "?";
        var meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) return meta.getDisplayName();
        return item.getType().name().replace('_', ' ').toLowerCase();
    }

    private String pre() { return plugin.getConfig().getString("messages.prefix", "&8[&6₩&8] &r"); }
    private String c(String s) { return org.bukkit.ChatColor.translateAlternateColorCodes('&', s); }
}
