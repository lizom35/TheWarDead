package fr.thewardead.progression.gui;

import fr.thewardead.progression.model.JobClass;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class ClassChoiceGUI {

    public static final String TITLE   = "§8⚔ Choisissez votre métier";
    public static final int    SLOT_FERMIER    = 11;
    public static final int    SLOT_MILITAIRE  = 15;

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // Remplissage avec des vitres grises
        ItemStack glass = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        // FERMIER
        inv.setItem(SLOT_FERMIER, makeItem(
            Material.WHEAT,
            "§a§lFermier",
            Arrays.asList(
                "§7Agriculture, élevage, pêche.",
                "",
                "§8Paliers : Novice → Légende Agraire",
                "",
                "§a✦ Sources d'XP :",
                "§7  • Récolte de cultures matures",
                "§7  • Élevage et pêche",
                "§7  • Abattage d'animaux",
                "",
                "§a✦ Bonus max (niv. 10) :",
                "§7  • +60% drops de récolte",
                "§7  • 40% chance double moisson",
                "§7  • Double drops de pêche",
                "§7  • +3₩ par récolte",
                "",
                "§e▶ Cliquez pour choisir"
            )
        ));

        // MILITAIRE
        inv.setItem(SLOT_MILITAIRE, makeItem(
            Material.IRON_SWORD,
            "§c§lMilitaire",
            Arrays.asList(
                "§7Combat, mobs hostiles, PvP.",
                "",
                "§8Paliers : Recrue → Dieu de la Guerre",
                "",
                "§c✦ Sources d'XP :",
                "§7  • Tuer des mobs hostiles",
                "§7  • Kills PvP (joueurs)",
                "§7  • Boss (Wither, Dragon…)",
                "",
                "§c✦ Bonus max (niv. 10) :",
                "§7  • +55% dégâts infligés",
                "§7  • -35% dégâts reçus",
                "§7  • +15₩ par kill",
                "",
                "§e▶ Cliquez pour choisir"
            )
        ));

        player.openInventory(inv);
    }

    private static ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
