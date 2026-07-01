package fr.thewardead.kingdoms.listener;

import fr.thewardead.kingdoms.KingdomPlugin;
import fr.thewardead.kingdoms.gui.FirstConnectGUI;
import fr.thewardead.kingdoms.gui.KingdomMenuGUI;
import fr.thewardead.kingdoms.gui.PlayerMenuGUI;
import fr.thewardead.kingdoms.gui.ReconnectGUI;
import fr.thewardead.kingdoms.manager.KingdomManager;
import fr.thewardead.kingdoms.model.ChunkCoord;
import fr.thewardead.kingdoms.model.Kingdom;
import fr.thewardead.kingdoms.model.KingdomLevel;
import fr.thewardead.kingdoms.model.KingdomPlayer;
import fr.thewardead.kingdoms.model.KingdomRole;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.UUID;

public class GUIListener implements Listener {

    private final KingdomPlugin plugin;

    public GUIListener(KingdomPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        // Only intercept our own GUIs — never block normal inventory interactions
        if (!title.equals(FirstConnectGUI.TITLE) && !title.equals(ReconnectGUI.TITLE)
                && !title.equals(PlayerMenuGUI.TITLE) && !title.equals(KingdomMenuGUI.TITLE)) {
            return;
        }

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        if      (title.equals(FirstConnectGUI.TITLE)) handleFirstConnect(event, player);
        else if (title.equals(ReconnectGUI.TITLE))    handleReconnect(event, player);
        else if (title.equals(PlayerMenuGUI.TITLE))   handlePlayerMenu(event, player);
        else if (title.equals(KingdomMenuGUI.TITLE))  handleKingdomMenu(event, player);
    }

    // ── Premier connexion ─────────────────────────────────────────────────

    private void handleFirstConnect(InventoryClickEvent event, Player player) {
        switch (event.getSlot()) {
            case 11 -> requestCreate(player);                     // Créer un royaume
            case 13 -> requestJoin(player);                       // Rejoindre
            case 15 -> { player.closeInventory(); goSpawn(player); } // Map
        }
    }

    // ── Reconnexion ───────────────────────────────────────────────────────

    private void handleReconnect(InventoryClickEvent event, Player player) {
        switch (event.getSlot()) {
            case 11 -> {
                player.closeInventory();
                Kingdom k = km().getPlayerKingdom(player.getUniqueId());
                if (k != null && k.getHome() != null) {
                    player.teleport(k.getHome());
                    player.sendMessage(c(pre() + "&aTéléportation vers &e" + k.getName() + " &a!"));
                } else {
                    goSpawn(player);
                }
            }
            case 15 -> { player.closeInventory(); goSpawn(player); }
        }
    }

    // ── Menu joueur ───────────────────────────────────────────────────────

    private void handlePlayerMenu(InventoryClickEvent event, Player player) {
        Kingdom k = km().getPlayerKingdom(player.getUniqueId());
        switch (event.getSlot()) {
            case 4 -> {} // profil — info seulement
            case 11 -> {
                if (k == null) requestCreate(player);                             // Créer
                else { player.closeInventory(); plugin.getKingdomMenuGUI().open(player); } // Gérer
            }
            case 15 -> {
                if (k == null) requestJoin(player);                               // Rejoindre
                else { player.closeInventory(); goSpawn(player); }                // Spawn
            }
        }
    }

    // ── Menu royaume ──────────────────────────────────────────────────────

    private void handleKingdomMenu(InventoryClickEvent event, Player player) {
        Kingdom k = km().getPlayerKingdom(player.getUniqueId());
        if (k == null) { player.closeInventory(); return; }

        boolean isChief   = k.getChiefUUID().equals(player.getUniqueId());
        KingdomRole role  = k.getRole(player.getUniqueId());
        boolean isOfficer = role != null && role.isAtLeast(KingdomRole.OFFICIER);

        switch (event.getSlot()) {

            // ── Slot 9 : TP Home ──────────────────────────────────────────
            case 9 -> {
                player.closeInventory();
                if (k.getHome() != null) {
                    player.teleport(k.getHome());
                    player.sendMessage(c(pre() + "&aTéléportation vers &e" + k.getName() + " &a!"));
                } else {
                    player.sendMessage(c(pre() + "&cAucun point de retour défini."));
                }
            }

            // ── Slot 11 : Banque ──────────────────────────────────────────
            case 11 -> {
                if (event.getClick() == ClickType.LEFT) {
                    chat().request(player, "Combien de ₩ voulez-vous déposer dans la banque ?", input -> {
                        try {
                            long amount = Long.parseLong(input);
                            if (amount <= 0) { player.sendMessage(c(pre() + "&cMontant invalide.")); return; }
                            KingdomPlayer kp = km().getOrCreatePlayer(player);
                            if (!kp.withdraw(amount)) { player.sendMessage(c(pre() + "&cSolde insuffisant.")); return; }
                            k.depositBank(amount);
                            km().save();
                            player.sendMessage(c(pre() + "&aDépôt de §e" + amount + "₩ §aeffectué. Banque : §e" + k.getBank() + "₩"));
                        } catch (NumberFormatException e) { player.sendMessage(c(pre() + "&cMontant invalide.")); }
                    });
                } else if (event.getClick() == ClickType.RIGHT) {
                    if (!isChief) { player.sendMessage(c(pre() + "&cSeul le Chef peut retirer de la banque.")); return; }
                    chat().request(player, "Combien de ₩ voulez-vous retirer de la banque ?", input -> {
                        try {
                            long amount = Long.parseLong(input);
                            if (amount <= 0) { player.sendMessage(c(pre() + "&cMontant invalide.")); return; }
                            if (!k.withdrawBank(amount)) { player.sendMessage(c(pre() + "&cFonds insuffisants.")); return; }
                            km().getOrCreatePlayer(player).deposit(amount);
                            km().save();
                            player.sendMessage(c(pre() + "&aRetrait de §e" + amount + "₩ §aeffectué. Banque : §e" + k.getBank() + "₩"));
                        } catch (NumberFormatException e) { player.sendMessage(c(pre() + "&cMontant invalide.")); }
                    });
                }
            }

            // ── Slot 13 : Territoire / Claim ──────────────────────────────
            case 13 -> {
                if (!isOfficer) { player.sendMessage(c(pre() + "&cOfficiers et Chef uniquement.")); return; }
                player.closeInventory();
                org.bukkit.Chunk chunk = player.getLocation().getChunk();
                ChunkCoord coord = new ChunkCoord(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
                switch (km().claimChunk(k, coord)) {
                    case OK ->
                        player.sendMessage(c(pre() + "&aChunk revendiqué ! Territoire : §e"
                            + k.getClaimedChunks() + "§a/§e" + k.getMaxChunks()));
                    case TERRITORY_FULL ->
                        player.sendMessage(c(pre() + "&cTerritoire plein (§e" + k.getMaxChunks()
                            + " §cchunks). Évoluez avec /kingdom upgrade."));
                    case ALREADY_CLAIMED -> {
                        Kingdom owner = km().getKingdomAt(coord);
                        player.sendMessage(c(pre() + "&cDéjà revendiqué par §e"
                            + (owner != null ? owner.getName() : "un autre royaume") + "&c."));
                    }
                }
            }

            // ── Slot 15 : Membres (info seulement) ───────────────────────
            case 15 -> {} // affichage, pas d'action

            // ── Slot 18 : Inviter ─────────────────────────────────────────
            case 18 -> {
                if (!isOfficer) { player.sendMessage(c(pre() + "&cOfficiers et Chef uniquement.")); return; }
                chat().request(player, "Quel joueur voulez-vous inviter ?", input -> {
                    Player target = Bukkit.getPlayer(input);
                    if (target == null) { player.sendMessage(c(pre() + "&cJoueur introuvable : §e" + input)); return; }
                    if (!km().invitePlayer(k, target)) {
                        player.sendMessage(c(pre() + "&cCe joueur est déjà dans un royaume.")); return;
                    }
                    player.sendMessage(c(pre() + "&aInvitation envoyée à §e" + target.getName() + " §a!"));
                    target.sendMessage(c(pre() + "&e" + player.getName() + " §avous invite à rejoindre §e" + k.getName()
                        + " §a! Tapez §b/kingdom accept " + k.getName() + " §apour accepter."));
                });
            }

            // ── Slot 20 : Expulser ────────────────────────────────────────
            case 20 -> {
                if (!isOfficer) { player.sendMessage(c(pre() + "&cOfficiers et Chef uniquement.")); return; }
                chat().request(player, "Quel joueur voulez-vous expulser ?", input -> {
                    Player target = Bukkit.getPlayer(input);
                    UUID targetUUID = target != null ? target.getUniqueId()
                        : Bukkit.getOfflinePlayer(input).getUniqueId();
                    if (!km().kickPlayer(k, targetUUID)) {
                        player.sendMessage(c(pre() + "&cImpossible d'expulser ce joueur.")); return;
                    }
                    player.sendMessage(c(pre() + "&aJoueur expulsé."));
                    if (target != null)
                        target.sendMessage(c(pre() + "&cVous avez été expulsé du royaume §e" + k.getName() + "&c."));
                });
            }

            // ── Slot 22 : Changer rang ────────────────────────────────────
            case 22 -> {
                if (!isChief) { player.sendMessage(c(pre() + "&cRéservé au Chef.")); return; }
                chat().request(player, "Tapez : <nom du joueur> MEMBRE|OFFICIER", input -> {
                    String[] parts = input.split("\\s+", 2);
                    if (parts.length < 2) { player.sendMessage(c(pre() + "&cFormat attendu : <nom> MEMBRE|OFFICIER")); return; }
                    KingdomRole newRole;
                    try { newRole = KingdomRole.valueOf(parts[1].toUpperCase()); }
                    catch (Exception e) { player.sendMessage(c(pre() + "&cRôle invalide. Utilisez MEMBRE ou OFFICIER.")); return; }
                    Player target = Bukkit.getPlayer(parts[0]);
                    UUID targetUUID = target != null ? target.getUniqueId()
                        : Bukkit.getOfflinePlayer(parts[0]).getUniqueId();
                    if (!km().setRole(k, targetUUID, newRole)) {
                        player.sendMessage(c(pre() + "&cImpossible de modifier le rôle.")); return;
                    }
                    player.sendMessage(c(pre() + "&aRôle de §e" + parts[0] + " §amis à jour → §e" + newRole.getDisplayName()));
                    if (target != null)
                        target.sendMessage(c(pre() + "&eVotre rôle dans §b" + k.getName()
                            + " §eest maintenant : §a" + newRole.getDisplayName()));
                });
            }

            // ── Slot 27 : Définir Home ────────────────────────────────────
            case 27 -> {
                if (!isChief) { player.sendMessage(c(pre() + "&cRéservé au Chef.")); return; }
                player.closeInventory();
                k.setHome(player.getLocation());
                km().save();
                player.sendMessage(c(pre() + "&aPoint de retour du royaume défini à votre position."));
            }

            // ── Slot 29 : Évoluer ─────────────────────────────────────────
            case 29 -> {
                if (!isChief) { player.sendMessage(c(pre() + "&cRéservé au Chef.")); return; }
                player.closeInventory();
                KingdomLevel next = k.getLevel().next();
                if (next == null) { player.sendMessage(c(pre() + "&eNiveau maximum atteint !")); return; }
                switch (km().upgradeKingdom(k)) {
                    case MAX_LEVEL ->
                        player.sendMessage(c(pre() + "&eNiveau maximum."));
                    case NOT_ENOUGH_MEMBERS ->
                        player.sendMessage(c(pre() + "&cCritère non rempli : §e" + next.getMinMembers()
                            + " §cmembres requis (actuel : §e" + k.getMemberCount() + "§c)."));
                    case NOT_ENOUGH_BANK ->
                        player.sendMessage(c(pre() + "&cCritère non rempli : §e" + next.getMinBank()
                            + "₩ §cen banque requis (actuel : §e" + k.getBank() + "₩§c)."));
                    case CANNOT_AFFORD ->
                        player.sendMessage(c(pre() + "&cFonds insuffisants pour payer le coût d'évolution : §e"
                            + next.getUpgradeCost() + "₩§c."));
                    case OK -> {
                        String newLvl = k.getLevel().getDisplayName();
                        int newMax = k.getMaxChunks();
                        for (UUID memberId : k.getMemberUUIDs()) {
                            Player m = Bukkit.getPlayer(memberId);
                            if (m != null) {
                                m.sendMessage(c(pre() + "&6&l✦ Royaume §e" + k.getName()
                                    + " &6&lévolué → §e" + newLvl + " &6&l✦"));
                                m.sendMessage(c(pre() + "&7Territoire débloqué : §e" + k.getClaimedChunks()
                                    + "§7/§e" + newMax + " §7chunks (§a+" + (newMax - k.getClaimedChunks())
                                    + " §7disponibles — §b/kingdom claim§7)"));
                            }
                        }
                    }
                }
            }

            // ── Slot 31 : Taxe ────────────────────────────────────────────
            case 31 -> {
                if (!isChief) { player.sendMessage(c(pre() + "&cRéservé au Chef.")); return; }
                chat().request(player, "Tapez le nouveau taux de taxe (0-100) :", input -> {
                    try {
                        k.setTaxRate(Integer.parseInt(input));
                        km().save();
                        player.sendMessage(c(pre() + "&aTaux de taxe fixé à §e" + k.getTaxRate() + "%"));
                    } catch (NumberFormatException e) { player.sendMessage(c(pre() + "&cValeur invalide (0-100).")); }
                });
            }

            // ── Slot 33 : Frais d'entrée ──────────────────────────────────
            case 33 -> {
                if (!isChief) { player.sendMessage(c(pre() + "&cRéservé au Chef.")); return; }
                chat().request(player, "Tapez le nouveau montant des frais d'entrée (en ₩) :", input -> {
                    try {
                        k.setEntryFee(Integer.parseInt(input));
                        km().save();
                        player.sendMessage(c(pre() + "&aFrais d'entrée fixés à §e" + k.getEntryFee() + "₩"));
                    } catch (NumberFormatException e) { player.sendMessage(c(pre() + "&cMontant invalide.")); }
                });
            }

            // ── Slot 36 : Quitter / Dissoudre ─────────────────────────────
            case 36 -> {
                if (isChief && !event.getClick().isShiftClick()) {
                    player.sendMessage(c(pre()
                        + "&cChef : faites §4Shift+Clic §csur §lDissoudre §cpour confirmer."));
                    return;
                }
                player.closeInventory();
                km().leaveKingdom(player);
                player.sendMessage(c(pre() + (isChief
                    ? "&cVous avez dissous votre royaume."
                    : "&cVous avez quitté le royaume.")));
            }

            // ── Slot 44 : Retour au menu joueur ───────────────────────────
            case 44 -> {
                player.closeInventory();
                plugin.getPlayerMenuGUI().open(player);
            }
        }
    }

    // ── Helpers partagés ─────────────────────────────────────────────────

    private void requestCreate(Player player) {
        chat().request(player, "Tapez le nom de votre nouveau royaume :", name -> {
            switch (km().createKingdom(player, name)) {
                case OK -> {
                    player.sendMessage(c(pre() + "&aRoyaume §e" + name + " §acréé !"));
                    player.sendMessage(c(pre() + "&7Un territoire de §e10 chunks §7a été attribué automatiquement."));
                    player.sendMessage(c(pre() + "&7Tapez §b/kingdom home §7pour vous y téléporter."));
                }
                case ALREADY_IN_KINGDOM ->
                    player.sendMessage(c(pre() + "&cVous êtes déjà dans un royaume."));
                case NAME_TAKEN ->
                    player.sendMessage(c(pre() + "&cCe nom est déjà pris. Choisissez-en un autre."));
                case MAX_REACHED ->
                    player.sendMessage(c(pre() + "&cNombre maximum de royaumes atteint."));
                case TERRITORY_NOT_FOUND ->
                    player.sendMessage(c(pre() + "&cImpossible de trouver un territoire valide. Réessayez."));
            }
        });
    }

    private void requestJoin(Player player) {
        player.closeInventory();
        if (km().getAllKingdoms().isEmpty()) {
            player.sendMessage(c(pre() + "&cAucun royaume n'existe encore. Soyez le premier à en créer un !"));
            return;
        }
        player.sendMessage(c(pre() + "&eRoyaumes disponibles :"));
        for (Kingdom k : km().getAllKingdoms()) {
            player.sendMessage(c("  §b" + k.getName() + " §7— " + k.getLevel().getDisplayName()
                + " §8(" + k.getMemberCount() + " membres)"));
        }
        chat().request(player, "Tapez le nom du royaume à rejoindre (invitation requise) :", name -> {
            switch (km().joinKingdom(player, name)) {
                case OK ->
                    player.sendMessage(c(pre() + "&aVous avez rejoint §e" + name + " §a!"));
                case ALREADY_IN_KINGDOM ->
                    player.sendMessage(c(pre() + "&cVous êtes déjà dans un royaume."));
                case KINGDOM_NOT_FOUND ->
                    player.sendMessage(c(pre() + "&cRoyaume introuvable : §e" + name));
                case NO_INVITE ->
                    player.sendMessage(c(pre() + "&cVous n'avez pas d'invitation pour §e" + name + "&c."));
            }
        });
    }

    private void goSpawn(Player player) {
        player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        player.sendMessage(c(pre() + "&aBienvenue sur TheWarDead !"));
    }

    private KingdomManager km() { return plugin.getKingdomManager(); }
    private fr.thewardead.kingdoms.gui.ChatInputManager chat() { return plugin.getChatInputManager(); }
    private String pre() { return plugin.getConfig().getString("messages.prefix", "&8[&cTWD&8] &r"); }
    private String c(String s) { return org.bukkit.ChatColor.translateAlternateColorCodes('&', s); }
}
