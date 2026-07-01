package fr.thewardead.kingdoms.model;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class KingdomPlayer {

    private final UUID uuid;
    private String name;
    private UUID kingdomId;
    private long balance;
    private boolean firstJoin;
    private final Set<UUID> pendingInvites = new LinkedHashSet<>();

    public KingdomPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.kingdomId = null;
        this.balance = 0;
        this.firstJoin = true;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getKingdomId() { return kingdomId; }
    public void setKingdomId(UUID kingdomId) { this.kingdomId = kingdomId; }
    public boolean isInKingdom() { return kingdomId != null; }

    public long getBalance() { return balance; }
    public void deposit(long amount) { balance += amount; }
    public boolean withdraw(long amount) {
        if (balance < amount) return false;
        balance -= amount;
        return true;
    }

    public boolean isFirstJoin() { return firstJoin; }
    public void setFirstJoin(boolean firstJoin) { this.firstJoin = firstJoin; }

    public Set<UUID> getPendingInvites() { return pendingInvites; }
    public void addInvite(UUID kingdomId) { pendingInvites.add(kingdomId); }
    public void removeInvite(UUID kingdomId) { pendingInvites.remove(kingdomId); }
}
