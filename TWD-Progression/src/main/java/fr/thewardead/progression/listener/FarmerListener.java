package fr.thewardead.progression.listener;

import fr.thewardead.progression.ProgressionPlugin;
import fr.thewardead.progression.level.FermierLevel;
import fr.thewardead.progression.model.JobClass;
import fr.thewardead.progression.model.ProgressionPlayer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class FarmerListener implements Listener {

    // Cultures matures donnant de l'XP
    private static final Map<Material, Integer> CROP_XP = new EnumMap<>(Material.class);

    static {
        CROP_XP.put(Material.WHEAT,            3);
        CROP_XP.put(Material.CARROTS,          3);
        CROP_XP.put(Material.POTATOES,         3);
        CROP_XP.put(Material.BEETROOTS,        3);
        CROP_XP.put(Material.NETHER_WART,      3);
        CROP_XP.put(Material.SWEET_BERRY_BUSH, 4);
        CROP_XP.put(Material.COCOA,            4);
        // Blocs complets (pas Ageable) :
        CROP_XP.put(Material.MELON,            5);
        CROP_XP.put(Material.PUMPKIN,          5);
        CROP_XP.put(Material.SUGAR_CANE,       2);
        CROP_XP.put(Material.CACTUS,           2);
    }

    // Animaux de ferme tués par le fermier
    private static final Set<Class<? extends Entity>> LIVESTOCK = Set.of(
        Cow.class, Pig.class, Sheep.class, Chicken.class, Rabbit.class, MushroomCow.class
    );

    private static final Set<Material> AGEABLE_CROPS = Set.of(
        Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
        Material.NETHER_WART, Material.SWEET_BERRY_BUSH, Material.COCOA
    );

    private final ProgressionPlugin plugin;
    private final Random rng = new Random();

    public FarmerListener(ProgressionPlugin plugin) {
        this.plugin = plugin;
    }

    // ── XP récolte ───────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player p = (Player) (event.getPlayer() instanceof Player ? event.getPlayer() : null);
        if (p == null) return;
        ProgressionPlayer pp = plugin.getProgressionManager().get(p.getUniqueId());
        if (pp == null || pp.getJobClass() != JobClass.FERMIER) return;

        Block block = event.getBlock();
        int xp = cropXp(block);
        if (xp > 0) plugin.getProgressionManager().addXp(p.getUniqueId(), xp);
    }

    // ── Bonus drops récolte (BlockDropItemEvent) ──────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(BlockDropItemEvent event) {
        Player p = event.getPlayer();
        ProgressionPlayer pp = plugin.getProgressionManager().get(p.getUniqueId());
        if (pp == null || pp.getJobClass() != JobClass.FERMIER) return;
        if (!CROP_XP.containsKey(event.getBlock().getType())) return;
        if (!isMatureCrop(event.getBlock())) return;

        FermierLevel lv = plugin.getProgressionManager().getFermierLevel(pp);

        // Double récolte complète
        if (lv.getDoubleHarvestChance() > 0 && rng.nextDouble() < lv.getDoubleHarvestChance()) {
            event.getItems().forEach(item ->
                item.getItemStack().setAmount(item.getItemStack().getAmount() * 2));
            return; // déjà doublé, pas besoin de l'extra drop
        }

        // Extra drop probabiliste sur chaque item
        if (lv.getExtraDropChance() > 0) {
            List<Item> extras = new ArrayList<>();
            for (Item item : event.getItems()) {
                ItemStack stack = item.getItemStack();
                int extra = 0;
                for (int i = 0; i < stack.getAmount(); i++) {
                    if (rng.nextDouble() < lv.getExtraDropChance()) extra++;
                }
                if (extra > 0) {
                    ItemStack bonus = stack.clone();
                    bonus.setAmount(extra);
                    Item entity = event.getBlock().getWorld()
                        .dropItemNaturally(event.getBlock().getLocation().add(0.5, 0.5, 0.5), bonus);
                    extras.add(entity);
                }
            }
            event.getItems().addAll(extras);
        }

        // Wars passifs (hauts niveaux)
        if (lv.getWarsPerHarvest() > 0) {
            plugin.getProgressionManager().giveWars(p.getUniqueId(), lv.getWarsPerHarvest());
        }
    }

    // ── XP élevage ───────────────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player p)) return;
        ProgressionPlayer pp = plugin.getProgressionManager().get(p.getUniqueId());
        if (pp == null || pp.getJobClass() != JobClass.FERMIER) return;
        plugin.getProgressionManager().addXp(p.getUniqueId(), 15);
    }

    // ── XP + bonus drops animaux ──────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onAnimalDeath(EntityDeathEvent event) {
        Player p = event.getEntity().getKiller();
        if (p == null) return;
        ProgressionPlayer pp = plugin.getProgressionManager().get(p.getUniqueId());
        if (pp == null || pp.getJobClass() != JobClass.FERMIER) return;
        if (LIVESTOCK.stream().noneMatch(c -> c.isInstance(event.getEntity()))) return;

        plugin.getProgressionManager().addXp(p.getUniqueId(), 4);

        // Bonus drops animaux à partir du niveau 5 (Éleveur)
        FermierLevel lv = plugin.getProgressionManager().getFermierLevel(pp);
        if (lv.getNum() >= 5) {
            List<ItemStack> extras = new ArrayList<>();
            for (ItemStack drop : event.getDrops()) {
                if (rng.nextDouble() < 0.30) { // +30% extra drop animal
                    ItemStack bonus = drop.clone();
                    bonus.setAmount(1);
                    extras.add(bonus);
                }
            }
            event.getDrops().addAll(extras);
        }
    }

    // ── XP + bonus pêche ──────────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player p = event.getPlayer();
        ProgressionPlayer pp = plugin.getProgressionManager().get(p.getUniqueId());
        if (pp == null || pp.getJobClass() != JobClass.FERMIER) return;

        plugin.getProgressionManager().addXp(p.getUniqueId(), 10);

        // Bonus drops pêche (à partir du niveau 4, Herboriste)
        FermierLevel lv = plugin.getProgressionManager().getFermierLevel(pp);
        if (lv.getFishingMultiplier() > 0 && event.getCaught() instanceof Item caught) {
            if (rng.nextDouble() < lv.getFishingMultiplier()) {
                ItemStack bonus = caught.getItemStack().clone();
                bonus.setAmount(caught.getItemStack().getAmount());
                caught.getWorld().dropItemNaturally(caught.getLocation(), bonus);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int cropXp(Block block) {
        Integer base = CROP_XP.get(block.getType());
        if (base == null) return 0;
        if (AGEABLE_CROPS.contains(block.getType()) && !isMaxAge(block)) return 0;
        return base;
    }

    private boolean isMatureCrop(Block block) {
        if (!AGEABLE_CROPS.contains(block.getType())) return true; // melon/citrouille/canne = toujours mature
        return isMaxAge(block);
    }

    private boolean isMaxAge(Block block) {
        if (!(block.getBlockData() instanceof Ageable a)) return false;
        return a.getAge() == a.getMaximumAge();
    }
}
