package fr.thewardead.kingdoms.gui;

import fr.thewardead.kingdoms.KingdomPlugin;
import fr.thewardead.kingdoms.model.Kingdom;
import fr.thewardead.kingdoms.model.KingdomLevel;
import fr.thewardead.kingdoms.model.KingdomRole;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KingdomMenuGUI {

    public static final String TITLE = "§c§lGestion du Royaume";

    private final KingdomPlugin plugin;

    public KingdomMenuGUI(KingdomPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Layout 45 slots (5 rangées × 9) :
     *
     * [g][g][g][g][Info][g][g][g][g]         rangée 0
     * [Home][g][Banque][g][Territoire][g][Membres][g][g]  rangée 1
     * [Inviter][g][Expulser][g][Rang][g][g][g][g]         rangée 2
     * [SetHome][g][Évoluer][g][Taxe][g][Frais][g][g]      rangée 3
     * [Quitter][g][g][g][g][g][g][g][Retour]              rangée 4
     */
    public void open(Player player) {
        Kingdom k = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
        if (k == null) {
            player.sendMessage(c("&8[&cTWD&8] &cVous n'êtes pas dans un royaume."));
            return;
        }

        boolean isChief   = k.getChiefUUID().equals(player.getUniqueId());
        KingdomRole role  = k.getRole(player.getUniqueId());
        boolean isOfficer = role != null && role.isAtLeast(KingdomRole.OFFICIER);
        KingdomLevel next = k.getLevel().next();
        int income = plugin.getConfig().getInt("income." + k.getLevel().name(), 50);

        Inventory inv = Bukkit.createInventory(null, 45, TITLE);

        // ── Slot 4 : Infos royaume ──────────────────────────────────────────
        List<String> infoLore = new ArrayList<>(List.of(
            "§7Niveau : §e" + k.getLevel().getDisplayName(),
            "§7Membres : §e" + k.getMemberCount(),
            "§7Trésor : §e" + k.getBank() + " ₩",
            "§7Revenu/h : §a+" + income + " ₩",
            "§7Territoire : §e" + k.getClaimedChunks() + "§7/§e" + k.getMaxChunks() + " §7chunks"
        ));
        if (next != null) {
            infoLore.add("");
            infoLore.add("§8▶ Prochain : §e" + next.getDisplayName());
            infoLore.add("§8  Membres : §e" + next.getMinMembers()
                + (k.getMemberCount() >= next.getMinMembers() ? " §a✓" : " §c✗"));
            infoLore.add("§8  Banque : §e" + next.getMinBank() + "₩"
                + (k.getBank() >= next.getMinBank() ? " §a✓" : " §c✗"));
            infoLore.add("§8  Coût : §e" + next.getUpgradeCost() + " ₩");
        }
        inv.setItem(4, make(Material.BOOK, "§c§l" + k.getName(), infoLore));

        // ── Rangée 1 : Actions communes ────────────────────────────────────

        // Slot 9 : Home TP
        if (k.getHome() != null) {
            inv.setItem(9, make(Material.RED_BED, "§a§lAller au Royaume",
                List.of("§7Téléporte au point de retour.", "", "§aCliquez pour vous TP")));
        } else {
            inv.setItem(9, make(Material.BARRIER, "§c§lAucun Home défini",
                List.of("§cDemandez au Chef : /kingdom sethome")));
        }

        // Slot 11 : Banque
        inv.setItem(11, make(Material.GOLD_INGOT, "§e§lBanque du Royaume",
            List.of(
                "§7Solde : §6" + k.getBank() + " ₩",
                "",
                "§aClic gauche §7→ §aDéposer des ₩",
                "§eClic droit §7→ §eRetirer des ₩ §8(Chef)"
            )));

        // Slot 13 : Territoire
        int free = k.getMaxChunks() - k.getClaimedChunks();
        List<String> terrLore = new ArrayList<>(List.of(
            "§7Chunks revendiqués : §e" + k.getClaimedChunks() + "§7/§e" + k.getMaxChunks()
        ));
        if (free > 0) {
            terrLore.add("§a" + free + " chunk(s) libre(s)");
            terrLore.add("");
            terrLore.add("§aCliquez §7→ Revendiquer le chunk actuel");
            terrLore.add("§8(Officiers et Chef uniquement)");
        } else {
            terrLore.add("§cTerritoire complet — Évoluez le royaume.");
        }
        inv.setItem(13, make(Material.GRASS_BLOCK, "§2§lTerritoire", terrLore));

        // Slot 15 : Membres (info)
        List<String> memberLore = new ArrayList<>();
        for (var entry : k.getMembers().entrySet()) {
            String mName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            boolean online = Bukkit.getPlayer(entry.getKey()) != null;
            memberLore.add((online ? "§a" : "§8") + "● §7"
                + (mName != null ? mName : "?")
                + " §8— §e" + entry.getValue().getDisplayName());
        }
        inv.setItem(15, make(Material.PLAYER_HEAD,
            "§b§lMembres §8(" + k.getMemberCount() + ")", memberLore));

        // ── Rangée 2 : Officiers+ ───────────────────────────────────────────

        // Slot 18 : Inviter
        inv.setItem(18, isOfficer
            ? make(Material.PAPER, "§a§lInviter un Joueur",
                List.of("§7Envoyez une invitation.", "", "§aCliquez pour inviter"))
            : make(Material.PAPER, "§7Inviter un Joueur",
                List.of("§cRéservé aux Officiers et au Chef.")));

        // Slot 20 : Expulser
        inv.setItem(20, isOfficer
            ? make(Material.BARRIER, "§c§lExpulser un Membre",
                List.of("§7Retirez un membre du royaume.", "", "§cCliquez pour expulser"))
            : make(Material.BARRIER, "§7Expulser un Membre",
                List.of("§cRéservé aux Officiers et au Chef.")));

        // Slot 22 : Changer rang
        inv.setItem(22, isChief
            ? make(Material.EXPERIENCE_BOTTLE, "§d§lChanger le Rang",
                List.of("§7Modifiez le rang d'un membre.", "", "§dCliquez pour changer"))
            : make(Material.EXPERIENCE_BOTTLE, "§7Changer le Rang",
                List.of("§cRéservé au Chef.")));

        // ── Rangée 3 : Chef ─────────────────────────────────────────────────

        // Slot 27 : Set Home
        inv.setItem(27, isChief
            ? make(Material.RESPAWN_ANCHOR, "§6§lDéfinir le Home",
                List.of("§7Fixe le TP royaume à votre position.", "", "§6Cliquez pour définir"))
            : make(Material.RESPAWN_ANCHOR, "§7Définir le Home",
                List.of("§cRéservé au Chef.")));

        // Slot 29 : Évoluer
        if (next != null) {
            boolean canUpgrade = k.getMemberCount() >= next.getMinMembers()
                && k.getBank() >= next.getMinBank();
            inv.setItem(29, isChief
                ? make(canUpgrade ? Material.NETHER_STAR : Material.COAL,
                    canUpgrade ? "§6§l✦ Évoluer → " + next.getDisplayName() : "§7Évolution : " + next.getDisplayName(),
                    List.of(
                        "§7Membres : §e" + k.getMemberCount() + "§8/§e" + next.getMinMembers()
                            + (k.getMemberCount() >= next.getMinMembers() ? " §a✓" : " §c✗"),
                        "§7Banque : §e" + k.getBank() + "§8/§e" + next.getMinBank() + "₩"
                            + (k.getBank() >= next.getMinBank() ? " §a✓" : " §c✗"),
                        "§7Coût : §e" + next.getUpgradeCost() + " ₩",
                        "",
                        canUpgrade ? "§6Cliquez pour évoluer !" : "§cCritères non remplis."
                    ))
                : make(Material.COAL, "§7Évolution",
                    List.of("§cRéservé au Chef.")));
        } else {
            inv.setItem(29, make(Material.NETHER_STAR, "§6§l✦ Niveau Maximum ✦",
                List.of("§7Votre royaume est un §lEmpire §7!", "§6Félicitations !")));
        }

        // Slot 31 : Taxe
        inv.setItem(31, isChief
            ? make(Material.COMPARATOR, "§c§lTaux de Taxe",
                List.of("§7Actuel : §e" + k.getTaxRate() + "%",
                    "§7Prélevée sur les membres à chaque income.", "", "§cCliquez pour modifier"))
            : make(Material.COMPARATOR, "§7Taux de Taxe",
                List.of("§7Actuel : §e" + k.getTaxRate() + "%", "§cRéservé au Chef.")));

        // Slot 33 : Frais d'entrée
        inv.setItem(33, isChief
            ? make(Material.IRON_INGOT, "§b§lFrais d'Entrée",
                List.of("§7Actuels : §e" + k.getEntryFee() + " ₩",
                    "§7Payés pour entrer sur le territoire.", "", "§bCliquez pour modifier"))
            : make(Material.IRON_INGOT, "§7Frais d'Entrée",
                List.of("§7Actuels : §e" + k.getEntryFee() + " ₩", "§cRéservé au Chef.")));

        // ── Rangée 4 : Bas ──────────────────────────────────────────────────

        // Slot 36 : Quitter
        inv.setItem(36, make(Material.OAK_DOOR,
            isChief ? "§4§lDissoudre le Royaume" : "§c§lQuitter le Royaume",
            isChief
                ? List.of("§cAttention : vous êtes Chef !", "§cCela dissoudra le royaume.", "", "§4Shift+Clic pour confirmer")
                : List.of("§7Quittez définitivement le royaume.", "", "§cCliquez pour quitter")));

        // Slot 44 : Retour
        inv.setItem(44, make(Material.ARROW, "§7← Retour",
            List.of("§7Retour au menu joueur.")));

        // Décoration
        ItemStack filler = FirstConnectGUI.makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 45; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }

        player.openInventory(inv);
    }

    private static ItemStack make(Material mat, String name, List<String> lore) {
        return FirstConnectGUI.makeItem(mat, name, lore);
    }

    private String c(String s) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }
}
