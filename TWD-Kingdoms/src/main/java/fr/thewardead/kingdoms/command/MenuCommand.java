package fr.thewardead.kingdoms.command;

import fr.thewardead.kingdoms.KingdomPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MenuCommand implements CommandExecutor {

    private final KingdomPlugin plugin;

    public MenuCommand(KingdomPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }
        plugin.getPlayerMenuGUI().open(p);
        return true;
    }
}
