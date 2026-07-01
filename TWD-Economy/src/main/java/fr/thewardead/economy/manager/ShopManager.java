package fr.thewardead.economy.manager;

import fr.thewardead.economy.EconomyPlugin;
import fr.thewardead.economy.model.ShopListing;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ShopManager {

    private final EconomyPlugin plugin;
    private final File file;
    private final Map<UUID, ShopListing> listings = new LinkedHashMap<>();

    public ShopManager(EconomyPlugin plugin) {
        this.plugin = plugin;
        this.file   = new File(plugin.getDataFolder(), "shops.yml");
    }

    // ---- CRUD ----

    public ShopListing createListing(UUID sellerId, String sellerName,
                                     ItemStack item, int stock, long price,
                                     ShopListing.ListingType type, UUID kingdomId) {
        UUID id = UUID.randomUUID();
        ShopListing listing = new ShopListing(id, sellerId, sellerName, item, stock, price, type, kingdomId);
        listings.put(id, listing);
        save();
        return listing;
    }

    public boolean removeListing(UUID listingId) {
        if (listings.remove(listingId) == null) return false;
        save();
        return true;
    }

    public ShopListing getListing(UUID id) {
        return listings.get(id);
    }

    // ---- Queries ----

    public List<ShopListing> getActive() {
        return listings.values().stream()
            .filter(ShopListing::isActive)
            .sorted(Comparator.comparingLong(ShopListing::getCreatedAt).reversed())
            .collect(Collectors.toList());
    }

    public List<ShopListing> getActiveByType(ShopListing.ListingType type) {
        return getActive().stream()
            .filter(l -> l.getType() == type)
            .collect(Collectors.toList());
    }

    public List<ShopListing> getActiveBySeller(UUID sellerId) {
        return getActive().stream()
            .filter(l -> l.getSellerId().equals(sellerId))
            .collect(Collectors.toList());
    }

    public List<ShopListing> getActiveByKingdom(UUID kingdomId) {
        return getActive().stream()
            .filter(l -> l.getType() == ShopListing.ListingType.KINGDOM
                && kingdomId.equals(l.getKingdomId()))
            .collect(Collectors.toList());
    }

    /** Listings d'armes/items de mods (namespace != minecraft). */
    public List<ShopListing> getActiveModItems() {
        return getActive().stream()
            .filter(l -> !l.getTemplate().getType().getKey().getNamespace().equals("minecraft")
                || isWeapon(l.getTemplate()))
            .collect(Collectors.toList());
    }

    public int countActiveByPlayer(UUID sellerId) {
        return (int) listings.values().stream()
            .filter(l -> l.getSellerId().equals(sellerId) && l.isActive())
            .count();
    }

    // ---- Persistence ----

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (ShopListing l : listings.values()) {
            String p = l.getId().toString();
            cfg.set(p + ".seller",      l.getSellerId().toString());
            cfg.set(p + ".sellerName",  l.getSellerName());
            cfg.set(p + ".price",       l.getPricePerUnit());
            cfg.set(p + ".stock",       l.getStock());
            cfg.set(p + ".created",     l.getCreatedAt());
            cfg.set(p + ".type",        l.getType().name());
            cfg.set(p + ".kingdom",     l.getKingdomId() != null ? l.getKingdomId().toString() : null);
            cfg.set(p + ".item",        l.getTemplate());
        }
        try { cfg.save(file); } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder shops.yml : " + e.getMessage());
        }
    }

    public void load() {
        listings.clear();
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String key : cfg.getKeys(false)) {
            try {
                UUID id          = UUID.fromString(key);
                UUID seller      = UUID.fromString(cfg.getString(key + ".seller"));
                String sName     = cfg.getString(key + ".sellerName", "?");
                long price       = cfg.getLong(key + ".price");
                int stock        = cfg.getInt(key + ".stock");
                ShopListing.ListingType type =
                    ShopListing.ListingType.valueOf(cfg.getString(key + ".type", "PERSONAL"));
                String kStr      = cfg.getString(key + ".kingdom");
                UUID kingdomId   = kStr != null ? UUID.fromString(kStr) : null;
                ItemStack item   = cfg.getItemStack(key + ".item");
                if (item == null || stock <= 0) continue;

                ShopListing l = new ShopListing(id, seller, sName, item, stock, price, type, kingdomId);
                listings.put(id, l);
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur chargement annonce " + key + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("Chargé " + listings.size() + " annonces shop.");
    }

    // ---- Helpers ----

    private boolean isWeapon(ItemStack item) {
        String name = item.getType().name();
        return name.contains("SWORD") || name.contains("BOW") || name.contains("AXE")
            || name.contains("TRIDENT") || name.contains("CROSSBOW");
    }
}
