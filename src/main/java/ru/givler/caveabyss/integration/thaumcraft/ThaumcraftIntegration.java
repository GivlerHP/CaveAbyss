package ru.givler.caveabyss.integration.thaumcraft;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import java.lang.reflect.Method;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Optional Thaumcraft integration without linking to its classes. */
public final class ThaumcraftIntegration {
    private static final Logger LOG = LogManager.getLogger("CaveAbyss");
    private static final String[] ORE_NAMES = {
            "oreCinnabar", "oreInfusedAir", "oreInfusedFire", "oreInfusedWater",
            "oreInfusedEarth", "oreInfusedOrder", "oreInfusedEntropy", "oreAmber"
    };
    private static Block deepOre;
    private static Block sourceOre;
    private static Method biomeBlacklist;
    private static boolean cinnabarEnabled = true;
    private static boolean infusedEnabled = true;
    private static boolean amberEnabled = true;

    private ThaumcraftIntegration() { }

    public static boolean isEnabled() {
        return deepOre != null;
    }

    public static void preInit() {
        if (!Loader.isModLoaded("Thaumcraft")) return;
        deepOre = new BlockThaumcraftDeepslateOre();
        GameRegistry.registerBlock(deepOre, ItemBlockThaumcraftDeepslateOre.class,
                "deepslate_thaumcraft_ore");
        for (int metadata = 0; metadata < ORE_NAMES.length; metadata++)
            OreDictionary.registerOre(ORE_NAMES[metadata], new ItemStack(deepOre, 1, metadata));
    }

    public static void postInit() {
        if (deepOre == null) return;
        sourceOre = GameRegistry.findBlock("Thaumcraft", "blockCustomOre");
        if (sourceOre == null) {
            LOG.warn("Thaumcraft ore block was not found; deep Thaumcraft ore generation is disabled");
            return;
        }
        try {
            Class<?> config = Class.forName("thaumcraft.common.config.Config");
            cinnabarEnabled = config.getField("genCinnibar").getBoolean(null);
            infusedEnabled = config.getField("genInfusedStone").getBoolean(null);
            amberEnabled = config.getField("genAmber").getBoolean(null);
            biomeBlacklist = Class.forName("thaumcraft.common.lib.world.ThaumcraftWorldGenerator")
                    .getMethod("getBiomeBlacklist", int.class);
        } catch (ReflectiveOperationException error) {
            LOG.warn("Could not read Thaumcraft ore generation settings; using CaveAbyss defaults", error);
        }
    }

    public static Block sourceOre() {
        return sourceOre;
    }

    public static void generate(World world, Random random, int[] terrain, int deepslate,
                                int chunkX, int chunkZ) {
        if (deepOre == null || sourceOre == null || !biomeAllowsOres(world, chunkX, chunkZ)) return;
        int stone = Block.getIdFromBlock(Blocks.stone);
        int deepId = Block.getIdFromBlock(deepOre);
        int normalId = Block.getIdFromBlock(sourceOre);
        if (cinnabarEnabled)
            for (int n = 0; n < 4; n++)
                placeSingle(random, terrain, stone, deepslate, normalId, deepId, 0, -62, -1);
        if (infusedEnabled)
            for (int n = 0; n < 2; n++)
                placeVein(random, terrain, stone, deepslate, normalId, deepId,
                        1 + random.nextInt(6), 6, -62, -1);
        if (amberEnabled)
            placeSingle(random, terrain, stone, deepslate, normalId, deepId, 7, -22, -1);
    }

    private static boolean biomeAllowsOres(World world, int chunkX, int chunkZ) {
        if (biomeBlacklist == null) return true;
        try {
            int biome = world.getBiomeGenForCoords((chunkX << 4) + 8, (chunkZ << 4) + 8).biomeID;
            int level = (Integer) biomeBlacklist.invoke(null, biome);
            return level != 0 && level != 2;
        } catch (ReflectiveOperationException error) {
            LOG.warn("Could not check Thaumcraft biome blacklist", error);
            biomeBlacklist = null;
            return true;
        }
    }

    private static void placeSingle(Random random, int[] terrain, int stone, int deepslate,
                                    int normalId, int deepId, int metadata, int minY, int maxY) {
        int x = random.nextInt(16), z = random.nextInt(16);
        int y = minY + random.nextInt(maxY - minY + 1);
        int index = ((y + 64) << 8) | (z << 4) | x;
        if (terrain[index] == deepslate) terrain[index] = deepId | (metadata << 16);
        else if (terrain[index] == stone) terrain[index] = normalId | (metadata << 16);
    }

    private static void placeVein(Random random, int[] terrain, int stone, int deepslate,
                                  int normalId, int deepId, int metadata, int size, int minY, int maxY) {
        int x = random.nextInt(16), z = random.nextInt(16);
        int y = minY + random.nextInt(maxY - minY + 1);
        for (int n = 0; n < size; n++) {
            if (x >= 0 && x < 16 && z >= 0 && z < 16 && y >= -63 && y < 0) {
                int index = ((y + 64) << 8) | (z << 4) | x;
                if (terrain[index] == deepslate)
                    terrain[index] = deepId | (metadata << 16);
                else if (terrain[index] == stone)
                    terrain[index] = normalId | (metadata << 16);
            }
            x += random.nextInt(3) - 1;
            y += random.nextInt(3) - 1;
            z += random.nextInt(3) - 1;
        }
    }
}
