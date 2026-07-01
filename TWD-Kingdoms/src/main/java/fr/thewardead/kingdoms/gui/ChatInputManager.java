package fr.thewardead.kingdoms.gui;

import fr.thewardead.kingdoms.KingdomPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Permet de capturer la prochaine saisie chat d'un joueur
 * pour remplacer les commandes textuelles par une interaction GUI.
 */
public class ChatInputManager implements Listener {

    private final KingdomPlugin plugin;
    private final Map<UUID, Consumer<String>> pending = new HashMap<>();

    public ChatInputManager(KingdomPlugin plugin) {
        this.plugin = plugin;
    }

    public void request(Player player, String prompt, Consumer<String> callback) {
        player.closeInventory();
        String pre = org.bukkit.ChatColor.translateAlternateColorCodes('&',
            plugin.getConfig().getString("messages.prefix", "&8[&cTWD&8] &r"));
        player.sendMessage(pre + org.bukkit.ChatColor.YELLOW + prompt);
        player.sendMessage(pre + org.bukkit.ChatColor.GRAY + "(Tapez " + org.bukkit.ChatColor.RED + "annuler" + org.bukkit.ChatColor.GRAY + " pour annuler)");
        pending.put(player.getUniqueId(), callback);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Consumer<String> callback = pending.remove(uuid);
        if (callback == null) return;

        event.setCancelled(true);
        String input = event.getMessage().trim();

        if (input.equalsIgnoreCase("annuler")) {
            String pre = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.prefix", "&8[&cTWD&8] &r"));
            Bukkit.getScheduler().runTask(plugin, () ->
                event.getPlayer().sendMessage(pre + org.bukkit.ChatColor.RED + "Action annulée."));
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(input));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }
}
