package fr.thewardead.kingdoms.task;

import fr.thewardead.kingdoms.KingdomPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class IncomeTask extends BukkitRunnable {

    private static long nextIncomeMillis = 0;

    private final KingdomPlugin plugin;

    public IncomeTask(KingdomPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getKingdomManager().distributeIncome();
        int minutes = plugin.getConfig().getInt("income-interval-minutes", 60);
        nextIncomeMillis = System.currentTimeMillis() + (long) minutes * 60_000;
    }

    /** Secondes restantes avant la prochaine paye (0 si inconnue ou passée). */
    public static long getSecondsUntilNextIncome() {
        long diff = nextIncomeMillis - System.currentTimeMillis();
        return Math.max(0, diff / 1000);
    }

    /** Lance la tâche périodique. intervalMinutes lu depuis config. */
    public static void start(KingdomPlugin plugin) {
        int minutes = plugin.getConfig().getInt("income-interval-minutes", 60);
        long ticks  = (long) minutes * 60 * 20;
        nextIncomeMillis = System.currentTimeMillis() + (long) minutes * 60_000;
        new IncomeTask(plugin).runTaskTimer(plugin, ticks, ticks);
    }
}
