package fr.thewardead.kingdoms.listener;

import fr.thewardead.kingdoms.KingdomPlugin;
import fr.thewardead.kingdoms.model.Kingdom;
import fr.thewardead.kingdoms.model.KingdomRole;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashSet;
import java.util.UUID;

public class ChatListener implements Listener {

    private final KingdomPlugin plugin;

    public ChatListener(KingdomPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);

        Player sender = event.getPlayer();
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(sender.getUniqueId());

        if (kingdom == null) {
            sender.sendMessage(color("&c❌ &7Tu dois appartenir à un royaume pour parler dans le chat."));
            return;
        }

        KingdomRole role = kingdom.getRole(sender.getUniqueId());
        String rolePrefix = role != null ? roleColor(role) + "[" + role.getDisplayName() + "] " : "";
        String formatted = color(
            "&8[&b" + kingdom.getName() + "&8] " +
            rolePrefix +
            "&f" + sender.getName() +
            "&8: &7" + event.getMessage()
        );

        // Copie pour éviter ConcurrentModificationException depuis le thread async
        for (UUID memberId : new HashSet<>(kingdom.getMemberUUIDs())) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) member.sendMessage(formatted);
        }

        plugin.getLogger().info("[" + kingdom.getName() + "] " + sender.getName() + ": " + event.getMessage());
    }

    private String roleColor(KingdomRole role) {
        switch (role) {
            case CHEF:     return "§c";
            case OFFICIER: return "§6";
            default:       return "§7";
        }
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
