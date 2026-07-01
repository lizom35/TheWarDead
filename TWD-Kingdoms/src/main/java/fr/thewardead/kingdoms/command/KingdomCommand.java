package fr.thewardead.kingdoms.command;

import fr.thewardead.kingdoms.KingdomPlugin;
import fr.thewardead.kingdoms.manager.KingdomManager;
import fr.thewardead.kingdoms.model.ChunkCoord;
import fr.thewardead.kingdoms.model.Kingdom;
import fr.thewardead.kingdoms.model.KingdomLevel;
import fr.thewardead.kingdoms.model.KingdomPlayer;
import fr.thewardead.kingdoms.model.KingdomRole;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class KingdomCommand implements CommandExecutor, TabCompleter {

    private final KingdomPlugin plugin;
    private final KingdomManager km;

    public KingdomCommand(KingdomPlugin plugin) {
        this.plugin = plugin;
        this.km = plugin.getKingdomManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        if (args.length == 0) { sendHelp(p); return true; }

        switch (args[0].toLowerCase()) {
            case "create"    -> handleCreate(p, args);
            case "join"      -> handleJoin(p, args);
            case "leave"     -> handleLeave(p);
            case "invite"    -> handleInvite(p, args);
            case "accept"    -> handleAccept(p, args);
            case "kick"      -> handleKick(p, args);
            case "setrank"   -> handleSetRank(p, args);
            case "home"      -> handleHome(p);
            case "border"    -> handleBorder(p);
            case "info"      -> handleInfo(p, args);
            case "list"      -> handleList(p);
            case "bank"      -> handleBank(p, args);
            case "setfee"    -> handleSetFee(p, args);
            case "settax"    -> handleSetTax(p, args);
            case "upgrade"   -> handleUpgrade(p);
            case "claim"     -> handleClaim(p);
            case "territory" -> handleTerritory(p);
            default          -> sendHelp(p);
        }
        return true;
    }

    // ---- Sous-commandes ----

    private void handleCreate(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage(c(pre() + "&cUsage : /kingdom create <nom>")); return; }
        String name = args[1];

        switch (km.createKingdom(p, name)) {
            case OK -> {
                p.sendMessage(c(pre() + msg("kingdom-created").replace("%name%", name)));
                p.sendMessage(c(pre() + msg("territory-found")));
            }
            case ALREADY_IN_KINGDOM ->
                p.sendMessage(c(pre() + msg("already-in-kingdom")));
            case NAME_TAKEN ->
                p.sendMessage(c(pre() + "&cCe nom de royaume est déjà pris."));
            case MAX_REACHED ->
                p.sendMessage(c(pre() + msg("max-kingdoms")));
            case TERRITORY_NOT_FOUND ->
                p.sendMessage(c(pre() + msg("territory-not-found")));
        }
    }

    private void handleJoin(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage(c(pre() + "&cUsage : /kingdom join <nom>")); return; }
        String name = args[1];

        switch (km.joinKingdom(p, name)) {
            case OK ->
                p.sendMessage(c(pre() + msg("kingdom-joined").replace("%name%", name)));
            case ALREADY_IN_KINGDOM ->
                p.sendMessage(c(pre() + msg("already-in-kingdom")));
            case KINGDOM_NOT_FOUND ->
                p.sendMessage(c(pre() + msg("kingdom-not-found").replace("%name%", name)));
            case NO_INVITE ->
                p.sendMessage(c(pre() + "&cVous n'avez pas d'invitation pour ce royaume."));
        }
    }

    private void handleLeave(Player p) {
        if (!km.leaveKingdom(p))
            p.sendMessage(c(pre() + msg("not-in-kingdom")));
        else
            p.sendMessage(c(pre() + "&aVous avez quitté votre royaume."));
    }

    private void handleInvite(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage(c(pre() + "&cUsage : /kingdom invite <joueur>")); return; }

        Kingdom k = km.getPlayerKingdom(p.getUniqueId());
        if (k == null) { p.sendMessage(c(pre() + msg("not-in-kingdom"))); return; }
        if (!k.getRole(p.getUniqueId()).isAtLeast(KingdomRole.OFFICIER)) {
            p.sendMessage(c(pre() + msg("no-permission"))); return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { p.sendMessage(c(pre() + msg("player-not-found").replace("%name%", args[1]))); return; }

        if (!km.invitePlayer(k, target)) {
            p.sendMessage(c(pre() + "&cCe joueur est déjà dans un royaume.")); return;
        }
        p.sendMessage(c(pre() + msg("invite-sent").replace("%player%", target.getName())));
        target.sendMessage(c(pre() + msg("invite-received")
            .replace("%player%", p.getName())
            .replace("%kingdom%", k.getName())));
    }

    private void handleAccept(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage(c(pre() + "&cUsage : /kingdom accept <nom>")); return; }
        handleJoin(p, args);
    }

    private void handleKick(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage(c(pre() + "&cUsage : /kingdom kick <joueur>")); return; }

        Kingdom k = km.getPlayerKingdom(p.getUniqueId());
        if (k == null) { p.sendMessage(c(pre() + msg("not-in-kingdom"))); return; }
        if (!k.getRole(p.getUniqueId()).isAtLeast(KingdomRole.OFFICIER)) {
            p.sendMessage(c(pre() + msg("no-permission"))); return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUUID = target != null ? target.getUniqueId()
            : Bukkit.getOfflinePlayer(args[1]).getUniqueId();

        if (!km.kickPlayer(k, targetUUID)) {
            p.sendMessage(c(pre() + "&cImpossible d'expulser ce joueur.")); return;
        }
        p.sendMessage(c(pre() + "&aJoueur expulsé."));
        if (target != null) target.sendMessage(c(pre() + msg("kicked").replace("%name%", k.getName())));
    }

    private void handleSetRank(Player p, String[] args) {
        if (args.length < 3) {
            p.sendMessage(c(pre() + "&cUsage : /kingdom setrank <joueur> <MEMBRE|OFFICIER>")); return;
        }
        Kingdom k = km.getPlayerKingdom(p.getUniqueId());
        if (k == null) { p.sendMessage(c(pre() + msg("not-in-kingdom"))); return; }
        if (!k.getChiefUUID().equals(p.getUniqueId())) {
            p.sendMessage(c(pre() + msg("no-permission"))); return;
        }

        KingdomRole newRole;
        try { newRole = KingdomRole.valueOf(args[2].toUpperCase()); }
        catch (Exception e) { p.sendMessage(c(pre() + "&cRôle invalide. Utilisez MEMBRE ou OFFICIER.")); return; }

        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUUID = target != null ? target.getUniqueId()
            : Bukkit.getOfflinePlayer(args[1]).getUniqueId();

        if (!km.setRole(k, targetUUID, newRole)) {
            p.sendMessage(c(pre() + "&cImpossible de modifier le rôle.")); return;
        }
        p.sendMessage(c(pre() + "&aRôle mis à jour."));
        if (target != null)
            target.sendMessage(c(pre() + "&eVotre rôle dans &b" + k.getName()
                + " &eest maintenant : &a" + newRole.getDisplayName()));
    }

    private void handleHome(Player p) {
        Kingdom k = km.getPlayerKingdom(p.getUniqueId());
        if (k == null) { p.sendMessage(c(pre() + msg("not-in-kingdom"))); return; }
        if (k.getHome() == null) {
            p.sendMessage(c(pre() + "&cAucun point de retour défini pour ce royaume.")); return;
        }
        p.teleport(k.getHome());
        p.sendMessage(c(pre() + msg("home-teleported")));
    }

    private void handleInfo(Player p, String[] args) {
        Kingdom k;
        if (args.length >= 2) {
            k = km.getKingdomByName(args[1]);
            if (k == null) { p.sendMessage(c(pre() + msg("kingdom-not-found").replace("%name%", args[1]))); return; }
        } else {
            k = km.getPlayerKingdom(p.getUniqueId());
            if (k == null) { p.sendMessage(c(pre() + msg("not-in-kingdom"))); return; }
        }

        KingdomLevel next = k.getLevel().next();
        int income = plugin.getConfig().getInt("income." + k.getLevel().name(), 50);

        p.sendMessage(c("&8&m                    "));
        p.sendMessage(c("  &c&l" + k.getName() + "  &7— " + k.getLevel().getDisplayName()));
        p.sendMessage(c("  &7Chef : &e" + Bukkit.getOfflinePlayer(k.getChiefUUID()).getName()));
        p.sendMessage(c("  &7Membres : &e" + k.getMemberCount()
            + (next != null ? " &8(prochain niveau : &e" + next.getMinMembers() + "&8)" : "")));
        p.sendMessage(c("  &7Trésor : &e" + k.getBank() + "₩"
            + (next != null ? " &8(coût évolution : &e" + next.getUpgradeCost() + "₩&8)" : "")));
        p.sendMessage(c("  &7Revenu/h : &a+" + income + "₩"));
        p.sendMessage(c("  &7Territoire : &e" + k.getClaimedChunks() + "&7/&e" + k.getMaxChunks() + " &7chunks"
            + (k.getClaimedChunks() < k.getMaxChunks()
                ? "  &a(" + (k.getMaxChunks() - k.getClaimedChunks()) + " disponibles)"
                : "")));
        p.sendMessage(c("  &7Taxe : &e" + k.getTaxRate() + "%  &7Entrée : &e" + k.getEntryFee() + "₩"));
        p.sendMessage(c("&8&m                    "));
    }

    private void handleList(Player p) {
        p.sendMessage(c("&8&m                    "));
        p.sendMessage(c("  &c&lRoyaumes actifs"));
        if (km.getAllKingdoms().isEmpty()) {
            p.sendMessage(c("  &7Aucun royaume pour le moment.")); return;
        }
        for (Kingdom k : km.getAllKingdoms()) {
            p.sendMessage(c("  &e" + k.getName() + " &7— " + k.getLevel().getDisplayName()
                + " &7(" + k.getMemberCount() + " membres, "
                + k.getClaimedChunks() + "/" + k.getMaxChunks() + " chunks)"));
        }
        p.sendMessage(c("&8&m                    "));
    }

    private void handleBank(Player p, String[] args) {
        Kingdom k = km.getPlayerKingdom(p.getUniqueId());
        if (k == null) { p.sendMessage(c(pre() + msg("not-in-kingdom"))); return; }

        if (args.length >= 3 && args[1].equalsIgnoreCase("deposit")) {
            try {
                long amount = Long.parseLong(args[2]);
                KingdomPlayer kp = km.getOrCreatePlayer(p);
                if (!kp.withdraw(amount)) { p.sendMessage(c(pre() + "&cSolde insuffisant.")); return; }
                k.depositBank(amount);
                km.save();
                p.sendMessage(c(pre() + "&aDépôt de &e" + amount + "₩ &adans la banque du royaume."));
            } catch (NumberFormatException e) {
                p.sendMessage(c(pre() + "&cMontant invalide."));
            }
        } else if (args.length >= 3 && args[1].equalsIgnoreCase("withdraw")) {
            if (!k.getChiefUUID().equals(p.getUniqueId())) {
                p.sendMessage(c(pre() + msg("no-permission"))); return;
            }
            try {
                long amount = Long.parseLong(args[2]);
                if (!k.withdrawBank(amount)) { p.sendMessage(c(pre() + "&cFonds insuffisants.")); return; }
                km.getOrCreatePlayer(p).deposit(amount);
                km.save();
                p.sendMessage(c(pre() + "&aRetrait de &e" + amount + "₩ &ade la banque du royaume."));
            } catch (NumberFormatException e) {
                p.sendMessage(c(pre() + "&cMontant invalide."));
            }
        } else {
            p.sendMessage(c(pre() + "&7Banque de &e" + k.getName() + " &7: &a" + k.getBank() + "₩"));
            p.sendMessage(c(pre() + "&7Usage : /kingdom bank deposit <montant> | withdraw <montant>"));
        }
    }

    private void handleSetFee(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage(c(pre() + "&cUsage : /kingdom setfee <montant>")); return; }
        Kingdom k = km.getPlayerKingdom(p.getUniqueId());
        if (k == null) { p.sendMessage(c(pre() + msg("not-in-kingdom"))); return; }
        if (!k.getChiefUUID().equals(p.getUniqueId())) { p.sendMessage(c(pre() + msg("no-permission"))); return; }
        try {
            k.setEntryFee(Integer.parseInt(args[1]));
            km.save();
            p.sendMessage(c(pre() + msg("fee-set").replace("%amount%", args[1])));
        } catch (NumberFormatException e) { p.sendMessage(c(pre() + "&cMontant invalide.")); }
    }

    private void handleSetTax(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage(c(pre() + "&cUsage : /kingdom settax <0-100>")); return; }
        Kingdom k = km.getPlayerKingdom(p.getUniqueId());
        if (k == null) { p.sendMessage(c(pre() + msg("not-in-kingdom"))); return; }
        if (!k.getChiefUUID().equals(p.getUniqueId())) { p.sendMessage(c(pre() + msg("no-permission"))); return; }
        try {
            k.setTaxRate(Integer.parseInt(args[1]));
            km.save();
            p.sendMessage(c(pre() + msg("tax-set").replace("%rate%", String.valueOf(k.getTaxRate()))));
        } catch (NumberFormatException e) { p.sendMessage(c(pre() + "&cValeur invalide (0-100).")); }
    }

    private void handleUpgrade(Player p) {
        Kingdom k = km.getPlayerKingdom(p.getUniqueId());
        if (k == null) { p.sendMessage(c(pre() + msg("not-in-kingdom"))); return; }
        if (!k.getChiefUUID().equals(p.getUniqueId())) {
            p.sendMessage(c(pre() + "&cSeul le Chef peut faire évoluer le royaume.")); return;
        }

        KingdomLevel next = k.getLevel().next();
        if (next == null) {
            p.sendMessage(c(pre() + "&eVotre royaume est au niveau maximum : &c" + k.getLevel().getDisplayName()));
            return;
        }

        // Afficher les critères si pas encore atteints
        switch (km.upgradeKingdom(k)) {
            case MAX_LEVEL ->
                p.sendMessage(c(pre() + "&eNiveau maximum atteint."));
            case NOT_ENOUGH_MEMBERS ->
                p.sendMessage(c(pre() + "&cCritère non rempli : &e" + next.getMinMembers()
                    + " membres &crequis (actuel : &e" + k.getMemberCount() + "&c)."));
            case NOT_ENOUGH_BANK ->
                p.sendMessage(c(pre() + "&cCritère non rempli : &e" + next.getMinBank()
                    + "₩ en banque &crequis (actuel : &e" + k.getBank() + "₩&c)."));
            case CANNOT_AFFORD ->
                p.sendMessage(c(pre() + "&cFonds insuffisants. Coût : &e" + next.getUpgradeCost()
                    + "₩ &c(banque : &e" + k.getBank() + "₩&c)."));
            case OK -> {
                KingdomLevel newLevel = k.getLevel(); // déjà mis à jour par upgradeKingdom
                int newMax = newLevel.getMaxChunks();
                int claimed = k.getClaimedChunks();
                for (UUID memberId : k.getMemberUUIDs()) {
                    Player member = Bukkit.getPlayer(memberId);
                    if (member != null) {
                        member.sendMessage(c(pre()
                            + "&6&l✦ Royaume &e" + k.getName()
                            + " &6&lévolué → &e" + newLevel.getDisplayName() + " &6&l✦"));
                        member.sendMessage(c(pre()
                            + "&7Territoire : &e" + claimed + "&7/&e" + newMax
                            + " &7chunks. Utilisez &b/kingdom claim &7pour réclamer les nouveaux."));
                    }
                }
            }
        }
    }

    private void handleClaim(Player p) {
        Kingdom k = km.getPlayerKingdom(p.getUniqueId());
        if (k == null) { p.sendMessage(c(pre() + msg("not-in-kingdom"))); return; }
        if (!k.getRole(p.getUniqueId()).isAtLeast(KingdomRole.OFFICIER)) {
            p.sendMessage(c(pre() + "&cSeuls les Officiers et le Chef peuvent revendiquer des chunks.")); return;
        }

        org.bukkit.Chunk chunk = p.getLocation().getChunk();
        ChunkCoord coord = new ChunkCoord(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());

        switch (km.claimChunk(k, coord)) {
            case TERRITORY_FULL ->
                p.sendMessage(c(pre() + "&cTerritoire plein (&e" + k.getMaxChunks()
                    + " chunks&c). Faites &b/kingdom upgrade &cpour débloquer plus."));
            case ALREADY_CLAIMED -> {
                Kingdom owner = km.getKingdomAt(coord);
                String ownerName = owner != null ? owner.getName() : "un autre royaume";
                p.sendMessage(c(pre() + "&cCe chunk appartient déjà à &e" + ownerName + "&c."));
            }
            case OK ->
                p.sendMessage(c(pre() + "&aChunk revendiqué ! Territoire : &e"
                    + k.getClaimedChunks() + "&a/&e" + k.getMaxChunks() + " &achunks."));
        }
    }

    private void handleTerritory(Player p) {
        Kingdom k = km.getPlayerKingdom(p.getUniqueId());
        if (k == null) { p.sendMessage(c(pre() + msg("not-in-kingdom"))); return; }

        int income = plugin.getConfig().getInt("income." + k.getLevel().name(), 50);
        KingdomLevel next = k.getLevel().next();

        p.sendMessage(c("&8&m                    "));
        p.sendMessage(c("  &c&lTerritoire — " + k.getName()));
        p.sendMessage(c("  &7Niveau : &e" + k.getLevel().getDisplayName()));
        p.sendMessage(c("  &7Chunks : &e" + k.getClaimedChunks() + "&7/&e" + k.getMaxChunks()));
        if (k.getClaimedChunks() < k.getMaxChunks())
            p.sendMessage(c("  &a→ " + (k.getMaxChunks() - k.getClaimedChunks())
                + " chunks à revendiquer (/kingdom claim)"));
        if (next != null) {
            p.sendMessage(c("  &7Évolution vers &e" + next.getDisplayName() + " &7:"));
            p.sendMessage(c("    &7Membres requis : &e" + next.getMinMembers()
                + " &8(actuel : &e" + k.getMemberCount() + "&8)"));
            p.sendMessage(c("    &7Banque requise : &e" + next.getMinBank()
                + "₩ &8(actuel : &e" + k.getBank() + "₩&8)"));
            p.sendMessage(c("    &7Coût d'évolution : &e" + next.getUpgradeCost() + "₩"));
            p.sendMessage(c("    &7Chunks débloqués : &e+" + (next.getMaxChunks() - k.getMaxChunks())));
        }
        p.sendMessage(c("&8&m                    "));
    }

    private void handleBorder(Player p) {
        int cx = p.getLocation().getBlockX() >> 4;
        int cz = p.getLocation().getBlockZ() >> 4;
        String world = p.getWorld().getName();
        final int range = 3; // 7×7 grid

        Kingdom own = km.getPlayerKingdom(p.getUniqueId());

        // Collect foreign kingdoms encountered in the grid and assign them a color
        String[] palette = {"&e", "&6", "&d", "&c", "&9"};
        java.util.Map<UUID, String[]> others = new java.util.LinkedHashMap<>();
        int[] palIdx = {0};
        for (int dz = -range; dz <= range; dz++) {
            for (int dx = -range; dx <= range; dx++) {
                Kingdom k = km.getKingdomAt(new ChunkCoord(world, cx + dx, cz + dz));
                if (k == null) continue;
                if (own != null && k.getId().equals(own.getId())) continue;
                others.computeIfAbsent(k.getId(), id -> {
                    String col = palette[palIdx[0] % palette.length];
                    palIdx[0]++;
                    String sym = k.getName().isEmpty() ? "?" : String.valueOf(k.getName().charAt(0)).toUpperCase();
                    return new String[]{col, sym, k.getName()};
                });
            }
        }

        p.sendMessage(c("&8&m                                   "));
        p.sendMessage(c("  &c&l⌁ Carte &8— &7chunk &8(" + cx + ", " + cz + ")  &8↑&7=Nord"));

        for (int dz = -range; dz <= range; dz++) {
            StringBuilder row = new StringBuilder("  ");
            for (int dx = -range; dx <= range; dx++) {
                if (dx == 0 && dz == 0) {
                    row.append("&b[@]");
                    continue;
                }
                Kingdom k = km.getKingdomAt(new ChunkCoord(world, cx + dx, cz + dz));
                if (k == null) {
                    row.append("&8[.]");
                } else if (own != null && k.getId().equals(own.getId())) {
                    row.append("&a[#]");
                } else {
                    String[] info = others.get(k.getId());
                    row.append(info[0]).append("[").append(info[1]).append("]");
                }
            }
            p.sendMessage(c(row.toString()));
        }

        StringBuilder leg = new StringBuilder("  &7");
        leg.append("&b[@]&7 Vous  &8[.] &7Neutre");
        if (own != null) leg.append("  &a[#] &7").append(own.getName());
        for (String[] info : others.values()) {
            leg.append("  ").append(info[0]).append("[").append(info[1]).append("] &7").append(info[2]);
        }
        p.sendMessage(c(leg.toString()));
        p.sendMessage(c("&8&m                                   "));
    }

    private void sendHelp(Player p) {
        p.sendMessage(c("&8&m                    "));
        p.sendMessage(c("  &c&lTWD Royaumes &8— Aide"));
        p.sendMessage(c("  &b/kingdom create <nom>    &7Créer un royaume"));
        p.sendMessage(c("  &b/kingdom join <nom>      &7Rejoindre (avec invitation)"));
        p.sendMessage(c("  &b/kingdom leave           &7Quitter votre royaume"));
        p.sendMessage(c("  &b/kingdom invite <joueur> &7Inviter un joueur"));
        p.sendMessage(c("  &b/kingdom kick <joueur>   &7Expulser un membre"));
        p.sendMessage(c("  &b/kingdom setrank <joueur> <rôle>"));
        p.sendMessage(c("  &b/kingdom home            &7Se TP au royaume"));
        p.sendMessage(c("  &b/kingdom info [nom]      &7Infos sur un royaume"));
        p.sendMessage(c("  &b/kingdom list            &7Liste des royaumes"));
        p.sendMessage(c("  &b/kingdom bank [deposit|withdraw] <montant>"));
        p.sendMessage(c("  &b/kingdom setfee <montant> &7Frais d'entrée territoire"));
        p.sendMessage(c("  &b/kingdom settax <0-100>  &7Taux de taxe"));
        p.sendMessage(c("  &b/kingdom upgrade         &7Évoluer au niveau suivant"));
        p.sendMessage(c("  &b/kingdom claim           &7Revendiquer le chunk où vous êtes"));
        p.sendMessage(c("  &b/kingdom territory       &7Infos territoire & évolution"));
        p.sendMessage(c("  &b/kingdom border          &7Afficher les limites du territoire (10s)"));
        p.sendMessage(c("&8&m                    "));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create","join","leave","invite","accept","kick","setrank",
                "home","info","list","bank","setfee","settax","upgrade","claim","territory","border");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("join"))) {
            return km.getAllKingdoms().stream().map(Kingdom::getName).toList();
        }
        return List.of();
    }

    // ---- Util ----

    private String pre() { return plugin.getConfig().getString("messages.prefix", "&8[&cTWD&8] &r"); }
    private String msg(String key) { return plugin.getConfig().getString("messages." + key, ""); }
    private String c(String s) { return org.bukkit.ChatColor.translateAlternateColorCodes('&', s); }
}
