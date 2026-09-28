package ru.givler.caveabyss.integration.bop;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraft.util.Direction;
import net.minecraft.util.Facing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.givler.caveabyss.config.DeepOreConfig;

/** Mirrors BOP 2.1.x gem-to-biome rules in CaveAbyss's negative terrain. */
public final class BopIntegration {
    private static final Logger LOG = LogManager.getLogger("CaveAbyss");
    private static final Map<String, Integer> BIOME_GEMS = new HashMap<String, Integer>();
    private static final Set<String> MOSS_BIOMES = new HashSet<String>();
    private static final boolean[] ENABLED = new boolean[7];
    private static BlockBopDeepslateOre deepOre;
    private static Block sourceOre;
    private static Block mushrooms;
    private static Block moss;

    static {
        // BOP's biome decorators use source metadata 2, 4, ... 14.
        biomes(0, "Brushland", "Canyon", "CanyonRavine", "LushDesert", "Oasis",
                "Outback", "Scrubland", "Steppe", "XericShrubland");
        biomes(1, "Chaparral", "FlowerField", "Garden", "Grassland", "Heathland",
                "LavenderFields", "Meadow", "MeadowForest", "Orchard", "Prairie", "Shrubland");
        biomes(2, "BambooForest", "CherryBlossomGrove", "EucalyptusForest", "MysticGrove",
                "Rainforest", "SacredSprings", "TropicalRainforest");
        biomes(3, "Arctic", "ConiferousForestSnow", "DeadForest", "FrostForest",
                "Glacier", "MapleWoods", "Tundra");
        biomes(4, "Bayou", "Bog", "DeadSwamp", "Fen", "LandOfLakes",
                "LandOfLakesMarsh", "LushSwamp", "Marsh", "Moor", "OminousWoods",
                "Quagmire", "Silkglades", "Sludgepit", "Wasteland", "Wetland");
        biomes(5, "CoralReef", "KelpForest", "Mangrove", "Tropics", "Volcano");
        biomes(6, "BorealForest", "ConiferousForest", "DeciduousForest", "DenseForest",
                "FungiForest", "Grove", "RedwoodForest", "SeasonalForest",
                "SeasonalForestClearing", "Shield", "SpruceWoods", "TemperateRainforest",
                "Thicket", "Woodland");
        for (String name : new String[] {"Bayou", "EucalyptusForest", "Fen", "Shield",
                "TemperateRainforest", "Wetland"}) MOSS_BIOMES.add("BiomeGen" + name);
    }

    private BopIntegration() { }

    private static void biomes(int gem, String... names) {
        for (String name : names) BIOME_GEMS.put("BiomeGen" + name, gem);
    }

    public static void preInit() {
        if (!Loader.isModLoaded("BiomesOPlenty")) return;
        deepOre = new BlockBopDeepslateOre();
        GameRegistry.registerBlock(deepOre, ItemBlockBopDeepslateOre.class, "deepslate_bop_ore");
        for (int gem = 0; gem < BlockBopDeepslateOre.GEMS.length; gem++)
            OreDictionary.registerOre("ore" + capitalize(BlockBopDeepslateOre.GEMS[gem]),
                    new ItemStack(deepOre, 1, gem));
    }

    private static String capitalize(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public static void postInit() {
        if (deepOre == null) return;
        sourceOre = GameRegistry.findBlock("BiomesOPlenty", "gemOre");
        mushrooms = GameRegistry.findBlock("BiomesOPlenty", "mushrooms");
        moss = GameRegistry.findBlock("BiomesOPlenty", "moss");
        if (sourceOre == null) {
            LOG.warn("BOP gemOre was not found; deep BOP gem generation is disabled");
            return;
        }
        try {
            Method enabled = Class.forName("biomesoplenty.api.utils.BiomeUtils")
                    .getMethod("oreWithMetaEnabled", int.class);
            for (int gem = 0; gem < ENABLED.length; gem++)
                ENABLED[gem] = (Boolean) enabled.invoke(null, (gem + 1) * 2);
        } catch (ReflectiveOperationException error) {
            LOG.warn("Could not read BOP gem generation settings; deep BOP gems are disabled", error);
        }
    }

    static Block sourceOre() {
        return sourceOre;
    }

    public static void generate(World world, Random random, int[] terrain, int deepslate,
                                int chunkX, int chunkZ) {
        if (deepOre == null) return;
        generatePlants(world, random, terrain, deepslate, chunkX, chunkZ);
        if (sourceOre == null) return;
        DeepOreConfig.Rule gems = DeepOreConfig.BOP[0];
        if (!DeepOreConfig.enabled(gems) || random.nextDouble() >= gems.chance) return;
        int stone = Block.getIdFromBlock(Blocks.stone);
        int deepId = Block.getIdFromBlock(deepOre);
        int normalId = Block.getIdFromBlock(sourceOre);
        // BOP attempts 12-17 singles above ground. The extra layer gets about
        // half that budget, keeping the total per chunk under control.
        int attempts = DeepOreConfig.attempts(gems, random);
        for (int n = 0; n < attempts; n++) {
            int x = random.nextInt(16), z = random.nextInt(16);
            int y = gems.minY + random.nextInt(gems.maxY - gems.minY + 1);
            BiomeGenBase biome = world.getWorldChunkManager().getBiomeGenAt(
                    (chunkX << 4) + x, (chunkZ << 4) + z);
            if (biome == null) continue;
            Integer gem = BIOME_GEMS.get(biome.getClass().getSimpleName());
            if (gem == null || !ENABLED[gem]) continue;
            for (int step = 0; step < gems.size; step++) {
                if (x >= 0 && x < 16 && z >= 0 && z < 16 && y >= -63 && y < 0) {
                    int index = ((y + 64) << 8) | (z << 4) | x;
                    if (terrain[index] == deepslate) terrain[index] = deepId | (gem << 16);
                    else if (terrain[index] == stone)
                        terrain[index] = normalId | (((gem + 1) * 2) << 16);
                }
                x += random.nextInt(3) - 1;
                y += random.nextInt(3) - 1;
                z += random.nextInt(3) - 1;
            }
        }
    }

    private static void generatePlants(World world, Random random, int[] terrain, int deepslate,
                                       int chunkX, int chunkZ) {
        String biome = world.getWorldChunkManager().getBiomeGenAt(
                (chunkX << 4) + 8, (chunkZ << 4) + 8).getClass().getSimpleName();
        int stone = Block.getIdFromBlock(Blocks.stone);
        DeepOreConfig.Rule mushroomRule = DeepOreConfig.BOP[1];
        if (mushrooms != null && "BiomeGenFungiForest".equals(biome)
                && DeepOreConfig.enabled(mushroomRule) && random.nextDouble() < mushroomRule.chance) {
            int placed = 0, mushroomId = Block.getIdFromBlock(mushrooms);
            int limit = DeepOreConfig.attempts(mushroomRule, random);
            for (int n = 0; n < limit * 24 && placed < limit; n++) {
                int x = 1 + random.nextInt(14), z = 1 + random.nextInt(14);
                int y = mushroomRule.minY + random.nextInt(mushroomRule.maxY - mushroomRule.minY + 1);
                int index = ((y + 64) << 8) | (z << 4) | x;
                int support = terrain[index - 256];
                if (terrain[index] == 0 && (support == stone || support == deepslate)) {
                    terrain[index] = mushroomId | (3 << 16);
                    placed++;
                }
            }
        }
        DeepOreConfig.Rule mossRule = DeepOreConfig.BOP[2];
        if (moss != null && MOSS_BIOMES.contains(biome)
                && DeepOreConfig.enabled(mossRule) && random.nextDouble() < mossRule.chance) {
            int placed = 0, mossId = Block.getIdFromBlock(moss);
            int limit = DeepOreConfig.attempts(mossRule, random);
            for (int n = 0; n < limit * 14 && placed < limit; n++) {
                int x = 1 + random.nextInt(14), z = 1 + random.nextInt(14);
                int y = mossRule.minY + random.nextInt(mossRule.maxY - mossRule.minY + 1);
                int index = ((y + 64) << 8) | (z << 4) | x;
                if (terrain[index] != 0) continue;
                int side = 2 + random.nextInt(4);
                int neighbor = index + (side == 2 ? 16 : side == 3 ? -16 : side == 4 ? 1 : -1);
                int support = terrain[neighbor];
                if (support == stone || support == deepslate) {
                    int metadata = 1 << Direction.facingToDirection[Facing.oppositeSide[side]];
                    terrain[index] = mossId | (metadata << 16);
                    placed++;
                }
            }
        }
    }
}
