package fr.thewardead.kingdoms.manager;

import fr.thewardead.kingdoms.KingdomPlugin;
import fr.thewardead.kingdoms.model.ChunkCoord;
import fr.thewardead.kingdoms.model.Kingdom;
import fr.thewardead.kingdoms.model.KingdomLevel;
import fr.thewardead.kingdoms.model.KingdomPlayer;
import fr.thewardead.kingdoms.model.KingdomRole;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class DataManager {

    private final KingdomPlugin plugin;
    private final File kingdomsFile;
    private final File playersFile;

    public DataManager(KingdomPlugin plugin) {
        this.plugin = plugin;
        this.kingdomsFile = new File(plugin.getDataFolder(), "kingdoms.yml");
        this.playersFile  = new File(plugin.getDataFolder(), "players.yml");
    }

    // ---- Save ----

    public void saveKingdoms(Map<UUID, Kingdom> kingdoms) {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Kingdom k : kingdoms.values()) {
            String path = k.getId().toString();
            cfg.set(path + ".name",     k.getName());
            cfg.set(path + ".chief",    k.getChiefUUID().toString());
            cfg.set(path + ".bank",     k.getBank());
            cfg.set(path + ".entryFee", k.getEntryFee());
            cfg.set(path + ".taxRate",  k.getTaxRate());
            cfg.set(path + ".level",    k.getLevel().name());

            for (Map.Entry<UUID, KingdomRole> entry : k.getMembers().entrySet()) {
                cfg.set(path + ".members." + entry.getKey(), entry.getValue().name());
            }

            if (k.getHome() != null) {
                Location h = k.getHome();
                cfg.set(path + ".home.world", h.getWorld().getName());
                cfg.set(path + ".home.x",     h.getX());
                cfg.set(path + ".home.y",     h.getY());
                cfg.set(path + ".home.z",     h.getZ());
                cfg.set(path + ".home.yaw",   (double) h.getYaw());
                cfg.set(path + ".home.pitch", (double) h.getPitch());
            }

            // Territoire : liste de "world:x:z"
            List<String> terrList = k.getTerritory().stream()
                .map(ChunkCoord::serialize)
                .collect(Collectors.toList());
            cfg.set(path + ".territory", terrList);
        }
        try { cfg.save(kingdomsFile); } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder kingdoms.yml : " + e.getMessage());
        }
    }

    public void savePlayers(Map<UUID, KingdomPlayer> players) {
        YamlConfiguration cfg = new YamlConfiguration();
        for (KingdomPlayer p : players.values()) {
            String path = p.getUuid().toString();
            cfg.set(path + ".name",      p.getName());
            cfg.set(path + ".kingdom",   p.getKingdomId() != null ? p.getKingdomId().toString() : null);
            cfg.set(path + ".balance",   p.getBalance());
            cfg.set(path + ".firstJoin", p.isFirstJoin());
        }
        try { cfg.save(playersFile); } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder players.yml : " + e.getMessage());
        }
    }

    // ---- Load ----

    public Map<UUID, Kingdom> loadKingdoms() {
        Map<UUID, Kingdom> result = new HashMap<>();
        if (!kingdomsFile.exists()) return result;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(kingdomsFile);
        for (String key : cfg.getKeys(false)) {
            try {
                UUID id       = UUID.fromString(key);
                String name   = cfg.getString(key + ".name");
                UUID chief    = UUID.fromString(cfg.getString(key + ".chief"));
                long bank     = cfg.getLong(key + ".bank");
                int entryFee  = cfg.getInt(key + ".entryFee");
                int taxRate   = cfg.getInt(key + ".taxRate");

                Kingdom k = new Kingdom(id, name, chief);
                k.depositBank(bank);
                k.setEntryFee(entryFee);
                k.setTaxRate(taxRate);

                // Level
                String levelStr = cfg.getString(key + ".level", "HAMEAU");
                try { k.setLevel(KingdomLevel.valueOf(levelStr)); } catch (Exception ignored) {}

                // Members
                if (cfg.isConfigurationSection(key + ".members")) {
                    for (String memberKey : cfg.getConfigurationSection(key + ".members").getKeys(false)) {
                        UUID memberUUID = UUID.fromString(memberKey);
                        KingdomRole role = KingdomRole.valueOf(cfg.getString(key + ".members." + memberKey));
                        if (!memberUUID.equals(chief)) k.addMember(memberUUID);
                        k.setRole(memberUUID, role);
                    }
                }

                // Home
                if (cfg.isConfigurationSection(key + ".home")) {
                    String worldName = cfg.getString(key + ".home.world");
                    double x   = cfg.getDouble(key + ".home.x");
                    double y   = cfg.getDouble(key + ".home.y");
                    double z   = cfg.getDouble(key + ".home.z");
                    float yaw   = (float) cfg.getDouble(key + ".home.yaw");
                    float pitch = (float) cfg.getDouble(key + ".home.pitch");
                    Location home = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
                    k.setHome(home);
                }

                // Territoire
                for (String s : cfg.getStringList(key + ".territory")) {
                    try { k.addChunk(ChunkCoord.deserialize(s)); } catch (Exception ignored) {}
                }

                result.put(id, k);
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur chargement royaume " + key + " : " + e.getMessage());
            }
        }
        return result;
    }

    public Map<UUID, KingdomPlayer> loadPlayers() {
        Map<UUID, KingdomPlayer> result = new HashMap<>();
        if (!playersFile.exists()) return result;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(playersFile);
        for (String key : cfg.getKeys(false)) {
            try {
                UUID uuid        = UUID.fromString(key);
                String name      = cfg.getString(key + ".name", "Unknown");
                String kStr      = cfg.getString(key + ".kingdom");
                long balance     = cfg.getLong(key + ".balance");
                boolean first    = cfg.getBoolean(key + ".firstJoin", true);

                KingdomPlayer p = new KingdomPlayer(uuid, name);
                if (kStr != null) p.setKingdomId(UUID.fromString(kStr));
                p.deposit(balance);
                p.setFirstJoin(first);

                result.put(uuid, p);
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur chargement joueur " + key + " : " + e.getMessage());
            }
        }
        return result;
    }
}
