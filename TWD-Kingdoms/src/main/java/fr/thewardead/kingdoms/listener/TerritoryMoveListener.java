package fr.thewardead.kingdoms.listener;

import fr.thewardead.kingdoms.KingdomPlugin;
import fr.thewardead.kingdoms.model.ChunkCoord;
import fr.thewardead.kingdoms.model.Kingdom;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TerritoryMoveListener implements Listener {

    private final KingdomPlugin plugin;
    /** UUID du joueur → UUID du royaume dans lequel il se trouve (null = zone neutre). */
    private final Map<UUID, UUID> playerKingdom = new HashMap<>();

    public TerritoryMoveListener(KingdomPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;

        // Ignorer les mouvements sans changement de chunk
        if (event.getFrom().getBlockX() >> 4 == event.getTo().getBlockX() >> 4
                && event.getFrom().getBlockZ() >> 4 == event.getTo().getBlockZ() >> 4) return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        ChunkCoord toCoord = new ChunkCoord(
            event.getTo().getWorld().getName(),
            event.getTo().getBlockX() >> 4,
            event.getTo().getBlockZ() >> 4
        );

        Kingdom newKingdom = plugin.getKingdomManager().getKingdomAt(toCoord);
        UUID newId  = newKingdom != null ? newKingdom.getId() : null;
        UUID prevId = playerKingdom.get(playerId);

        if (java.util.Objects.equals(newId, prevId)) return;
        playerKingdom.put(playerId, newId);

        if (newKingdom != null) {
            Kingdom own = plugin.getKingdomManager().getPlayerKingdom(playerId);
            boolean isOwn = own != null && own.getId().equals(newId);

            String title    = color(isOwn ? "&b⚔ &f" : "&e⚔ &f") + newKingdom.getName();
            String subtitle = color(isOwn
                ? "&7Bienvenue dans votre royaume"
                : "&7Territoire de &e" + newKingdom.getName() + " &7— prudence !");

            player.sendTitle(title, subtitle, 10, 60, 20);

        } else if (prevId != null) {
            Kingdom prev = plugin.getKingdomManager().getKingdom(prevId);
            String name = prev != null ? prev.getName() : "ce territoire";
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(color("&7— Zone neutre — &8(quitte &e" + name + "&8)")));
            player.sendTitle("", "", 0, 1, 10); // efface le titre précédent rapidement
        }
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
