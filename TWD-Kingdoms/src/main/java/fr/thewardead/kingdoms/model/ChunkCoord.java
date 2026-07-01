package fr.thewardead.kingdoms.model;

import java.util.Objects;

public final class ChunkCoord {

    public final String world;
    public final int x;
    public final int z;

    public ChunkCoord(String world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;
    }

    public String serialize() {
        return world + ":" + x + ":" + z;
    }

    public static ChunkCoord deserialize(String s) {
        String[] p = s.split(":", 3);
        return new ChunkCoord(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChunkCoord c)) return false;
        return x == c.x && z == c.z && world.equals(c.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, z);
    }

    @Override
    public String toString() {
        return serialize();
    }
}
