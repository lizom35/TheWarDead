package fr.thewardead.economy.manager;

import fr.thewardead.economy.EconomyPlugin;
import fr.thewardead.economy.model.TradeSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TradeManager {

    private final EconomyPlugin plugin;
    /** playerUUID → session active */
    private final Map<UUID, TradeSession> sessions = new HashMap<>();
    /** playerA → playerB : invitations en attente */
    private final Map<UUID, UUID> pendingRequests = new HashMap<>();

    public TradeManager(EconomyPlugin plugin) {
        this.plugin = plugin;
        startTimeoutTask();
    }

    // ---- Invitations ----

    public void sendRequest(UUID from, UUID to) {
        pendingRequests.put(from, to);
    }

    public boolean hasPendingRequest(UUID from, UUID to) {
        return to.equals(pendingRequests.get(from));
    }

    public UUID getPendingRequestFrom(UUID to) {
        return pendingRequests.entrySet().stream()
            .filter(e -> e.getValue().equals(to))
            .map(Map.Entry::getKey)
            .findFirst().orElse(null);
    }

    public void removePendingRequest(UUID from) {
        pendingRequests.remove(from);
    }

    // ---- Sessions ----

    public boolean isInTrade(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public TradeSession getSession(UUID uuid) {
        return sessions.get(uuid);
    }

    public TradeSession openSession(Player playerA, Player playerB) {
        TradeSession session = new TradeSession(playerA.getUniqueId(), playerB.getUniqueId());
        sessions.put(playerA.getUniqueId(), session);
        sessions.put(playerB.getUniqueId(), session);
        return session;
    }

    public void closeSession(UUID uuid) {
        TradeSession session = sessions.remove(uuid);
        if (session != null) {
            // Retirer l'autre joueur aussi
            if (session.getPlayerA().equals(uuid)) sessions.remove(session.getPlayerB());
            else                                    sessions.remove(session.getPlayerA());
        }
    }

    // ---- Timeout automatique ----

    private void startTimeoutTask() {
        long timeoutSeconds = plugin.getConfig().getLong("trade.timeout-seconds", 120);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            sessions.entrySet().removeIf(entry -> {
                TradeSession s = entry.getValue();
                if (s.isTimedOut(timeoutSeconds)) {
                    cancelAndReturn(s, "Troc annulé (inactivité).");
                    return true;
                }
                return false;
            });
        }, 20L * 10, 20L * 10); // vérifie toutes les 10 secondes
    }

    public void cancelAndReturn(TradeSession session, String reason) {
        if (session.getState() == TradeSession.State.CANCELLED
            || session.getState() == TradeSession.State.DONE) return;

        session.setState(TradeSession.State.CANCELLED);

        for (UUID uuid : new UUID[]{session.getPlayerA(), session.getPlayerB()}) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;

            if (p.getOpenInventory().getTopInventory().equals(session.getInventory())) {
                p.closeInventory();
            }

            // Rendre les items offerts
            for (int slot : uuid.equals(session.getPlayerA())
                ? TradeSession.SLOTS_A : TradeSession.SLOTS_B) {
                var item = session.getInventory().getItem(slot);
                if (item != null) p.getInventory().addItem(item).values()
                    .forEach(drop -> p.getWorld().dropItemNaturally(p.getLocation(), drop));
            }

            p.sendMessage(c(pre() + "&c" + reason));
        }

        sessions.remove(session.getPlayerA());
        sessions.remove(session.getPlayerB());
    }

    private String pre() {
        return plugin.getConfig().getString("messages.prefix", "&8[&6₩&8] &r");
    }
    private String c(String s) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }
}
