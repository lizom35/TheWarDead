package fr.thewardead.progression.model;

import org.bukkit.Material;

public enum JobClass {

    FERMIER(
        "Fermier",
        "§a",
        Material.WHEAT,
        "Maîtrise de l'agriculture, de l'élevage et de la pêche.",
        "Bonus aux récoltes, double moisson, gains passifs en ₩"
    ),
    MILITAIRE(
        "Militaire",
        "§c",
        Material.IRON_SWORD,
        "Maîtrise du combat, des armes et de la guerre.",
        "Bonus dégâts, réduction dégâts reçus, ₩ par kill"
    );

    private final String   displayName;
    private final String   color;
    private final Material icon;
    private final String   shortDesc;
    private final String   bonusDesc;

    JobClass(String displayName, String color, Material icon,
             String shortDesc, String bonusDesc) {
        this.displayName = displayName;
        this.color       = color;
        this.icon        = icon;
        this.shortDesc   = shortDesc;
        this.bonusDesc   = bonusDesc;
    }

    public String   getDisplayName() { return displayName; }
    public String   getColor()       { return color; }
    public Material getIcon()        { return icon; }
    public String   getShortDesc()   { return shortDesc; }
    public String   getBonusDesc()   { return bonusDesc; }
}
