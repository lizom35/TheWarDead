package fr.thewardead.economy.manager;

import fr.thewardead.economy.EconomyPlugin;
import fr.thewardead.kingdoms.manager.KingdomManager;
import fr.thewardead.kingdoms.model.KingdomPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.stream.Collectors;

public class EconomyManager {

    private final EconomyPlugin plugin;
    private final KingdomManager km;

    public EconomyManager(EconomyPlugin plugin, KingdomManager km) {
        this.plugin = plugin;
        this.km     = km;
    }

    public long getBalance(UUID uuid) {
        KingdomPlayer kp = km.getPlayer(uuid);
        return kp != null ? kp.getBalance() : 0L;
    }

    public boolean deposit(UUID uuid, long amount) {
        KingdomPlayer kp = km.getPlayer(uuid);
        if (kp == null || amount <= 0) return false;
        kp.deposit(amount);
        km.save();
        return true;
    }

    public boolean withdraw(UUID uuid, long amount) {
        KingdomPlayer kp = km.getPlayer(uuid);
        if (kp == null || amount <= 0) return false;
        boolean result = kp.withdraw(amount);
        if (result) km.save();
        return result;
    }

    public boolean has(UUID uuid, long amount) {
        return getBalance(uuid) >= amount;
    }

    /** Transfère amount de 'from' vers 'to'. Applique la taxe configurée. */
    public boolean transfer(UUID from, UUID to, long amount) {
        if (!has(from, amount)) return false;
        int taxPct = plugin.getConfig().getInt("transaction-tax-percent", 5);
        long tax   = amount * taxPct / 100;
        long net   = amount - tax;
        if (!withdraw(from, amount)) return false;
        deposit(to, net);
        return true;
    }

    /** Top N joueurs par solde. */
    public List<Map.Entry<String, Long>> getTopBalances(int limit) {
        return km.getAllPlayers().stream()
            .filter(kp -> kp.getBalance() > 0)
            .sorted(Comparator.comparingLong(KingdomPlayer::getBalance).reversed())
            .limit(limit)
            .map(kp -> Map.entry(
                kp.getName() != null ? kp.getName() : "?",
                kp.getBalance()))
            .collect(Collectors.toList());
    }
}
