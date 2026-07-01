package fr.thewardead.progression.level;

/**
 * Paliers du métier Fermier.
 *
 *  extraDropChance     : probabilité d'obtenir un drop supplémentaire par item récolté
 *  doubleHarvestChance : probabilité de doubler TOUTE la récolte
 *  fishingMultiplier   : 0 = rien, 0.5 = +50%, 1.0 = double les drops de pêche
 *  warsPerHarvest      : ₩ offerts par récolte mature (hauts niveaux)
 */
public enum FermierLevel {

    //              num   nom                 xpTotal  extraDrop  x2Harvest  fishMult   wars/récolte
    NOVICE      (1,  "Novice",           0,      0.00,  0.00,  0.00,  0),
    APPRENTI    (2,  "Apprenti",         100,    0.10,  0.00,  0.00,  0),
    CULTIVATEUR (3,  "Cultivateur",      350,    0.20,  0.00,  0.00,  0),
    HERBORISTE  (4,  "Herboriste",       850,    0.20,  0.00,  0.50,  0),
    ELEVEUR     (5,  "Éleveur",          1850,   0.25,  0.00,  0.50,  0),
    EXPERT      (6,  "Expert",           3850,   0.30,  0.15,  0.75,  0),
    ARTISAN     (7,  "Artisan",          7350,   0.35,  0.20,  0.75,  1),
    MAITRE      (8,  "Maître",           12850,  0.40,  0.25,  1.00,  1),
    GRAND_MAITRE(9,  "Grand Maître",     21350,  0.50,  0.30,  1.00,  2),
    LEGENDE     (10, "Légende Agraire",  34350,  0.60,  0.40,  1.00,  3);

    private final int    num;
    private final String displayName;
    private final long   xpRequired;
    private final double extraDropChance;
    private final double doubleHarvestChance;
    private final double fishingMultiplier;
    private final int    warsPerHarvest;

    FermierLevel(int num, String displayName, long xpRequired,
                 double extraDropChance, double doubleHarvestChance,
                 double fishingMultiplier, int warsPerHarvest) {
        this.num                 = num;
        this.displayName         = displayName;
        this.xpRequired          = xpRequired;
        this.extraDropChance     = extraDropChance;
        this.doubleHarvestChance = doubleHarvestChance;
        this.fishingMultiplier   = fishingMultiplier;
        this.warsPerHarvest      = warsPerHarvest;
    }

    /** Retourne le palier correspondant au XP cumulatif. */
    public static FermierLevel forXp(long xp) {
        FermierLevel result = NOVICE;
        for (FermierLevel lv : values()) {
            if (xp >= lv.xpRequired) result = lv;
            else break;
        }
        return result;
    }

    public static FermierLevel forNum(int num) {
        for (FermierLevel lv : values()) {
            if (lv.num == num) return lv;
        }
        return NOVICE;
    }

    public FermierLevel next() { return num < 10 ? forNum(num + 1) : null; }

    /** XP encore nécessaire pour passer au palier suivant (-1 si max). */
    public long xpToNext(long currentXp) {
        FermierLevel next = next();
        return next == null ? -1 : next.xpRequired - currentXp;
    }

    public String bonusSummary() {
        if (num == 1) return "Aucun bonus — bienvenue dans le métier !";
        StringBuilder sb = new StringBuilder();
        if (extraDropChance > 0)     sb.append((int)(extraDropChance*100)).append("% drop bonus  ");
        if (doubleHarvestChance > 0) sb.append((int)(doubleHarvestChance*100)).append("% double récolte  ");
        if (fishingMultiplier > 0)   sb.append("+").append((int)(fishingMultiplier*100)).append("% pêche  ");
        if (warsPerHarvest > 0)      sb.append("+").append(warsPerHarvest).append("₩/récolte");
        return sb.toString().trim();
    }

    public int    getNum()                  { return num; }
    public String getDisplayName()          { return displayName; }
    public long   getXpRequired()           { return xpRequired; }
    public double getExtraDropChance()      { return extraDropChance; }
    public double getDoubleHarvestChance()  { return doubleHarvestChance; }
    public double getFishingMultiplier()    { return fishingMultiplier; }
    public int    getWarsPerHarvest()       { return warsPerHarvest; }
}
