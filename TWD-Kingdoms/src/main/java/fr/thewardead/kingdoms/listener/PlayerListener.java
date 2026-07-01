package fr.thewardead.kingdoms.listener;

import fr.thewardead.kingdoms.KingdomPlugin;
import fr.thewardead.kingdoms.gui.FirstConnectGUI;
import fr.thewardead.kingdoms.gui.ReconnectGUI;
import fr.thewardead.kingdoms.model.KingdomPlayer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final KingdomPlugin plugin;
    private final FirstConnectGUI firstConnectGUI;
    private final ReconnectGUI reconnectGUI;

    public PlayerListener(KingdomPlugin plugin) {
        this.plugin = plugin;
        this.firstConnectGUI = new FirstConnectGUI(plugin);
        this.reconnectGUI    = new ReconnectGUI(plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        KingdomPlayer kp = plugin.getKingdomManager().getOrCreatePlayer(player);

        // Mettre à jour le nom (en cas de changement de pseudo)
        kp.setName(player.getName());

        // Ouvrir la GUI après un léger délai (évite les conflits de chargement)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            if (kp.isFirstJoin()) {
                kp.setFirstJoin(false);
                plugin.getKingdomManager().save();
                firstConnectGUI.open(player);
            } else {
                reconnectGUI.open(player);
            }
        }, 20L); // 1 seconde
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getKingdomManager().save();
        if (plugin.getHudTask() != null) {
            plugin.getHudTask().removeBoard(event.getPlayer().getUniqueId());
        }
    }
}
