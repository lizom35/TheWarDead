package fr.thewardead.economy.command;

import fr.thewardead.economy.EconomyPlugin;
import fr.thewardead.economy.model.ShopListing;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ShopCommand implements CommandExecutor {

    private final EconomyPlugin plugin;

    public ShopCommand(EconomyPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage(c(pre() + "&cJoueur uniquement.")); return true; }

        if (args.length == 0) {
            // Ouvre le navigateur de marché
            plugin.getShopBrowserGUI().openPersonal(p, 0);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "sell" -> handleSell(p, args, ShopListing.ListingType.PERSONAL, null);
            case "remove" -> handleRemove(p, args);
            case "myshop" -> plugin.getShopOwnerGUI().open(p);
            default -> {
                p.sendMessage(c(pre() + "&cUsage : /shop [sell <prix> [stock]] [remove <id>] [myshop]"));
            }
        }
        return true;
    }

    private void handleSell(Player p, String[] args, ShopListing.ListingType type, java.util.UUID kingdomId) {
        if (args.length < 2) {
            p.sendMessage(c(pre() + "&cUsage : /shop sell <prix> [stock]"));
            return;
        }
        long price;
        try { price = Long.parseLong(args[1]); } catch (NumberFormatException e) {
            p.sendMessage(c(pre() + "&cPrix invalide.")); return;
        }
        if (price <= 0) { p.sendMessage(c(pre() + "&cLe prix doit être positif.")); return; }

        int maxListings = plugin.getConfig().getInt("shop.max-listings-per-player", 10);
        if (plugin.getShopManager().countActiveByPlayer(p.getUniqueId()) >= maxListings) {
            p.sendMessage(c(pre() + "&cQuota atteint (" + maxListings + " annonces max)."));
            return;
        }

        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            p.sendMessage(c(pre() + "&cTenez un item en main.")); return;
        }

        int maxStock = plugin.getConfig().getInt("shop.max-stock-per-listing", 1024);
        int stock    = args.length >= 3 ? Math.min(parseInt(args[2], hand.getAmount()), maxStock) : hand.getAmount();
        if (stock <= 0) { p.sendMessage(c(pre() + "&cStock invalide.")); return; }

        // Retirer les items de l'inventaire du joueur
        int taken = 0;
        for (int i = 0; i < p.getInventory().getSize() && taken < stock; i++) {
            ItemStack slot = p.getInventory().getItem(i);
            if (slot != null && slot.isSimilar(hand)) {
                int canTake = Math.min(slot.getAmount(), stock - taken);
                taken += canTake;
                slot.setAmount(slot.getAmount() - canTake);
            }
        }
        if (taken < stock) { p.sendMessage(c(pre() + "&cVous n'avez pas assez de cet item.")); return; }

        ItemStack template = hand.clone();
        template.setAmount(1);
        plugin.getShopManager().createListing(p.getUniqueId(), p.getName(), template, stock, price, type, kingdomId);
        p.sendMessage(c(pre() + "&aAnnonce créée : §e" + stock + "x §f" + typeName(hand)
            + " §aà §6" + price + "₩ §al'unité."));
    }

    private void handleRemove(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage(c(pre() + "&cUsage : /shop remove <id>")); return; }
        java.util.UUID id;
        try { id = java.util.UUID.fromString(args[1]); } catch (IllegalArgumentException e) {
            p.sendMessage(c(pre() + "&cID invalide.")); return;
        }
        var listing = plugin.getShopManager().getListing(id);
        if (listing == null) { p.sendMessage(c(pre() + "&cAnnonce introuvable.")); return; }
        if (!listing.getSellerId().equals(p.getUniqueId())) {
            p.sendMessage(c(pre() + "&cCe n'est pas votre annonce.")); return;
        }
        // Rendre le stock
        ItemStack toReturn = listing.build(listing.getStock());
        p.getInventory().addItem(toReturn).values()
            .forEach(drop -> p.getWorld().dropItemNaturally(p.getLocation(), drop));
        plugin.getShopManager().removeListing(id);
        p.sendMessage(c(pre() + "&aAnnonce retirée. Items rendus."));
    }

    // ── Util ────────────────────────────────────────────────────────────────

    /** Expose la méthode sell pour RoshopCommand. */
    public void sell(Player p, String[] args, ShopListing.ListingType type, java.util.UUID kingdomId) {
        handleSell(p, args, type, kingdomId);
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }

    private String typeName(ItemStack item) {
        var meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) return meta.getDisplayName();
        return item.getType().name().replace('_', ' ').toLowerCase();
    }

    private String pre() { return plugin.getConfig().getString("messages.prefix", "&8[&6₩&8] &r"); }
    private String c(String s) { return org.bukkit.ChatColor.translateAlternateColorCodes('&', s); }
}
