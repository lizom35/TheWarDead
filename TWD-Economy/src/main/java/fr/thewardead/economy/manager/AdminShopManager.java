package fr.thewardead.economy.manager;

import fr.thewardead.economy.EconomyPlugin;
import fr.thewardead.economy.model.AdminListing;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AdminShopManager {

    private final EconomyPlugin plugin;
    private final File file;
    private final Map<UUID, AdminListing> listings = new LinkedHashMap<>();

    public AdminShopManager(EconomyPlugin plugin) {
        this.plugin = plugin;
        this.file   = new File(plugin.getDataFolder(), "romarket.yml");
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public AdminListing add(ItemStack item, long price) {
        UUID id = UUID.randomUUID();
        AdminListing l = new AdminListing(id, item, price);
        listings.put(id, l);
        save();
        return l;
    }

    public boolean remove(UUID id) {
        if (listings.remove(id) == null) return false;
        save();
        return true;
    }

    public boolean setPrice(UUID id, long price) {
        AdminListing l = listings.get(id);
        if (l == null) return false;
        l.setPrice(price);
        save();
        return true;
    }

    public AdminListing get(UUID id) { return listings.get(id); }

    public List<AdminListing> getAll() {
        return new ArrayList<>(listings.values());
    }

    // ── Persistance ────────────────────────────────────────────────────────────

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (AdminListing l : listings.values()) {
            String p = l.getId().toString();
            cfg.set(p + ".price", l.getPricePerUnit());
            cfg.set(p + ".item",  l.getTemplate());
        }
        try { cfg.save(file); }
        catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder romarket.yml : " + e.getMessage());
        }
    }

    // ── Seed (import Ballistix) ────────────────────────────────────────────────

    /**
     * Importe les articles depuis romarket-seed.yml.
     * @param reset si true, vide le marché avant l'import
     * @return nombre d'articles ajoutés
     */
    public int importSeed(boolean reset) {
        File seedFile = new File(plugin.getDataFolder(), "romarket-seed.yml");
        if (!seedFile.exists()) {
            plugin.getLogger().warning("romarket-seed.yml introuvable — lancez d'abord le serveur pour l'extraire.");
            return 0;
        }

        // Construire une map NamespacedKey → Material en une seule itération
        Map<String, Material> keyMap = new HashMap<>();
        for (Material m : Material.values()) {
            keyMap.put(m.getKey().toString(), m);
        }

        if (reset) {
            listings.clear();
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(seedFile);
        List<?> seeds = cfg.getList("seeds", java.util.Collections.emptyList());

        int added = 0;
        for (Object rawObj : seeds) {
            if (!(rawObj instanceof Map<?, ?> entry)) continue;
            String rawId = (String) entry.get("id");
            String rawName = (String) entry.get("name");
            long price;
            try { price = ((Number) entry.get("price")).longValue(); }
            catch (Exception e) { plugin.getLogger().warning("Prix manquant pour " + rawId); continue; }

            // Résolution du matériau par NamespacedKey
            Material mat = keyMap.get(rawId);
            if (mat == null) {
                // Fallback : tente uppercase avec '_' (ex: BALLISTIX_BULLET)
                String[] parts = rawId.split(":", 2);
                if (parts.length == 2) {
                    String candidate = (parts[0] + "_" + parts[1]).toUpperCase(Locale.ENGLISH);
                    mat = Material.getMaterial(candidate);
                }
            }
            if (mat == null) {
                plugin.getLogger().warning("[Seed] Matériau introuvable : " + rawId + " (mod chargé ?)");
                continue;
            }

            ItemStack item = new ItemStack(mat);
            if (rawName != null && !rawName.isBlank()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', rawName));
                    item.setItemMeta(meta);
                }
            }

            UUID id = UUID.randomUUID();
            listings.put(id, new AdminListing(id, item, price));
            added++;
        }

        if (added > 0) save();
        plugin.getLogger().info("[Seed] " + added + "/" + seeds.size() + " articles importés dans le Marché Admin.");
        return added;
    }

    public void load() {
        listings.clear();
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String key : cfg.getKeys(false)) {
            try {
                UUID id        = UUID.fromString(key);
                long price     = cfg.getLong(key + ".price");
                ItemStack item = cfg.getItemStack(key + ".item");
                if (item == null) continue;
                listings.put(id, new AdminListing(id, item, price));
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur chargement romarket " + key + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("Chargé " + listings.size() + " articles dans le Marché Admin.");
    }
}
