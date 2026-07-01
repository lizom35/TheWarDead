package fr.thewardead.economy.command;

import fr.thewardead.economy.EconomyPlugin;
import fr.thewardead.economy.model.ShopListing;
import fr.thewardead.kingdoms.model.Kingdom;
import fr.thewardead.kingdoms.model.KingdomRole;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RoshopCommand implements CommandExecutor {

    private final EconomyPlugin plugin;

    public RoshopCommand(EconomyPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage(c(pre() + "&cJoueur uniquement.")); return true; }

        Kingdom k = plugin.getKingdomManager().getPlayerKingdom(p.getUniqueId());
        if (k == null) { p.sendMessage(c(pre() + "&cVous n'avez pas de royaume.")); return true; }

        if (args.length == 0) {
            plugin.getKingdomShopGUI().open(p);
            return true;
        }

        if (args[0].equalsIgnoreCase("sell")) {
            KingdomRole role = k.getRole(p.getUniqueId());
            if (role != KingdomRole.CHEF && role != KingdomRole.OFFICIER) {
                p.sendMessage(c(pre() + "&cSeuls les officiers et le chef peuvent vendre."));
                return true;
            }
            // Déléguer à ShopCommand.sell avec le type KINGDOM
            plugin.getShopCommand().sell(p, args, ShopListing.ListingType.KINGDOM, k.getId());
        } else {
            p.sendMessage(c(pre() + "&cUsage : /roshop [sell <prix> [stock]]"));
        }
        return true;
    }

    private String pre() { return plugin.getConfig().getString("messages.prefix", "&8[&6₩&8] &r"); }
    private String c(String s) { return org.bukkit.ChatColor.translateAlternateColorCodes('&', s); }
}
