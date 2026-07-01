package fr.thewardead.economy.model;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class TradeSession {

    public enum State { PENDING_ACCEPT, OPEN, CONFIRMED_A, CONFIRMED_B, DONE, CANCELLED }

    // Slots de l'inventaire réservés à chaque joueur
    // Joueur A : 0-2, 9-11, 18-20   |   Joueur B : 6-8, 15-17, 24-26
    public static final int[] SLOTS_A = {0,1,2, 9,10,11, 18,19,20};
    public static final int[] SLOTS_B = {6,7,8, 15,16,17, 24,25,26};
    public static final int SLOT_GOLD_A = 27;
    public static final int SLOT_GOLD_B = 35;
    public static final int SLOT_CONFIRM_A = 36;
    public static final int SLOT_CANCEL_A  = 37;
    public static final int SLOT_STATUS    = 31;
    public static final int SLOT_CANCEL_B  = 43;
    public static final int SLOT_CONFIRM_B = 44;

    private final UUID playerA;
    private final UUID playerB;
    private Inventory inventory;
    private State state = State.PENDING_ACCEPT;

    private long moneyA = 0;
    private long moneyB = 0;

    private long lastActivity = System.currentTimeMillis();

    public TradeSession(UUID playerA, UUID playerB) {
        this.playerA = playerA;
        this.playerB = playerB;
    }

    public boolean isSlotAllowedFor(UUID player, int slot) {
        boolean isA = player.equals(playerA);
        int[] allowed = isA ? SLOTS_A : SLOTS_B;
        for (int s : allowed) if (s == slot) return true;
        // Boutons confirm/cancel propres au joueur
        if (isA) return slot == SLOT_CONFIRM_A || slot == SLOT_CANCEL_A || slot == SLOT_GOLD_A;
        else     return slot == SLOT_CONFIRM_B || slot == SLOT_CANCEL_B || slot == SLOT_GOLD_B;
    }

    public boolean isPlayerASlot(int slot) {
        for (int s : SLOTS_A) if (s == slot) return true;
        return slot == SLOT_GOLD_A || slot == SLOT_CONFIRM_A || slot == SLOT_CANCEL_A;
    }

    public boolean isPlayerBSlot(int slot) {
        for (int s : SLOTS_B) if (s == slot) return true;
        return slot == SLOT_GOLD_B || slot == SLOT_CONFIRM_B || slot == SLOT_CANCEL_B;
    }

    /** Réinitialise les confirmations si l'offre change. */
    public void resetConfirm() {
        if (state == State.CONFIRMED_A) state = State.OPEN;
        if (state == State.CONFIRMED_B) state = State.OPEN;
        touch();
    }

    /** Retourne true si l'échange est prêt (les deux ont confirmé). */
    public boolean confirm(UUID player) {
        touch();
        if (player.equals(playerA)) {
            state = (state == State.CONFIRMED_B) ? State.DONE : State.CONFIRMED_A;
        } else {
            state = (state == State.CONFIRMED_A) ? State.DONE : State.CONFIRMED_B;
        }
        return state == State.DONE;
    }

    public boolean isConfirmedBy(UUID player) {
        if (player.equals(playerA)) return state == State.CONFIRMED_A || state == State.DONE;
        else                         return state == State.CONFIRMED_B || state == State.DONE;
    }

    public ItemStack[] getItemsFor(UUID player) {
        int[] slots = player.equals(playerA) ? SLOTS_A : SLOTS_B;
        ItemStack[] items = new ItemStack[slots.length];
        for (int i = 0; i < slots.length; i++) {
            items[i] = inventory.getItem(slots[i]);
        }
        return items;
    }

    public void touch() { lastActivity = System.currentTimeMillis(); }
    public boolean isTimedOut(long timeoutSeconds) {
        return (System.currentTimeMillis() - lastActivity) > timeoutSeconds * 1000L;
    }

    public UUID getPlayerA()       { return playerA; }
    public UUID getPlayerB()       { return playerB; }
    public Inventory getInventory(){ return inventory; }
    public void setInventory(Inventory inv) { this.inventory = inv; }
    public State getState()        { return state; }
    public void setState(State s)  { this.state = s; }
    public long getMoneyA()        { return moneyA; }
    public void setMoneyA(long v)  { this.moneyA = Math.max(0, v); }
    public long getMoneyB()        { return moneyB; }
    public void setMoneyB(long v)  { this.moneyB = Math.max(0, v); }
}
