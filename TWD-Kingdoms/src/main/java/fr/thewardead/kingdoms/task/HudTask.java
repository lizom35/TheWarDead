package fr.thewardead.kingdoms.task;

import fr.thewardead.kingdoms.KingdomPlugin;
import fr.thewardead.kingdoms.model.Kingdom;
import fr.thewardead.kingdoms.model.KingdomPlayer;
import fr.thewardead.kingdoms.model.KingdomRole;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class HudTask extends BukkitRunnable {

    /**
     * Clés invisibles utilisées comme identifiants d'entrée dans le scoreboard.
     * Chaque score (0-10) a sa propre clé unique (couleur + reset).
     */
    private static final String[] KEYS = {
        "§0§r", "§1§r", "§2§r", "§3§r", "§4§r",
        "§5§r", "§6§r", "§7§r", "§8§r", "§9§r", "§a§r"
    };

    private final KingdomPlugin plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    public HudTask(KingdomPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            update(player);
        }
    }

    private void update(Player player) {
        KingdomPlayer kp = plugin.getKingdomManager().getPlayer(player.getUniqueId());
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());

        Scoreboard board = boards.computeIfAbsent(player.getUniqueId(), id -> {
            Scoreboard b = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(b);
            return b;
        });

        if (player.getScoreboard() != board) {
            player.setScoreboard(board);
        }

        Objective obj = board.getObjective("twd_hud");
        if (obj == null) {
            obj = registerObjective(board);
        }

        long balance = kp != null ? kp.getBalance() : 0;
        int line = 10;

        setLine(board, obj, line--, color("&8&m──────────────"));

        if (kingdom != null) {
            KingdomRole role = kingdom.getRole(player.getUniqueId());
            String roleDisplay = role != null
                ? "  " + roleColor(role) + role.getDisplayName()
                : "  §7—";

            setLine(board, obj, line--, color("&e💰 &fPortefeuille"));
            setLine(board, obj, line--, color("  &a" + balance + " &f₩"));
            setLine(board, obj, line--, "§b§r");

            setLine(board, obj, line--, color("&b🏰 &fRoyaume"));
            setLine(board, obj, line--, color("  &b" + kingdom.getName()));
            setLine(board, obj, line--, color(roleDisplay));
            setLine(board, obj, line--, "§c§r");

            setLine(board, obj, line--, color("&d⏳ &fProchaine paye"));
            long secs = IncomeTask.getSecondsUntilNextIncome();
            setLine(board, obj, line--, color("  &d" + formatTimer(secs)));
        } else {
            setLine(board, obj, line--, color("&e💰 &fPortefeuille"));
            setLine(board, obj, line--, color("  &a" + balance + " &f₩"));
            setLine(board, obj, line--, "§b§r");
            setLine(board, obj, line--, color("&c❌ &fSans royaume"));
            setLine(board, obj, line--, color("  &7/kingdom join <nom>"));
        }

        setLine(board, obj, line, color("&8&m──────────────"));
        clearBelow(board, obj, line - 1);

        updateTabName(player, kingdom);
    }

    private void updateTabName(Player player, Kingdom kingdom) {
        if (kingdom != null) {
            KingdomRole role = kingdom.getRole(player.getUniqueId());
            String prefix = roleColor(role) + "[" + kingdom.getName() + "] ";
            player.setPlayerListName(color(prefix + "&f" + player.getName()));
        } else {
            player.setPlayerListName(color("&7" + player.getName()));
        }
    }

    private void setLine(Scoreboard board, Objective obj, int score, String text) {
        String key = KEYS[score];
        Team team = board.getTeam("hudline" + score);
        if (team == null) {
            team = board.registerNewTeam("hudline" + score);
            team.addEntry(key);
        }
        if (!text.equals(team.getPrefix())) {
            team.setPrefix(text);
        }
        obj.getScore(key).setScore(score);
    }

    @SuppressWarnings("deprecation")
    private Objective registerObjective(Scoreboard board) {
        Objective obj = board.registerNewObjective("twd_hud", "dummy",
            color("&6&l⚔ The War Dead"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        return obj;
    }

    private void clearBelow(Scoreboard board, Objective obj, int below) {
        for (String entry : new HashSet<>(board.getEntries())) {
            Score s = obj.getScore(entry);
            if (s.isScoreSet() && s.getScore() <= below) {
                board.resetScores(entry);
            }
        }
    }

    public void removeBoard(UUID uuid) {
        Scoreboard board = boards.remove(uuid);
        if (board != null) {
            Objective obj = board.getObjective("twd_hud");
            if (obj != null) obj.unregister();
        }
    }

    private String roleColor(KingdomRole role) {
        if (role == null) return "§7";
        switch (role) {
            case CHEF:     return "§c";
            case OFFICIER: return "§6";
            default:       return "§7";
        }
    }

    private String formatTimer(long seconds) {
        if (seconds <= 0) return "Maintenant !";
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return String.format("%dh %02dm", h, m);
        return String.format("%dm %02ds", m, s);
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void start(KingdomPlugin plugin) {
        new HudTask(plugin).runTaskTimer(plugin, 20L, 20L);
    }
}
