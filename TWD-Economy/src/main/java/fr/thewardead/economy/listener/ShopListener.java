package fr.thewardead.economy.listener;

import fr.thewardead.economy.EconomyPlugin;
import fr.thewardead.economy.gui.KingdomShopGUI;
import fr.thewardead.economy.gui.ShopBrowserGUI;
import fr.thewardead.economy.gui.ShopOwnerGUI;
import fr.thewardead.economy.gui.ShopSellGUI;
import fr.thewardead.economy.model.ShopListing;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class ShopListener implements Listener {

    private final EconomyPlugin plugin;

    public ShopListener(EconomyPlugin plugin) { this.plugin = plugin; }

    // ── Clics ────────────────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();

        // ── ShopBrowserGUI ───────────────────────────────────────────────────
        if (title.equals(ShopBrowserGUI.TITLE_PERSONAL)
                || title.equals(ShopBrowserGUI.TITLE_KINGDOM)
                || title.equals(ShopBrowserGUI.TITLE_MARKET)) {
            e.setCancelled(true);
            handleBrowserClick(p, title, e.getRawSlot(), e.isShiftClick());
            return;
        }

        // ── ShopOwnerGUI ─────────────────────────────────────────────────────
        if (title.equals(ShopOwnerGUI.TITLE)) {
            e.setCancelled(true);
            handleOwnerClick(p, e.getRawSlot(), e.isShiftClick());
            return;
        }

        // ── ShopSellGUI ──────────────────────────────────────────────────────
        if (title.equals(ShopSellGUI.TITLE)) {
            int raw = e.getRawSlot();

            // Clic dans la zone du GUI (partie haute, slots 0-44) → boutons
            if (raw < 45) {
                e.setCancelled(true);
                plugin.getShopSellGUI().handleClick(p, raw);
                return;
            }

            // Clic dans l'inventaire du JOUEUR (slots ≥ 45) → sélection d'item
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked != null && !clicked.getType().isAir()) {
                plugin.getShopSellGUI().selectItem(p, clicked);
            }
            return;
        }

        // ── KingdomShopGUI ───────────────────────────────────────────────────
        if (title.equals(KingdomShopGUI.TITLE)) {
            e.setCancelled(true);
            handleKingdomShopClick(p, e.getRawSlot(), e.isShiftClick());
        }
    }

    // ── Fermetures ───────────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        String title = e.getView().getTitle();

        if (title.equals(ShopBrowserGUI.TITLE_PERSONAL)
                || title.equals(ShopBrowserGUI.TITLE_KINGDOM)
                || title.equals(ShopBrowserGUI.TITLE_MARKET)) {
            plugin.getShopBrowserGUI().clearTracking(p.getUniqueId());

        } else if (title.equals(ShopOwnerGUI.TITLE)) {
            plugin.getShopOwnerGUI().clearTracking(p.getUniqueId());

        } else if (title.equals(ShopSellGUI.TITLE)) {
            ShopSellGUI.SellState state = plugin.getShopSellGUI().getState(p.getUniqueId());
            // Si on attend une saisie chat, on garde le state pour rouvrir après
            if (state == null || !state.awaitingChatInput) {
                plugin.getShopSellGUI().clearState(p.getUniqueId());
            }

        } else if (title.equals(KingdomShopGUI.TITLE)) {
            plugin.getKingdomShopGUI().clearTracking(p.getUniqueId());
        }
    }

    // ── Browser ──────────────────────────────────────────────────────────────

    private void handleBrowserClick(Player p, String title, int slot, boolean shift) {
        var browser = plugin.getShopBrowserGUI();
        int page    = browser.getPage(p.getUniqueId());

        if (slot == 36 && page > 0) { navigate(browser, p, title, page - 1); return; }
        if (slot == 44)             { navigate(browser, p, title, page + 1); return; }

        // "Mon Shop" (slot 40, marché perso)
        if (slot == 40 && title.equals(ShopBrowserGUI.TITLE_PERSONAL)) {
            plugin.getShopOwnerGUI().open(p);
            return;
        }

        // Annonce (slots 0-35)
        if (slot < 0 || slot >= 36) return;
        List<ShopListing> listings = listingsFor(title, p);
        int idx = page * 36 + slot;
        if (idx >= listings.size()) return;
        purchase(p, listings.get(idx), shift ? listings.get(idx).getStock() : 1);
    }

    private void navigate(ShopBrowserGUI browser, Player p, String title, int page) {
        if (title.equals(ShopBrowserGUI.TITLE_KINGDOM))
            browser.openKingdom(p, browser.getKingdomId(p.getUniqueId()), page);
        else if (title.equals(ShopBrowserGUI.TITLE_MARKET))
            browser.openMarket(p, page);
        else
            browser.openPersonal(p, page);
    }

    private List<ShopListing> listingsFor(String title, Player p) {
        if (title.equals(ShopBrowserGUI.TITLE_KINGDOM)) {
            UUID kId = plugin.getShopBrowserGUI().getKingdomId(p.getUniqueId());
            return plugin.getShopManager().getActiveByKingdom(kId);
        }
        if (title.equals(ShopBrowserGUI.TITLE_MARKET))
            return plugin.getShopManager().getActiveModItems();
        return plugin.getShopManager().getActive();
    }

    private void purchase(Player buyer, ShopListing listing, int qty) {
        qty = Math.min(qty, listing.getStock());
        if (qty <= 0) return;

        long total = listing.getPricePerUnit() * qty;

        // Vérification solde (commune)
        if (!plugin.getEconomyManager().has(buyer.getUniqueId(), total)) {
            buyer.sendMessage(c(pre() + "&cSolde insuffisant (besoin : §6" + total + "₩&c).")); return;
        }

        if (listing.getType() == ShopListing.ListingType.KINGDOM) {
            // ── Shop Royaume : argent → banque du royaume (sans taxe) ──────────
            var kingdom = plugin.getKingdomManager().getKingdom(listing.getKingdomId());
            if (kingdom == null) {
                buyer.sendMessage(c(pre() + "&cLe royaume vendeur n'existe plus.")); return;
            }
            if (!listing.consume(qty)) {
                buyer.sendMessage(c(pre() + "&cStock insuffisant.")); return;
            }
            plugin.getEconomyManager().withdraw(buyer.getUniqueId(), total);
            kingdom.depositBank(total);
            plugin.getKingdomManager().save();
            plugin.getShopManager().save();

            buyer.getInventory().addItem(listing.build(qty)).values()
                .forEach(drop -> buyer.getWorld().dropItemNaturally(buyer.getLocation(), drop));
            buyer.sendMessage(c(pre() + "&aAchat au shop de §b" + kingdom.getName()
                + " §a: §e" + qty + "x §fitem §apour §6" + total + "₩."));

            // Notifier les officiers/chef en ligne
            for (UUID memberId : kingdom.getMemberUUIDs()) {
                var role = kingdom.getRole(memberId);
                if (role != fr.thewardead.kingdoms.model.KingdomRole.CHEF
                        && role != fr.thewardead.kingdoms.model.KingdomRole.OFFICIER) continue;
                var online = org.bukkit.Bukkit.getPlayer(memberId);
                if (online != null && !online.equals(buyer)) {
                    online.sendMessage(c(pre() + "§e" + buyer.getName()
                        + " §aa acheté §e" + qty + "x §fau shop. §a+§6" + total + "₩ §aen banque."));
                }
            }

        } else {
            // ── Shop Personnel : argent → joueur vendeur (avec taxe) ──────────
            if (listing.getSellerId().equals(buyer.getUniqueId())) {
                buyer.sendMessage(c(pre() + "&cVous ne pouvez pas acheter votre propre annonce.")); return;
            }
            if (!listing.consume(qty)) {
                buyer.sendMessage(c(pre() + "&cStock insuffisant.")); return;
            }
            plugin.getEconomyManager().transfer(buyer.getUniqueId(), listing.getSellerId(), total);
            plugin.getShopManager().save();

            buyer.getInventory().addItem(listing.build(qty)).values()
                .forEach(drop -> buyer.getWorld().dropItemNaturally(buyer.getLocation(), drop));
            buyer.sendMessage(c(pre() + "&aAchat : §e" + qty + "x §fitem §apour §6" + total + "₩."));

            var seller = org.bukkit.Bukkit.getPlayer(listing.getSellerId());
            if (seller != null) {
                int tax = plugin.getConfig().getInt("transaction-tax-percent", 5);
                seller.sendMessage(c(pre() + "§e" + buyer.getName()
                    + " §aa acheté §e" + qty + "x §fde vous pour §6"
                    + (total - total * tax / 100) + "₩."));
            }
        }

        plugin.getShopBrowserGUI().refresh(buyer);
    }

    // ── Owner shop ───────────────────────────────────────────────────────────

    private void handleOwnerClick(Player p, int slot, boolean shift) {
        // Slot 45 : Nouvelle annonce
        if (slot == 45) { plugin.getShopSellGUI().open(p); return; }
        // Slot 53 : Retour marché
        if (slot == 53) { plugin.getShopBrowserGUI().openPersonal(p, 0); return; }
        if (slot >= 45) return;

        UUID listingId = plugin.getShopOwnerGUI().getListingAt(p.getUniqueId(), slot);
        if (listingId == null) return;

        ShopListing listing = plugin.getShopManager().getListing(listingId);
        if (listing == null) { plugin.getShopOwnerGUI().open(p); return; }

        if (shift) {
            ItemStack items = listing.build(listing.getStock());
            p.getInventory().addItem(items).values()
                .forEach(drop -> p.getWorld().dropItemNaturally(p.getLocation(), drop));
            plugin.getShopManager().removeListing(listingId);
            p.sendMessage(c(pre() + "&aAnnonce retirée, §e" + listing.getStock() + " §aitems rendus."));
            plugin.getShopOwnerGUI().open(p);
        } else {
            p.sendMessage(c(pre() + "&7Shift+Clic pour retirer l'annonce et récupérer le stock."));
        }
    }

    // ── Kingdom shop ─────────────────────────────────────────────────────────

    private void handleKingdomShopClick(Player p, int slot, boolean shift) {
        var km = plugin.getKingdomManager();
        var kingdom = km.getPlayerKingdom(p.getUniqueId());

        // Slot 49 : Ajouter annonce royaume (officier/chef seulement)
        if (slot == 49) {
            if (kingdom == null) return;
            var role = kingdom.getRole(p.getUniqueId());
            if (role == fr.thewardead.kingdoms.model.KingdomRole.CHEF
                    || role == fr.thewardead.kingdoms.model.KingdomRole.OFFICIER) {
                plugin.getShopSellGUI().openKingdom(p, kingdom.getId());
            } else {
                p.sendMessage(c(pre() + "&cSeuls les Officiers et le Chef peuvent vendre."));
            }
            return;
        }

        // Slot 53 : Retour au marché
        if (slot == 53) { plugin.getShopBrowserGUI().openPersonal(p, 0); return; }

        // Slots 0-44 : acheter une annonce
        if (slot >= 45) return;
        UUID listingId = plugin.getKingdomShopGUI().getListingAt(p.getUniqueId(), slot);
        if (listingId == null) return;

        ShopListing listing = plugin.getShopManager().getListing(listingId);
        if (listing == null) { plugin.getKingdomShopGUI().open(p); return; }

        purchase(p, listing, shift ? listing.getStock() : 1);
        plugin.getKingdomShopGUI().open(p);
    }

    // ── Util ─────────────────────────────────────────────────────────────────

    private String pre() { return plugin.getConfig().getString("messages.prefix", "&8[&6₩&8] &r"); }
    private String c(String s) { return org.bukkit.ChatColor.translateAlternateColorCodes('&', s); }
}
