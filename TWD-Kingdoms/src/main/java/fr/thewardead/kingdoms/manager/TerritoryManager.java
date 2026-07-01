package fr.thewardead.kingdoms.manager;

import fr.thewardead.kingdoms.KingdomPlugin;
import fr.thewardead.kingdoms.model.ChunkCoord;
import fr.thewardead.kingdoms.model.Kingdom;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class TerritoryManager {

    /**
     * Forme du territoire initial (10 chunks) autour du centre {0,0}.
     * L'index CENTER_INDEX (4) correspond toujours au chunk central {0,0}.
     */
    private static final int[][] SHAPE = {
        {-1,-1}, {0,-1}, {1,-1},   // 0 1 2
        {-1, 0}, {0, 0}, {1, 0},   // 3 4 5  ← index 4 = centre
        {-1, 1}, {0, 1}, {1, 1},   // 6 7 8
        { 0, 2}                     // 9
    };
    public static final int CENTER_INDEX = 4;

    private final KingdomPlugin plugin;
    private final Random rng = new Random();

    public TerritoryManager(KingdomPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Cherche un emplacement terrestre valide pour 10 chunks, dans un rayon
     * [min-distance, max-distance] autour du spawn.
     * Retourne la liste des 10 ChunkCoords, ou une liste vide si aucun trouvé.
     */
    public List<ChunkCoord> findInitialTerritory(World world, Location spawn,
                                                  Collection<Kingdom> existing) {
        int minDist     = plugin.getConfig().getInt("territory.min-distance", 1000);
        int maxDist     = plugin.getConfig().getInt("territory.max-distance", 30000);
        int maxAttempts = plugin.getConfig().getInt("territory.max-search-attempts", 150);

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            double angle = rng.nextDouble() * 2 * Math.PI;
            double dist  = minDist + rng.nextDouble() * (maxDist - minDist);

            int cx = (int)(spawn.getBlockX() + Math.cos(angle) * dist) >> 4;
            int cz = (int)(spawn.getBlockZ() + Math.sin(angle) * dist) >> 4;

            List<ChunkCoord> chunks = buildChunks(world.getName(), cx, cz);

            // Check ALL chunks of the territory, not just the center
            if (allChunksLand(world, chunks) && !overlaps(chunks, existing)) {
                return chunks;
            }
        }

        plugin.getLogger().warning("TerritoryManager: aucun territoire trouvé après "
            + maxAttempts + " tentatives.");
        return List.of();
    }

    /**
     * Construit les 10 ChunkCoords à partir d'un centre (cx, cz).
     */
    public List<ChunkCoord> buildChunks(String worldName, int cx, int cz) {
        List<ChunkCoord> list = new ArrayList<>(SHAPE.length);
        for (int[] offset : SHAPE) {
            list.add(new ChunkCoord(worldName, cx + offset[0], cz + offset[1]));
        }
        return list;
    }

    /**
     * Calcule la Location du point d'accueil au centre du territoire
     * (charge le chunk central pour obtenir la hauteur de surface).
     */
    public Location getHomeForCenter(World world, int cx, int cz) {
        int blockX = cx * 16 + 8;
        int blockZ = cz * 16 + 8;
        world.loadChunk(cx, cz, true);
        int blockY = world.getHighestBlockAt(blockX, blockZ).getY() + 1;
        return new Location(world, blockX + 0.5, blockY, blockZ + 0.5, 0f, 0f);
    }

    /**
     * Retourne le royaume propriétaire du chunk, ou null.
     */
    public Kingdom getKingdomAt(ChunkCoord coord, Collection<Kingdom> kingdoms) {
        for (Kingdom k : kingdoms) {
            if (k.hasChunk(coord)) return k;
        }
        return null;
    }

    // ---- Vérifications privées ----

    private boolean allChunksLand(World world, List<ChunkCoord> chunks) {
        for (ChunkCoord coord : chunks) {
            if (!isBiomeLand(world, coord.x, coord.z)) return false;
        }
        return true;
    }

    /**
     * Biomes à rejeter explicitement (eau profonde, terrain ingérable, zones spéciales).
     * Les biomes inconnus (mods) passent le filtre si la surface est solide.
     */
    private static final List<String> BANNED_BIOMES = List.of(
        // Océans / eau
        "ocean", "deep_ocean", "cold_ocean", "deep_cold_ocean",
        "frozen_ocean", "deep_frozen_ocean", "lukewarm_ocean",
        "deep_lukewarm_ocean", "warm_ocean",
        // Rivières
        "river", "frozen_river",
        // Marécages
        "swamp", "mangrove_swamp",
        // Îles champignons
        "mushroom_fields",
        // Terrain extrême / inaccessible
        "frozen_peaks", "jagged_peaks", "ice_spikes", "snowy_slopes",
        // Zones souterraines (ne devrait pas apparaître en surface, mais au cas où)
        "deep_dark"
    );

    private boolean isBiomeLand(World world, int cx, int cz) {
        try {
            int bx = cx * 16 + 8;
            int bz = cz * 16 + 8;
            String biomeKey = world.getBiome(bx, 64, bz).getKey().getKey().toLowerCase();

            // Rejeter si biome explicitement banni
            for (String banned : BANNED_BIOMES) {
                if (biomeKey.equals(banned) || biomeKey.contains(banned)) return false;
            }

            // Pour les biomes inconnus (mods), vérifier le bloc de surface
            // (nécessite le chargement du chunk — seulement si le biome est inconnu)
            boolean isVanilla = biomeKey.length() > 0 && !biomeKey.contains(":");
            if (!isVanilla) {
                // Biome de mod : vérifier que la surface est herbe/terre/sable/gravier
                world.loadChunk(cx, cz, true);
                org.bukkit.Material surface = world.getHighestBlockAt(bx, bz).getType();
                String surfName = surface.name();
                return surfName.contains("GRASS") || surfName.contains("DIRT")
                    || surfName.contains("SAND") || surfName.contains("PODZOL")
                    || surfName.contains("MYCELIUM") || surfName.equals("STONE");
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean overlaps(List<ChunkCoord> proposed, Collection<Kingdom> existing) {
        for (Kingdom k : existing) {
            for (ChunkCoord coord : proposed) {
                if (k.hasChunk(coord)) return true;
            }
        }
        return false;
    }
}
