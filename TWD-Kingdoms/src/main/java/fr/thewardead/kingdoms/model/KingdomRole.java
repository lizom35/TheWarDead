package fr.thewardead.kingdoms.model;

public enum KingdomRole {
    MEMBRE("Membre"),
    OFFICIER("Officier"),
    CHEF("Chef");

    private final String displayName;

    KingdomRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAtLeast(KingdomRole role) {
        return this.ordinal() >= role.ordinal();
    }
}
