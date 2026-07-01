package fr.thewardead.economy.command;

import fr.thewardead.economy.EconomyPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RomarketCommand implements CommandExecutor {

    private final EconomyPlugin plugin;

    public RomarketCommand(EconomyPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(c(pre() + "&cJoueur uniquement.")); return true;
        }

        // ── Sous-commandes admin ──────────────────────────────────────────────
        if (args.length > 0) {
            if (!p.hasPermission("twd.economy.admin") && !p.isOp()) {
                p.sendMessage(c(pre() + "&cVous n'avez pas la permission.")); return true;
            }
            switch (args[0].toLowerCase()) {
                case "manage", "gerer", "admin" -> {
                    plugin.getRomarketAdminGUI().open(p);
                    return true;
                }
                case "add", "ajouter" -> {
                    handleAdd(p, args);
                    return true;
                }
                case "remove", "supprimer" -> {
                    handleRemove(p, args);
                    return true;
                }
                case "setprice", "prix" -> {
                    handleSetPrice(p, args);
                    return true;
                }
                case "seed", "import" -> {
                    handleSeed(p, args);
                    return true;
                }
            }
        }

        // ── Ouverture GUI joueur ──────────────────────────────────────────────
        plugin.getRomarketGUI().open(p, 0);
        return true;
    }

    // ── Ajout d'item ─────────────────────────────────────────────────────────

    private void handleAdd(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(c(pre() + "&cUsage : /romarket add <prix>"));
            return;
        }
        long price;
        try { price = Long.parseLong(args[1]); }
        catch (NumberFormatException e) {
            p.sendMessage(c(pre() + "&cPrix invalide.")); return;
        }
        if (price <= 0) { p.sendMessage(c(pre() + "&cLe prix doit être > 0.")); return; }

        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            p.sendMessage(c(pre() + "&cTenez l'item à ajouter en main principale."));
            return;
        }

        var listing = plugin.getAdminShopManager().add(hand, price);
        p.sendMessage(c(pre() + "&aArticle ajouté au Marché Admin : §6"
            + price + "₩/u §a(ID : §8" + listing.getId().toString().substring(0, 8) + "&a)"));
    }

    // ── Suppression ──────────────────────────────────────────────────────────

    private void handleRemove(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(c(pre() + "&cUsage : /romarket remove <id-court|complet>"));
            return;
        }
        java.util.UUID id = resolveId(args[1]);
        if (id == null) { p.sendMessage(c(pre() + "&cArticle introuvable.")); return; }

        if (plugin.getAdminShopManager().remove(id))
            p.sendMessage(c(pre() + "&aArticle retiré du Marché Admin."));
        else
            p.sendMessage(c(pre() + "&cArticle introuvable."));
    }

    // ── Changement de prix ────────────────────────────────────────────────────

    private void handleSetPrice(Player p, String[] args) {
        if (args.length < 3) {
            p.sendMessage(c(pre() + "&cUsage : /romarket setprice <id> <prix>"));
            return;
        }
        java.util.UUID id = resolveId(args[1]);
        if (id == null) { p.sendMessage(c(pre() + "&cArticle introuvable.")); return; }

        long price;
        try { price = Long.parseLong(args[2]); }
        catch (NumberFormatException e) { p.sendMessage(c(pre() + "&cPrix invalide.")); return; }
        if (price <= 0) { p.sendMessage(c(pre() + "&cLe prix doit être > 0.")); return; }

        if (plugin.getAdminShopManager().setPrice(id, price))
            p.sendMessage(c(pre() + "&aPrix mis à jour : §6" + price + "₩/u."));
        else
            p.sendMessage(c(pre() + "&cArticle introuvable."));
    }

    // ── Import seed Ballistix ─────────────────────────────────────────────────

    private void handleSeed(Player p, String[] args) {
        boolean reset = args.length >= 2 && args[1].equalsIgnoreCase("reset");
        if (reset) {
            p.sendMessage(c(pre() + "&eRéinitialisation du marché + import du catalogue Ballistix..."));
        } else {
            p.sendMessage(c(pre() + "&eImport du catalogue Ballistix (articles existants conservés)..."));
        }
        int added = plugin.getAdminShopManager().importSeed(reset);
        p.sendMessage(c(pre() + "&a" + added + " article(s) ajouté(s) au Marché Admin."));
    }

    // ── Util ─────────────────────────────────────────────────────────────────

    /** Résout un UUID partiel (8 premiers chars) ou complet. */
    private java.util.UUID resolveId(String input) {
        try { return java.util.UUID.fromString(input); }
        catch (IllegalArgumentException ignored) {}

        return plugin.getAdminShopManager().getAll().stream()
            .filter(l -> l.getId().toString().startsWith(input))
            .map(fr.thewardead.economy.model.AdminListing::getId)
            .findFirst().orElse(null);
    }

    private String pre() { return plugin.getConfig().getString("messages.prefix", "&8[&6₩&8] &r"); }
    private String c(String s) { return org.bukkit.ChatColor.translateAlternateColorCodes('&', s); }
}
