package fr.thewardead.economy.command;

import fr.thewardead.economy.EconomyPlugin;
import fr.thewardead.economy.manager.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class BalanceCommand implements CommandExecutor {

    private final EconomyPlugin plugin;
    private final EconomyManager em;

    public BalanceCommand(EconomyPlugin plugin) {
        this.plugin = plugin;
        this.em     = plugin.getEconomyManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String name = cmd.getName().toLowerCase();

        return switch (name) {
            case "balance" -> handleBalance(sender, args);
            case "pay"     -> handlePay(sender, args);
            case "baltop"  -> handleBaltop(sender);
            default -> false;
        };
    }

    private boolean handleBalance(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(c(pre() + "&cUn joueur uniquement."));
            return true;
        }
        Player target = args.length > 0 ? Bukkit.getPlayerExact(args[0]) : p;
        if (target == null) { p.sendMessage(c(pre() + "&cJoueur introuvable.")); return true; }

        long bal = em.getBalance(target.getUniqueId());
        p.sendMessage(c(pre() + "&7Solde de §e" + target.getName() + " &7: §6" + bal + "₩"));
        return true;
    }

    private boolean handlePay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(c(pre() + "&cUn joueur uniquement."));
            return true;
        }
        if (args.length < 2) {
            p.sendMessage(c(pre() + "&cUsage : /pay <joueur> <montant>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { p.sendMessage(c(pre() + "&cJoueur introuvable.")); return true; }
        if (target.equals(p)) { p.sendMessage(c(pre() + "&cVous ne pouvez pas vous payer vous-même.")); return true; }

        long amount;
        try { amount = Long.parseLong(args[1]); } catch (NumberFormatException e) {
            p.sendMessage(c(pre() + "&cMontant invalide."));
            return true;
        }
        if (amount <= 0) { p.sendMessage(c(pre() + "&cMontant invalide.")); return true; }

        int taxPct = plugin.getConfig().getInt("transaction-tax-percent", 5);
        long tax  = amount * taxPct / 100;
        long net  = amount - tax;

        if (!em.transfer(p.getUniqueId(), target.getUniqueId(), amount)) {
            p.sendMessage(c(pre() + "&cSolde insuffisant. (solde : &6" + em.getBalance(p.getUniqueId()) + "₩&c)"));
            return true;
        }

        p.sendMessage(c(pre() + "&aVous avez envoyé &6" + amount + "₩ &a(taxe &e" + tax + "₩&a) à §e" + target.getName() + "&a."));
        target.sendMessage(c(pre() + "&aVous avez reçu &6" + net + "₩ &ade §e" + p.getName() + "&a."));
        return true;
    }

    private boolean handleBaltop(CommandSender sender) {
        List<Map.Entry<String, Long>> top = em.getTopBalances(10);
        sender.sendMessage(c("§6§l── Top 10 Fortunes (₩) ──"));
        for (int i = 0; i < top.size(); i++) {
            var e = top.get(i);
            sender.sendMessage(c("§e" + (i + 1) + ". §f" + e.getKey() + " §7: §6" + e.getValue() + "₩"));
        }
        if (top.isEmpty()) sender.sendMessage(c("§7Aucun joueur avec un solde."));
        return true;
    }

    private String pre() { return plugin.getConfig().getString("messages.prefix", "&8[&6₩&8] &r"); }
    private String c(String s) { return org.bukkit.ChatColor.translateAlternateColorCodes('&', s); }
}
