package fr.thewardead.kingdoms.model;

import org.bukkit.Location;

import java.util.*;

public class Kingdom {

    private final UUID id;
    private String name;
    private UUID chiefUUID;
    private final Map<UUID, KingdomRole> members = new LinkedHashMap<>();
    private long bank;
    private Location home;
    private int entryFee;
    private int taxRate;
    private KingdomLevel level = KingdomLevel.HAMEAU;
    private final Set<ChunkCoord> territory = new HashSet<>();

    public Kingdom(UUID id, String name, UUID chiefUUID) {
        this.id = id;
        this.name = name;
        this.chiefUUID = chiefUUID;
        this.bank = 0;
        this.entryFee = 0;
        this.taxRate = 0;
        members.put(chiefUUID, KingdomRole.CHEF);
    }

    // ---- Members ----

    public void addMember(UUID uuid) {
        members.put(uuid, KingdomRole.MEMBRE);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public boolean hasMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public KingdomRole getRole(UUID uuid) {
        return members.getOrDefault(uuid, null);
    }

    public void setRole(UUID uuid, KingdomRole role) {
        if (members.containsKey(uuid)) members.put(uuid, role);
    }

    public Set<UUID> getMemberUUIDs() {
        return Collections.unmodifiableSet(members.keySet());
    }

    public Map<UUID, KingdomRole> getMembers() {
        return Collections.unmodifiableMap(members);
    }

    public int getMemberCount() {
        return members.size();
    }

    // ---- Level ----

    public KingdomLevel getLevel() { return level; }
    public void setLevel(KingdomLevel level) { this.level = level; }

    // ---- Territory ----

    public Set<ChunkCoord> getTerritory() { return Collections.unmodifiableSet(territory); }
    public boolean hasChunk(ChunkCoord coord) { return territory.contains(coord); }
    public void addChunk(ChunkCoord coord) { territory.add(coord); }
    public int getClaimedChunks() { return territory.size(); }
    public int getMaxChunks() { return level.getMaxChunks(); }

    // ---- Bank ----

    public long getBank() { return bank; }
    public void depositBank(long amount) { bank += amount; }
    public boolean withdrawBank(long amount) {
        if (bank < amount) return false;
        bank -= amount;
        return true;
    }

    // ---- Getters / Setters ----

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getChiefUUID() { return chiefUUID; }
    public void setChiefUUID(UUID uuid) {
        members.put(uuid, KingdomRole.CHEF);
        chiefUUID = uuid;
    }
    public Location getHome() { return home; }
    public void setHome(Location home) { this.home = home; }
    public int getEntryFee() { return entryFee; }
    public void setEntryFee(int entryFee) { this.entryFee = Math.max(0, entryFee); }
    public int getTaxRate() { return taxRate; }
    public void setTaxRate(int taxRate) { this.taxRate = Math.max(0, Math.min(100, taxRate)); }
}
