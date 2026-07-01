package fr.thewardead.progression.listener;

import fr.thewardead.progression.ProgressionPlugin;
import fr.thewardead.progression.level.MilitaireLevel;
import fr.thewardead.progression.model.JobClass;
import fr.thewardead.progression.model.ProgressionPlayer;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashMap;
import java.util.Map;

public class MilitaireListener implements Listener {

    private static final Map<Class<? extends LivingEntity>, Integer> MOB_XP = new HashMap<>();

    static {
        MOB_XP.put(Zombie.class,          8);
        MOB_XP.put(ZombieVillager.class,  8);
        MOB_XP.put(Drowned.class,         8);
        MOB_XP.put(Husk.class,            8);
        MOB_XP.put(PigZombie.class,       10); // ZombifiedPiglin — nom API Spigot
        MOB_XP.put(Skeleton.class,        10);
        MOB_XP.put(Stray.class,           10);
        MOB_XP.put(WitherSkeleton.class,  15);
        MOB_XP.put(Spider.class,          8);
        MOB_XP.put(CaveSpider.class,      8);
        MOB_XP.put(Creeper.class,         12);
        MOB_XP.put(Enderman.class,        20);
        MOB_XP.put(Pillager.class,        15);
        MOB_XP.put(Vindicator.class,      15);
        MOB_XP.put(Evoker.class,          20);
        MOB_XP.put(Phantom.class,         12);
        MOB_XP.put(Blaze.class,           20);
        MOB_XP.put(Ghast.class,           25);
        MOB_XP.put(Slime.class,           5);
        MOB_XP.put(MagmaCube.class,       8);
        MOB_XP.put(Witch.class,           15);
        MOB_XP.put(Guardian.class,        15);
        MOB_XP.put(ElderGuardian.class,   200);
        MOB_XP.put(Wither.class,          500);  // WitherBoss → Wither
        MOB_XP.put(EnderDragon.class,     300);
    }

    private final ProgressionPlugin plugin;

    public MilitaireListener(ProgressionPlugin plugin) {
        this.plugin = plugin;
    }

    // ── XP par kill mob / joueur ──────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        ProgressionPlayer pp = plugin.getProgressionManager().get(killer.getUniqueId());
        if (pp == null || pp.getJobClass() != JobClass.MILITAIRE) return;

        long xp;
        if (event.getEntity() instanceof Player) {
            xp = 75;
        } else {
            xp = MOB_XP.entrySet().stream()
                .filter(e -> e.getKey().isInstance(event.getEntity()))
                .mapToLong(Map.Entry::getValue)
                .findFirst()
                .orElse(0L);
        }

        if (xp == 0) return;

        plugin.getProgressionManager().addXp(killer.getUniqueId(), xp);

        MilitaireLevel lv = plugin.getProgressionManager().getMilitaireLevel(pp);
        if (lv.getWarsPerKill() > 0) {
            plugin.getProgressionManager().giveWars(killer.getUniqueId(), lv.getWarsPerKill());
        }
    }

    // ── Bonus dégâts ─────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        // Bonus dégâts attaquant
        if (event.getDamager() instanceof Player attacker) {
            ProgressionPlayer pp = plugin.getProgressionManager().get(attacker.getUniqueId());
            if (pp != null && pp.getJobClass() == JobClass.MILITAIRE) {
                MilitaireLevel lv = plugin.getProgressionManager().getMilitaireLevel(pp);
                if (lv.getDamageBonus() > 0) {
                    event.setDamage(event.getDamage() * (1.0 + lv.getDamageBonus()));
                }
            }
        }

        // Réduction dégâts défenseur
        if (event.getEntity() instanceof Player defender) {
            ProgressionPlayer pp = plugin.getProgressionManager().get(defender.getUniqueId());
            if (pp != null && pp.getJobClass() == JobClass.MILITAIRE) {
                MilitaireLevel lv = plugin.getProgressionManager().getMilitaireLevel(pp);
                if (lv.getDamageReduction() > 0) {
                    event.setDamage(event.getDamage() * (1.0 - lv.getDamageReduction()));
                }
            }
        }
    }
}
