package fr.thewardead.economy.listener;

import fr.thewardead.economy.EconomyPlugin;
import fr.thewardead.economy.gui.TradeGUI;
import fr.thewardead.economy.model.TradeSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Set;

public class TradeListener implements Listener {

    private final EconomyPlugin plugin;

    public TradeListener(EconomyPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!e.getView().getTitle().equals(TradeGUI.TITLE)) return;

        TradeSession session = plugin.getTradeManager().getSession(p.getUniqueId());
        if (session == null) { e.setCancelled(true); return; }

        int slot = e.getRawSlot();
        boolean isA = p.getUniqueId().equals(session.getPlayerA());

        // ── Boutons fixes (hors zone items/mise) ──────────────────────────
        // Toujours annuler le clic sur les slots de contrôle
        if (isControlSlot(slot)) {
            e.setCancelled(true);
            handleControlClick(p, session, slot, isA);
            return;
        }

        // ── Séparateur et zone adverse : interdire ──────────────────────
        int[] allowedArr = isA ? TradeSession.SLOTS_A : TradeSession.SLOTS_B;
        int goldSlot = isA ? TradeSession.SLOT_GOLD_A : TradeSession.SLOT_GOLD_B;

        boolean inOwnZone = Arrays.stream(allowedArr).anyMatch(s -> s == slot) || slot == goldSlot;

        if (!inOwnZone) {
            e.setCancelled(true);
            return;
        }

        // ── Mouvement dans sa propre zone : autorisé, reset confirm ─────
        session.resetConfirm();
        session.touch();
        Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getTradeGUI().refresh(session), 1L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (!e.getView().getTitle().equals(TradeGUI.TITLE)) return;

        TradeSession session = plugin.getTradeManager().getSession(p.getUniqueId());
        if (session == null) return;
        if (session.getState() == TradeSession.State.DONE
            || session.getState() == TradeSession.State.CANCELLED) return;

        plugin.getTradeManager().cancelAndReturn(session, p.getName() + " a fermé l'inventaire.");
    }

    // ── Gestion des boutons ───────────────────────────────────────────────

    private void handleControlClick(Player p, TradeSession session, int slot, boolean isA) {
        int confirmSlot = isA ? TradeSession.SLOT_CONFIRM_A : TradeSession.SLOT_CONFIRM_B;
        int cancelSlot  = isA ? TradeSession.SLOT_CANCEL_A  : TradeSession.SLOT_CANCEL_B;

        if (slot == cancelSlot) {
            plugin.getTradeManager().cancelAndReturn(session, p.getName() + " a annulé le troc.");
            return;
        }

        if (slot == confirmSlot) {
            boolean done = session.confirm(p.getUniqueId());
            if (done) {
                executeExchange(session);
            } else {
                plugin.getTradeGUI().refresh(session);
            }
        }
    }

    // ── Échange effectif ─────────────────────────────────────────────────

    private void executeExchange(TradeSession session) {
        Player pA = Bukkit.getPlayer(session.getPlayerA());
        Player pB = Bukkit.getPlayer(session.getPlayerB());

        var inv = session.getInventory();

        // Collecter items A → aller vers B
        ItemStack[] forB = collectItems(inv, TradeSession.SLOTS_A, TradeSession.SLOT_GOLD_A);
        // Collecter items B → aller vers A
        ItemStack[] forA = collectItems(inv, TradeSession.SLOTS_B, TradeSession.SLOT_GOLD_B);

        if (pA != null) {
            pA.closeInventory();
            for (ItemStack it : forA) if (it != null)
                pA.getInventory().addItem(it).values()
                    .forEach(drop -> pA.getWorld().dropItemNaturally(pA.getLocation(), drop));
            pA.sendMessage(c(pre() + "&a§lTroc effectué avec succès !"));
        }
        if (pB != null) {
            pB.closeInventory();
            for (ItemStack it : forB) if (it != null)
                pB.getInventory().addItem(it).values()
                    .forEach(drop -> pB.getWorld().dropItemNaturally(pB.getLocation(), drop));
            pB.sendMessage(c(pre() + "&a§lTroc effectué avec succès !"));
        }

        plugin.getTradeManager().closeSession(session.getPlayerA());
    }

    private ItemStack[] collectItems(org.bukkit.inventory.Inventory inv, int[] slots, int goldSlot) {
        int len = slots.length + 1;
        ItemStack[] items = new ItemStack[len];
        for (int i = 0; i < slots.length; i++) {
            items[i] = inv.getItem(slots[i]);
            inv.setItem(slots[i], null);
        }
        items[slots.length] = inv.getItem(goldSlot);
        inv.setItem(goldSlot, null);
        return items;
    }

    // ── Util ────────────────────────────────────────────────────────────────

    private boolean isControlSlot(int slot) {
        return slot == TradeSession.SLOT_STATUS
            || slot == TradeSession.SLOT_CONFIRM_A || slot == TradeSession.SLOT_CANCEL_A
            || slot == TradeSession.SLOT_CONFIRM_B || slot == TradeSession.SLOT_CANCEL_B
            || isSeparator(slot);
    }

    private boolean isSeparator(int slot) {
        // Colonne 4 de chaque rangée (slots 4,13,22,31,40) = séparateur
        return slot == 4 || slot == 13 || slot == 22 || slot == 31 || slot == 40;
    }

    private String pre() { return plugin.getConfig().getString("messages.prefix", "&8[&6₩&8] &r"); }
    private String c(String s) { return org.bukkit.ChatColor.translateAlternateColorCodes('&', s); }
}
