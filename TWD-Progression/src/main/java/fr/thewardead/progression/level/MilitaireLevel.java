package fr.thewardead.progression.level;

/**
 * Paliers du métier Militaire.
 *
 *  damageBonus     : % bonus dégâts infligés (0.10 = +10%)
 *  damageReduction : % réduction dégâts reçus (0.10 = -10%)
 *  warsPerKill     : ₩ par kill (hauts niveaux)
 */
public enum MilitaireLevel {

    //              num   nom                  xpTotal   dmgBonus  dmgReduc  wars/kill
    RECRUE      (1,  "Recrue",            0,      0.00,  0.00,  0),
    SOLDAT      (2,  "Soldat",            100,    0.05,  0.00,  0),
    CAPORAL     (3,  "Caporal",           350,    0.10,  0.05,  0),
    SERGENT     (4,  "Sergent",           850,    0.15,  0.10,  0),
    LIEUTENANT  (5,  "Lieutenant",        1850,   0.20,  0.12,  0),
    CAPITAINE   (6,  "Capitaine",         3850,   0.25,  0.15,  5),
    CHEVALIER   (7,  "Chevalier",         7350,   0.30,  0.18,  5),
    CHAMPION    (8,  "Champion",          12850,  0.37,  0.22,  10),
    HEROS       (9,  "Héros",             21350,  0.45,  0.28,  10),
    DIEU_GUERRE (10, "Dieu de la Guerre", 34350,  0.55,  0.35,  15);

    private final int    num;
    private final String displayName;
    private final long   xpRequired;
    private final double damageBonus;
    private final double damageReduction;
    private final int    warsPerKill;

    MilitaireLevel(int num, String displayName, long xpRequired,
                   double damageBonus, double damageReduction, int warsPerKill) {
        this.num             = num;
        this.displayName     = displayName;
        this.xpRequired      = xpRequired;
        this.damageBonus     = damageBonus;
        this.damageReduction = damageReduction;
        this.warsPerKill     = warsPerKill;
    }

    public static MilitaireLevel forXp(long xp) {
        MilitaireLevel result = RECRUE;
        for (MilitaireLevel lv : values()) {
            if (xp >= lv.xpRequired) result = lv;
            else break;
        }
        return result;
    }

    public static MilitaireLevel forNum(int num) {
        for (MilitaireLevel lv : values()) {
            if (lv.num == num) return lv;
        }
        return RECRUE;
    }

    public MilitaireLevel next() { return num < 10 ? forNum(num + 1) : null; }

    public long xpToNext(long currentXp) {
        MilitaireLevel next = next();
        return next == null ? -1 : next.xpRequired - currentXp;
    }

    public String bonusSummary() {
        if (num == 1) return "Aucun bonus — bienvenue dans le métier !";
        StringBuilder sb = new StringBuilder();
        if (damageBonus     > 0) sb.append("+").append((int)(damageBonus*100)).append("% dégâts  ");
        if (damageReduction > 0) sb.append("-").append((int)(damageReduction*100)).append("% dégâts reçus  ");
        if (warsPerKill     > 0) sb.append("+").append(warsPerKill).append("₩/kill");
        return sb.toString().trim();
    }

    public int    getNum()             { return num; }
    public String getDisplayName()     { return displayName; }
    public long   getXpRequired()      { return xpRequired; }
    public double getDamageBonus()     { return damageBonus; }
    public double getDamageReduction() { return damageReduction; }
    public int    getWarsPerKill()     { return warsPerKill; }
}
