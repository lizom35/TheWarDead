package fr.thewardead.progression.command;

import fr.thewardead.progression.ProgressionPlugin;
import fr.thewardead.progression.level.FermierLevel;
import fr.thewardead.progression.level.MilitaireLevel;
import fr.thewardead.progression.model.JobClass;
import fr.thewardead.progression.model.ProgressionPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ClassCommand implements CommandExecutor, TabCompleter {

    private final ProgressionPlugin plugin;

    public ClassCommand(ProgressionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cCommande réservée aux joueurs.");
            return true;
        }

        if (args.length == 0) {
            showInfo(p);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info"   -> showInfo(p);
            case "top"    -> showTop(p);
            case "choose" -> openChoiceGUI(p);
            case "xp"     -> showXp(p);
            default       -> sendHelp(p, label);
        }
        return true;
    }

    // ── Sous-commandes ────────────────────────────────────────────────────────

    private void showInfo(Player p) {
        ProgressionPlayer pp = plugin.getProgressionManager().get(p.getUniqueId());
        if (pp == null || !pp.hasClass()) {
            p.sendMessage(c("§8[TWD] §cVous n'avez pas encore choisi de métier. Utilisez §f/class choose§c."));
            return;
        }

        String color     = pp.getJobClass().getColor();
        String levelName = plugin.getProgressionManager().getLevelName(pp);
        String bonuses   = plugin.getProgressionManager().getBonusSummary(pp);
        long   xpToNext  = xpToNext(pp);
        long   totalXpNv = totalXpForNext(pp);

        p.sendMessage(c("§8§m                              "));
        p.sendMessage(c("  " + color + "§l" + pp.getJobClass().getDisplayName()
            + " §8— §fniv. " + pp.getLevel() + " §7(" + levelName + ")"));
        p.sendMessage(c("  §7XP totale : §f" + pp.getXp()));
        if (pp.getLevel() < 10) {
            p.sendMessage(c("  §7Prochain palier dans : §f" + xpToNext + " XP §8(/" + totalXpNv + " requis)"));
        } else {
            p.sendMessage(c("  §6✦ Niveau maximum atteint !"));
        }
        p.sendMessage(c("  §7Bonus actifs : §f" + bonuses));
        p.sendMessage(c("§8§m                              "));
    }

    private void showTop(Player p) {
        p.sendMessage(c("§8§m                              "));
        p.sendMessage(c("  §6§lClassement — Métiers"));

        List<ProgressionPlayer> sorted = plugin.getProgressionManager().all().stream()
            .filter(ProgressionPlayer::hasClass)
            .sorted(Comparator.comparingLong(ProgressionPlayer::getXp).reversed())
            .limit(10)
            .collect(Collectors.toList());

        if (sorted.isEmpty()) {
            p.sendMessage(c("  §7Aucun joueur classé."));
        } else {
            int rank = 1;
            for (ProgressionPlayer pp : sorted) {
                String color = pp.getJobClass().getColor();
                String medal = rank == 1 ? "§6#1" : rank == 2 ? "§7#2" : rank == 3 ? "§c#3" : "§8#" + rank;
                p.sendMessage(c("  " + medal + " §f" + pp.getName()
                    + " §8— " + color + pp.getJobClass().getDisplayName()
                    + " §7niv." + pp.getLevel() + " §8(" + pp.getXp() + " XP)"));
                rank++;
            }
        }
        p.sendMessage(c("§8§m                              "));
    }

    private void openChoiceGUI(Player p) {
        ProgressionPlayer pp = plugin.getProgressionManager().get(p.getUniqueId());
        if (pp != null && pp.hasClass()) {
            p.sendMessage(c("§8[TWD] §cVous avez déjà un métier : "
                + pp.getJobClass().getColor() + "§l" + pp.getJobClass().getDisplayName()));
            return;
        }
        fr.thewardead.progression.gui.ClassChoiceGUI.open(p);
    }

    private void showXp(Player p) {
        ProgressionPlayer pp = plugin.getProgressionManager().get(p.getUniqueId());
        if (pp == null || !pp.hasClass()) {
            p.sendMessage(c("§8[TWD] §cVous n'avez pas de métier. Utilisez §f/class choose§c."));
            return;
        }

        String color     = pp.getJobClass().getColor();
        long   xpToNext  = xpToNext(pp);
        long   totalXpNv = totalXpForNext(pp);

        p.sendMessage(c("  " + color + "§l" + pp.getJobClass().getDisplayName()
            + " §8niv." + pp.getLevel() + " — §fXP : " + pp.getXp()));
        if (pp.getLevel() < 10) {
            p.sendMessage(c("  §7Prochain palier : §f" + xpToNext + " XP§7 restantes §8(seuil : " + totalXpNv + ")"));
        } else {
            p.sendMessage(c("  §6✦ Niveau maximum atteint !"));
        }
    }

    private void sendHelp(Player p, String label) {
        p.sendMessage(c("§8[TWD] §7/" + label + " §f<info|top|choose|xp>"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private long xpToNext(ProgressionPlayer pp) {
        return pp.getJobClass() == JobClass.FERMIER
            ? FermierLevel.forXp(pp.getXp()).xpToNext(pp.getXp())
            : MilitaireLevel.forXp(pp.getXp()).xpToNext(pp.getXp());
    }

    private long totalXpForNext(ProgressionPlayer pp) {
        if (pp.getJobClass() == JobClass.FERMIER) {
            FermierLevel next = FermierLevel.forXp(pp.getXp()).next();
            return next == null ? pp.getXp() : next.getXpRequired();
        } else {
            MilitaireLevel next = MilitaireLevel.forXp(pp.getXp()).next();
            return next == null ? pp.getXp() : next.getXpRequired();
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("info", "top", "choose", "xp").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        return List.of();
    }

    private String c(String s) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }
}
