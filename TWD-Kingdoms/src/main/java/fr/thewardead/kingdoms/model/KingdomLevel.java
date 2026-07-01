package fr.thewardead.kingdoms.model;

public enum KingdomLevel {

    //              displayName    minMembers  minBank  upgradeCost  maxChunks
    HAMEAU ("Hameau",        0,          0L,       0L,         10),
    VILLAGE("Village",       3,        200L,     500L,         20),
    CITE   ("Cité",          6,       1000L,    2000L,         30),
    ROYAUME("Royaume",      12,       3000L,    5000L,         40),
    EMPIRE ("Empire",       20,      10000L,   15000L,         50);

    private final String displayName;
    private final int    minMembers;
    private final long   minBank;
    private final long   upgradeCost;
    private final int    maxChunks;

    KingdomLevel(String displayName, int minMembers, long minBank, long upgradeCost, int maxChunks) {
        this.displayName  = displayName;
        this.minMembers   = minMembers;
        this.minBank      = minBank;
        this.upgradeCost  = upgradeCost;
        this.maxChunks    = maxChunks;
    }

    public String getDisplayName() { return displayName; }
    public int    getMinMembers()  { return minMembers; }
    public long   getMinBank()     { return minBank; }
    public long   getUpgradeCost() { return upgradeCost; }
    public int    getMaxChunks()   { return maxChunks; }

    /** Niveau suivant, null si EMPIRE. */
    public KingdomLevel next() {
        KingdomLevel[] vals = values();
        return ordinal() < vals.length - 1 ? vals[ordinal() + 1] : null;
    }
}
