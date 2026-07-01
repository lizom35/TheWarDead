package fr.thewardead.progression.listener;

import fr.thewardead.progression.ProgressionPlugin;
import fr.thewardead.progression.gui.ClassChoiceGUI;
import fr.thewardead.progression.model.JobClass;
import fr.thewardead.progression.model.ProgressionPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinListener implements Listener {

    private final ProgressionPlugin plugin;

    public JoinListener(ProgressionPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        ProgressionPlayer pp = plugin.getProgressionManager().getOrCreate(p);
        pp.setName(p.getName());

        // Ouvrir le GUI de choix si pas encore de métier
        if (!pp.hasClass()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) ClassChoiceGUI.open(p);
            }, 40L); // 2s de délai
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getProgressionManager().save();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (!ClassChoiceGUI.TITLE.equals(event.getView().getTitle())) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        JobClass chosen = null;
        if (slot == ClassChoiceGUI.SLOT_FERMIER)   chosen = JobClass.FERMIER;
        if (slot == ClassChoiceGUI.SLOT_MILITAIRE) chosen = JobClass.MILITAIRE;
        if (chosen == null) return;

        boolean ok = plugin.getProgressionManager().chooseClass(p, chosen);
        p.closeInventory();

        if (ok) {
            p.sendTitle(
                chosen.getColor() + "§l" + chosen.getDisplayName(),
                "§fVotre métier a été choisi !",
                10, 60, 20
            );
            p.sendMessage(c("§8[§6TWD§8] §eMétier choisi : "
                + chosen.getColor() + "§l" + chosen.getDisplayName()));
            p.sendMessage(c("§7" + chosen.getBonusDesc()));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        }
    }

    private String c(String s) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }
}
