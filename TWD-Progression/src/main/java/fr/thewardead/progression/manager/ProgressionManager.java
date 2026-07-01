package fr.thewardead.progression.manager;

import fr.thewardead.progression.ProgressionPlugin;
import fr.thewardead.progression.level.FermierLevel;
import fr.thewardead.progression.level.MilitaireLevel;
import fr.thewardead.progression.model.JobClass;
import fr.thewardead.progression.model.ProgressionPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

public class ProgressionManager {

    private final ProgressionPlugin plugin;
    private final Map<UUID, ProgressionPlayer> players = new LinkedHashMap<>();
    private static final String DATA_FILE = "playerdata.yml";

    public ProgressionManager(ProgressionPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Accès joueur ─────────────────────────────────────────────────────────

    public ProgressionPlayer getOrCreate(Player p) {
        return players.computeIfAbsent(p.getUniqueId(),
            id -> new ProgressionPlayer(id, p.getName(), null, 0, 1));
    }

    public ProgressionPlayer get(UUID uuid) { return players.get(uuid); }

    public Collection<ProgressionPlayer> all() {
        return Collections.unmodifiableCollection(players.values());
    }

    // ── Choix du métier ───────────────────────────────────────────────────────

    public boolean chooseClass(Player p, JobClass jobClass) {
        ProgressionPlayer pp = getOrCreate(p);
        if (pp.hasClass()) return false;
        pp.setJobClass(jobClass);
        save();
        return true;
    }

    // ── Gain d'XP ────────────────────────────────────────────────────────────

    public void addXp(UUID uuid, long amount) {
        ProgressionPlayer pp = players.get(uuid);
        if (pp == null || !pp.hasClass() || amount <= 0) return;

        pp.addXp(amount);

        int newLevel = calcLevel(pp);
        if (newLevel > pp.getLevel()) {
            int old = pp.getLevel();
            pp.setLevel(newLevel);
            save();
            onLevelUp(uuid, old, newLevel);
        }
    }

    private int calcLevel(ProgressionPlayer pp) {
        return pp.getJobClass() == JobClass.FERMIER
            ? FermierLevel.forXp(pp.getXp()).getNum()
            : MilitaireLevel.forXp(pp.getXp()).getNum();
    }

    private void onLevelUp(UUID uuid, int oldLevel, int newLevel) {
        Player p = Bukkit.getPlayer(uuid);
        if (p == null) return;
        ProgressionPlayer pp = players.get(uuid);

        String levelName = getLevelName(pp);
        String bonuses   = getBonusSummary(pp);
        String color     = pp.getJobClass().getColor();

        // Titre écran
        p.sendTitle(
            color + "§lNiveau " + newLevel + " !",
            "§f" + levelName,
            10, 70, 20
        );

        // Messages chat
        p.sendMessage(c("§8&m                              "));
        p.sendMessage(c("  " + color + "§l✦ Nouveau palier atteint ! §r" + color + levelName + " §8(niv. " + newLevel + ")"));
        p.sendMessage(c("  §7Bonus : §f" + bonuses));
        p.sendMessage(c("§8&m                              "));

        // Son
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

        // XP Minecraft offerte (récompense visuelle)
        p.giveExpLevels(newLevel);
    }

    // ── Accesseurs niveaux ────────────────────────────────────────────────────

    public FermierLevel   getFermierLevel(ProgressionPlayer pp) {
        return FermierLevel.forXp(pp.getXp());
    }

    public MilitaireLevel getMilitaireLevel(ProgressionPlayer pp) {
        return MilitaireLevel.forXp(pp.getXp());
    }

    public String getLevelName(ProgressionPlayer pp) {
        if (!pp.hasClass()) return "Sans métier";
        return pp.getJobClass() == JobClass.FERMIER
            ? getFermierLevel(pp).getDisplayName()
            : getMilitaireLevel(pp).getDisplayName();
    }

    public String getBonusSummary(ProgressionPlayer pp) {
        if (!pp.hasClass()) return "—";
        return pp.getJobClass() == JobClass.FERMIER
            ? getFermierLevel(pp).bonusSummary()
            : getMilitaireLevel(pp).bonusSummary();
    }

    // ── Économie (soft-depend TWD-Economy via reflection) ────────────────────

    /** Donne des Wars au joueur si TWD-Economy est chargé. */
    public void giveWars(UUID uuid, long amount) {
        if (amount <= 0) return;
        try {
            var econPlugin = Bukkit.getPluginManager().getPlugin("TWD-Economy");
            if (econPlugin == null) return;
            Method getManager = econPlugin.getClass().getMethod("getEconomyManager");
            Object manager    = getManager.invoke(econPlugin);
            Method deposit    = manager.getClass().getMethod("deposit", UUID.class, long.class);
            deposit.invoke(manager, uuid, amount);
        } catch (Exception ignored) {}
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (ProgressionPlayer pp : players.values()) {
            String path = "players." + pp.getUuid();
            yml.set(path + ".name",   pp.getName());
            yml.set(path + ".class",  pp.hasClass() ? pp.getJobClass().name() : null);
            yml.set(path + ".xp",     pp.getXp());
            yml.set(path + ".level",  pp.getLevel());
        }
        try {
            yml.save(new File(plugin.getDataFolder(), DATA_FILE));
        } catch (IOException e) {
            plugin.getLogger().warning("Sauvegarde impossible : " + e.getMessage());
        }
    }

    public void load() {
        players.clear();
        File file = new File(plugin.getDataFolder(), DATA_FILE);
        if (!file.exists()) return;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yml.getConfigurationSection("players");
        if (section == null) return;

        for (String uuidStr : section.getKeys(false)) {
            try {
                UUID    uuid      = UUID.fromString(uuidStr);
                String  name      = section.getString(uuidStr + ".name", "");
                String  className = section.getString(uuidStr + ".class", null);
                long    xp        = section.getLong(uuidStr + ".xp", 0);
                int     level     = section.getInt(uuidStr + ".level", 1);
                JobClass jobClass = className != null ? JobClass.valueOf(className) : null;
                players.put(uuid, new ProgressionPlayer(uuid, name, jobClass, xp, level));
            } catch (Exception e) {
                plugin.getLogger().warning("Joueur corrompu ignoré : " + uuidStr);
            }
        }
        plugin.getLogger().info("Chargé " + players.size() + " profils de progression.");
    }

    private String c(String s) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }
}
