package fr.thewardead.economy.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ShopListing {

    public enum ListingType { PERSONAL, KINGDOM }

    private final UUID id;
    private final UUID sellerId;
    private final String sellerName;
    private final ItemStack itemTemplate; // item modèle (1 unité)
    private int stock;
    private final long pricePerUnit;
    private final long createdAt;
    private final ListingType type;
    private final UUID kingdomId; // null si PERSONAL

    public ShopListing(UUID id, UUID sellerId, String sellerName,
                       ItemStack itemTemplate, int stock, long pricePerUnit,
                       ListingType type, UUID kingdomId) {
        this.id           = id;
        this.sellerId     = sellerId;
        this.sellerName   = sellerName;
        this.itemTemplate = itemTemplate.clone();
        this.itemTemplate.setAmount(1);
        this.stock        = stock;
        this.pricePerUnit = pricePerUnit;
        this.createdAt    = System.currentTimeMillis();
        this.type         = type;
        this.kingdomId    = kingdomId;
    }

    /** Retire qty items du stock. Retourne false si insuffisant. */
    public boolean consume(int qty) {
        if (stock < qty) return false;
        stock -= qty;
        return true;
    }

    public boolean isActive() { return stock > 0; }

    /** Construit un ItemStack de qty unités. */
    public ItemStack build(int qty) {
        ItemStack stack = itemTemplate.clone();
        stack.setAmount(qty);
        return stack;
    }

    public UUID getId()           { return id; }
    public UUID getSellerId()     { return sellerId; }
    public String getSellerName() { return sellerName; }
    public ItemStack getTemplate(){ return itemTemplate.clone(); }
    public int getStock()         { return stock; }
    public long getPricePerUnit() { return pricePerUnit; }
    public long getCreatedAt()    { return createdAt; }
    public ListingType getType()  { return type; }
    public UUID getKingdomId()    { return kingdomId; }
}
