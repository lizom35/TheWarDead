package fr.thewardead.kingdoms.manager;

import fr.thewardead.kingdoms.KingdomPlugin;
import fr.thewardead.kingdoms.model.ChunkCoord;
import fr.thewardead.kingdoms.model.Kingdom;
import fr.thewardead.kingdoms.model.KingdomLevel;
import fr.thewardead.kingdoms.model.KingdomPlayer;
import fr.thewardead.kingdoms.model.KingdomRole;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

public class KingdomManager {

    private final KingdomPlugin plugin;
    private final DataManager dataManager;
    private final TerritoryManager territoryManager;

    private final Map<UUID, Kingdom>       kingdoms = new LinkedHashMap<>();
    private final Map<UUID, KingdomPlayer> players  = new LinkedHashMap<>();

    public KingdomManager(KingdomPlugin plugin) {
        this.plugin           = plugin;
        this.dataManager      = new DataManager(plugin);
        this.territoryManager = new TerritoryManager(plugin);
    }

    // ---- Init ----

    public void load() {
        kingdoms.clear();
        players.clear();
        kingdoms.putAll(dataManager.loadKingdoms());
        players.putAll(dataManager.loadPlayers());
        plugin.getLogger().info("Chargé " + kingdoms.size() + " royaumes et " + players.size() + " joueurs.");
    }

    public void save() {
        dataManager.saveKingdoms(kingdoms);
        dataManager.savePlayers(players);
    }

    // ---- Player management ----

    public KingdomPlayer getOrCreatePlayer(Player p) {
        return players.computeIfAbsent(p.getUniqueId(),
            id -> new KingdomPlayer(id, p.getName()));
    }

    public KingdomPlayer getPlayer(UUID uuid) {
        return players.get(uuid);
    }

    // ---- Kingdom creation / deletion ----

    public enum CreateResult { OK, NAME_TAKEN, MAX_REACHED, ALREADY_IN_KINGDOM, TERRITORY_NOT_FOUND }

    public CreateResult createKingdom(Player chief, String name) {
        KingdomPlayer kp = getOrCreatePlayer(chief);
        if (kp.isInKingdom()) return CreateResult.ALREADY_IN_KINGDOM;
        if (getKingdomByName(name) != null) return CreateResult.NAME_TAKEN;

        int maxKingdoms = plugin.getConfig().getInt("max-kingdoms", 3);
        int threshold   = plugin.getConfig().getInt("kingdom-full-threshold", 10);
        if (kingdoms.size() >= maxKingdoms) {
            double avg = kingdoms.values().stream()
                .mapToInt(Kingdom::getMemberCount).average().orElse(0);
            if (avg < threshold) return CreateResult.MAX_REACHED;
        }

        // Recherche d'un territoire automatique
        World world = Bukkit.getWorlds().get(0);
        Location spawn = world.getSpawnLocation();
        List<ChunkCoord> territory = territoryManager.findInitialTerritory(world, spawn, kingdoms.values());
        if (territory.isEmpty()) return CreateResult.TERRITORY_NOT_FOUND;

        UUID id = UUID.randomUUID();
        Kingdom k = new Kingdom(id, name, chief.getUniqueId());

        for (ChunkCoord coord : territory) k.addChunk(coord);

        // Home = centre du territoire (SHAPE index 4 = {0,0})
        ChunkCoord center = territory.get(TerritoryManager.CENTER_INDEX);
        Location home = territoryManager.getHomeForCenter(world, center.x, center.z);
        k.setHome(home);

        kingdoms.put(id, k);
        kp.setKingdomId(id);
        save();
        return CreateResult.OK;
    }

    public boolean disbandKingdom(UUID kingdomId) {
        Kingdom k = kingdoms.remove(kingdomId);
        if (k == null) return false;
        for (UUID memberId : k.getMemberUUIDs()) {
            KingdomPlayer kp = players.get(memberId);
            if (kp != null) kp.setKingdomId(null);
        }
        save();
        return true;
    }

    // ---- Join / Leave ----

    public enum JoinResult { OK, ALREADY_IN_KINGDOM, KINGDOM_NOT_FOUND, NO_INVITE }

    public JoinResult joinKingdom(Player player, String kingdomName) {
        KingdomPlayer kp = getOrCreatePlayer(player);
        if (kp.isInKingdom()) return JoinResult.ALREADY_IN_KINGDOM;

        Kingdom k = getKingdomByName(kingdomName);
        if (k == null) return JoinResult.KINGDOM_NOT_FOUND;

        if (!kp.getPendingInvites().contains(k.getId())) return JoinResult.NO_INVITE;

        k.addMember(player.getUniqueId());
        kp.setKingdomId(k.getId());
        kp.removeInvite(k.getId());
        save();
        return JoinResult.OK;
    }

    public boolean leaveKingdom(Player player) {
        KingdomPlayer kp = getOrCreatePlayer(player);
        if (!kp.isInKingdom()) return false;

        Kingdom k = kingdoms.get(kp.getKingdomId());
        if (k == null) { kp.setKingdomId(null); save(); return true; }

        if (k.getChiefUUID().equals(player.getUniqueId())) {
            disbandKingdom(k.getId());
            Bukkit.broadcastMessage(color(msg("prefix")
                + "&eRoyaume &c" + k.getName() + " &edissous car le chef est parti."));
        } else {
            k.removeMember(player.getUniqueId());
            kp.setKingdomId(null);
            save();
        }
        return true;
    }

    // ---- Invitations ----

    public boolean invitePlayer(Kingdom kingdom, Player target) {
        KingdomPlayer kp = getOrCreatePlayer(target);
        if (kp.isInKingdom()) return false;
        kp.addInvite(kingdom.getId());
        save();
        return true;
    }

    // ---- Kick ----

    public boolean kickPlayer(Kingdom kingdom, UUID targetUUID) {
        if (!kingdom.hasMember(targetUUID)) return false;
        if (kingdom.getChiefUUID().equals(targetUUID)) return false;
        kingdom.removeMember(targetUUID);
        KingdomPlayer kp = players.get(targetUUID);
        if (kp != null) kp.setKingdomId(null);
        save();
        return true;
    }

    // ---- Roles ----

    public boolean setRole(Kingdom kingdom, UUID targetUUID, KingdomRole role) {
        if (!kingdom.hasMember(targetUUID)) return false;
        if (role == KingdomRole.CHEF) return false;
        kingdom.setRole(targetUUID, role);
        save();
        return true;
    }

    // ---- Territoire ----

    /** Retourne le royaume propriétaire d'un chunk, ou null. */
    public Kingdom getKingdomAt(ChunkCoord coord) {
        return territoryManager.getKingdomAt(coord, kingdoms.values());
    }

    /**
     * Évolution du royaume au niveau suivant.
     * Retourne un message d'erreur lisible, ou null si succès.
     */
    public enum UpgradeResult { OK, MAX_LEVEL, NOT_ENOUGH_MEMBERS, NOT_ENOUGH_BANK, CANNOT_AFFORD }

    public UpgradeResult upgradeKingdom(Kingdom k) {
        KingdomLevel next = k.getLevel().next();
        if (next == null) return UpgradeResult.MAX_LEVEL;
        if (k.getMemberCount() < next.getMinMembers()) return UpgradeResult.NOT_ENOUGH_MEMBERS;
        if (k.getBank() < next.getMinBank()) return UpgradeResult.NOT_ENOUGH_BANK;
        if (!k.withdrawBank(next.getUpgradeCost())) return UpgradeResult.CANNOT_AFFORD;

        k.setLevel(next);
        save();
        return UpgradeResult.OK;
    }

    /**
     * Claim le chunk donné pour le royaume.
     * Retourne un message d'erreur lisible, ou null si succès.
     */
    public enum ClaimResult { OK, TERRITORY_FULL, ALREADY_CLAIMED }

    public ClaimResult claimChunk(Kingdom k, ChunkCoord coord) {
        if (k.getClaimedChunks() >= k.getMaxChunks()) return ClaimResult.TERRITORY_FULL;
        Kingdom owner = getKingdomAt(coord);
        if (owner != null) return ClaimResult.ALREADY_CLAIMED;
        k.addChunk(coord);
        save();
        return ClaimResult.OK;
    }

    // ---- Income passif ----

    public void distributeIncome() {
        for (Kingdom k : kingdoms.values()) {
            int income = plugin.getConfig().getInt("income." + k.getLevel().name(), 50);
            k.depositBank(income);

            if (k.getTaxRate() > 0) {
                for (UUID memberId : k.getMemberUUIDs()) {
                    KingdomPlayer kp = players.get(memberId);
                    if (kp == null) continue;
                    long tax = kp.getBalance() * k.getTaxRate() / 100;
                    if (kp.withdraw(tax)) k.depositBank(tax);
                }
            }

            for (UUID memberId : k.getMemberUUIDs()) {
                Player online = Bukkit.getPlayer(memberId);
                if (online != null) {
                    online.sendMessage(color(msg("prefix")
                        + "&eTrésor de &b" + k.getName()
                        + " &e+&a" + income + "₩ &e(revenu passif). Total : &b" + k.getBank() + "₩"));
                }
            }
        }
        save();
    }

    // ---- Lookup helpers ----

    public Kingdom getKingdomByName(String name) {
        return kingdoms.values().stream()
            .filter(k -> k.getName().equalsIgnoreCase(name))
            .findFirst().orElse(null);
    }

    public Kingdom getKingdom(UUID id) { return kingdoms.get(id); }

    public Kingdom getPlayerKingdom(UUID playerUUID) {
        KingdomPlayer kp = players.get(playerUUID);
        if (kp == null || !kp.isInKingdom()) return null;
        Kingdom k = kingdoms.get(kp.getKingdomId());
        // Stale ref: kingdom was deleted or player was removed from it — auto-clean
        if (k == null || !k.hasMember(playerUUID)) {
            kp.setKingdomId(null);
            return null;
        }
        return k;
    }

    public Collection<Kingdom> getAllKingdoms() {
        return Collections.unmodifiableCollection(kingdoms.values());
    }

    public Collection<KingdomPlayer> getAllPlayers() {
        return Collections.unmodifiableCollection(players.values());
    }

    // ---- Util ----

    private String msg(String key) {
        return plugin.getConfig().getString("messages." + key, "");
    }

    private String color(String s) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }
}
