package fr.thewardead.economy.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/** Annonce admin-shop : stock infini, prix fixe. */
public class AdminListing {

    private final UUID      id;
    private final ItemStack template; // toujours 1 unité
    private long            pricePerUnit;

    public AdminListing(UUID id, ItemStack template, long pricePerUnit) {
        this.id           = id;
        this.template     = template.clone();
        this.template.setAmount(1);
        this.pricePerUnit = pricePerUnit;
    }

    /** Construit un ItemStack de qty unités. */
    public ItemStack build(int qty) {
        ItemStack s = template.clone();
        s.setAmount(Math.min(qty, 64));
        return s;
    }

    public UUID      getId()           { return id; }
    public ItemStack getTemplate()     { return template.clone(); }
    public long      getPricePerUnit() { return pricePerUnit; }
    public void      setPrice(long p)  { this.pricePerUnit = p; }
}
