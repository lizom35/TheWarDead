package fr.thewardead.economy.command;

import fr.thewardead.economy.EconomyPlugin;
import fr.thewardead.economy.model.TradeSession;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TradeCommand implements CommandExecutor {

    private final EconomyPlugin plugin;

    public TradeCommand(EconomyPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage(c(pre() + "&cJoueur uniquement.")); return true; }
        var tm = plugin.getTradeManager();

        if (args.length == 0) {
            p.sendMessage(c(pre() + "&cUsage : /trade <joueur> | /trade accept"));
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            java.util.UUID fromUUID = tm.getPendingRequestFrom(p.getUniqueId());
            if (fromUUID == null) { p.sendMessage(c(pre() + "&cAucune invitation de troc en attente.")); return true; }

            Player from = Bukkit.getPlayer(fromUUID);
            if (from == null) {
                tm.removePendingRequest(fromUUID);
                p.sendMessage(c(pre() + "&cCe joueur n'est plus connecté."));
                return true;
            }
            if (tm.isInTrade(p.getUniqueId()) || tm.isInTrade(fromUUID)) {
                p.sendMessage(c(pre() + "&cL'un de vous est déjà en train de troquer."));
                return true;
            }
            tm.removePendingRequest(fromUUID);
            TradeSession session = tm.openSession(from, p);
            plugin.getTradeGUI().open(session);
            from.sendMessage(c(pre() + "&e" + p.getName() + " §aa accepté votre invitation de troc."));
            p.sendMessage(c(pre() + "&aVous avez accepté le troc avec §e" + from.getName() + "&a."));
            return true;
        }

        // Envoyer une invitation
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { p.sendMessage(c(pre() + "&cJoueur introuvable.")); return true; }
        if (target.equals(p)) { p.sendMessage(c(pre() + "&cVous ne pouvez pas vous troquer vous-même.")); return true; }

        if (tm.isInTrade(p.getUniqueId())) { p.sendMessage(c(pre() + "&cVous êtes déjà en train de troquer.")); return true; }
        if (tm.isInTrade(target.getUniqueId())) { p.sendMessage(c(pre() + "&c" + target.getName() + " est déjà en train de troquer.")); return true; }

        tm.sendRequest(p.getUniqueId(), target.getUniqueId());
        p.sendMessage(c(pre() + "&aInvitation envoyée à §e" + target.getName() + "&a."));
        target.sendMessage(c(pre() + "§e" + p.getName()
            + " §avous invite à troquer. Tapez &6/trade accept &apour accepter."));
        return true;
    }

    private String pre() { return plugin.getConfig().getString("messages.prefix", "&8[&6₩&8] &r"); }
    private String c(String s) { return org.bukkit.ChatColor.translateAlternateColorCodes('&', s); }
}
