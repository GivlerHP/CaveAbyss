package ru.givler.caveabyss.integration.mf2;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.givler.caveabyss.config.DeepOreConfig;

/** Places MF2 ores into independently configured negative-layer bands. */
public final class Mf2Integration {
    private static final Logger LOG = LogManager.getLogger("CaveAbyss");
    private static final int NONE = 0, AIR = 1, BEDROCK = 2;
    private static final Ore[] ORES = {
            new Ore("oreCopper", "copper", 0, 2.0F, 3.0F, NONE),
            new Ore("oreTin", "tin", 0, 2.5F, 4.0F, NONE),
            new Ore("oreSilver", "silver", 2, 3.0F, 5.0F, NONE),
            new Ore("oreTungsten", "tungsten", 3, 4.0F, 2.5F, NONE),
            new Ore("oreNitre", "nitre", 2, 3.0F, 5.0F, AIR),
            new Ore("oreSulfur", "sulfur", 2, 3.0F, 2.0F, NONE),
            new Ore("oreMythic", "mythic", 4, 10.0F, 100.0F, BEDROCK)
    };
    private static boolean present;

    private Mf2Integration() { }

    public static void preInit() {
        present = Loader.isModLoaded("minefantasy2");
        if (!present) return;
        for (Ore ore : ORES) {
            ore.deep = new BlockMf2DeepslateOre(ore.name, ore.texture,
                    ore.harvestLevel, ore.hardness, ore.resistance);
            GameRegistry.registerBlock(ore.deep, "deepslate_mf2_" + ore.texture + "_ore");
            OreDictionary.registerOre(ore.name, new ItemStack(ore.deep));
        }
    }

    public static void postInit() {
        if (!present) return;
        for (Ore ore : ORES) {
            ore.source = GameRegistry.findBlock("minefantasy2", ore.name);
            if (ore.source == null)
                LOG.warn("MF2 block {} was not found; its deep ore generation is disabled", ore.name);
        }
    }

    static Block sourceOre(String name) {
        for (Ore ore : ORES) if (ore.name.equals(name)) return ore.source;
        return null;
    }

    public static void generate(World world, Random random, int[] terrain, int deepslate) {
        if (!present) return;
        int stone = Block.getIdFromBlock(Blocks.stone);
        int bedrock = Block.getIdFromBlock(Blocks.bedrock);
        for (int oreIndex = 0; oreIndex < ORES.length; oreIndex++) {
            Ore ore = ORES[oreIndex];
            if (ore.source == null) continue;
            DeepOreConfig.Rule rule = DeepOreConfig.MF2[oreIndex];
            if (!DeepOreConfig.enabled(rule) || random.nextDouble() >= rule.chance) continue;
            int count = DeepOreConfig.attempts(rule, random);
            int normalId = Block.getIdFromBlock(ore.source);
            int deepId = Block.getIdFromBlock(ore.deep);
            for (int n = 0; n < count; n++) {
                int x = random.nextInt(16), z = random.nextInt(16);
                int y = rule.minY + random.nextInt(rule.maxY - rule.minY + 1);
                placeVein(random, terrain, stone, deepslate, bedrock, normalId, deepId,
                        ore.condition, rule.size, x, y, z);
            }
        }
    }

    private static void placeVein(Random random, int[] terrain, int stone, int deepslate,
                                  int bedrock, int normalId, int deepId, int condition,
                                  int size, int x, int y, int z) {
        for (int i = 0; i < size; i++) {
            if (x >= 0 && x < 16 && z >= 0 && z < 16 && y >= -63 && y < 0) {
                int index = ((y + 64) << 8) | (z << 4) | x;
                int host = terrain[index];
                if ((host == stone || host == deepslate)
                        && (condition == NONE || adjacent(terrain, x, y, z,
                                condition == BEDROCK ? bedrock : 0))) {
                    terrain[index] = host == stone ? normalId : deepId;
                }
            }
            x += random.nextInt(3) - 1;
            y += random.nextInt(3) - 1;
            z += random.nextInt(3) - 1;
        }
    }

    static boolean adjacent(int[] terrain, int x, int y, int z, int target) {
        int index = ((y + 64) << 8) | (z << 4) | x;
        return (y > -64 && terrain[index - 256] == target)
                || (y < -1 && terrain[index + 256] == target)
                || (x > 0 && terrain[index - 1] == target)
                || (x < 15 && terrain[index + 1] == target)
                || (z > 0 && terrain[index - 16] == target)
                || (z < 15 && terrain[index + 16] == target);
    }

    private static final class Ore {
        final String name, texture;
        final int harvestLevel, condition;
        final float hardness, resistance;
        BlockMf2DeepslateOre deep;
        Block source;

        Ore(String name, String texture, int harvestLevel,
            float hardness, float resistance, int condition) {
            this.name = name;
            this.texture = texture;
            this.harvestLevel = harvestLevel;
            this.hardness = hardness;
            this.resistance = resistance;
            this.condition = condition;
        }
    }
}
