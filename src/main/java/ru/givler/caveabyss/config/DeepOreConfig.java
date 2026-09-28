package ru.givler.caveabyss.config;

import java.io.File;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/** Generation settings for the negative layer; Y values are absolute world coordinates. */
public final class DeepOreConfig {
    private static Configuration config;
    public static final Rule[] VANILLA = new Rule[7];
    public static final Rule[] THAUMCRAFT = new Rule[3];
    public static final Rule[] BOP = new Rule[3];
    public static final Rule[] MF2 = new Rule[7];
    private static final int OLD_INHERIT = -999;

    private DeepOreConfig() { }

    public static void load(File file) {
        config = new Configuration(file);
        config.load();
        VANILLA[0] = rule("vanilla.coal", 5, 9, -62, -1, 1.0);
        VANILLA[1] = rule("vanilla.iron", 20, 8, -62, -1, 1.0);
        VANILLA[2] = rule("vanilla.gold", 2, 8, -62, -26, 1.0);
        VANILLA[3] = rule("vanilla.redstone", 8, 7, -62, -24, 1.0);
        VANILLA[4] = rule("vanilla.lapis", 1, 6, -62, -16, 1.0);
        VANILLA[5] = rule("vanilla.diamond", 1, 5, -62, -38, 0.25);
        VANILLA[6] = rule("vanilla.emerald", 1, 1, -16, -1, 1.0 / 12.0);
        THAUMCRAFT[0] = rule("thaumcraft.cinnabar", 4, 1, -62, -1, 1.0);
        THAUMCRAFT[1] = rule("thaumcraft.infused", 2, 6, -62, -1, 1.0);
        THAUMCRAFT[2] = rule("thaumcraft.amber", 1, 1, -22, -1, 1.0);
        BOP[0] = rule("bop.gems", 6, 1, -32, -1, 1.0);
        BOP[1] = rule("bop.mushrooms", 4, 1, -60, -3, 1.0);
        BOP[2] = rule("bop.moss", 6, 1, -60, -3, 1.0);
        // Separate lower-layer distribution: copper/tin near Y=0, rare mythic at bedrock.
        MF2[0] = rule("mf2.copper", 4, 4, 8, -24, -1, 1.0);
        MF2[1] = rule("mf2.tin", 4, 4, 5, -24, -1, 1.0);
        MF2[2] = rule("mf2.silver", 2, 3, 8, -48, -16, 1.0);
        MF2[3] = rule("mf2.tungsten", 1, 1, 7, -60, -36, 1.0);
        MF2[4] = rule("mf2.nitre", 2, 4, 8, -40, -8, 1.0);
        MF2[5] = rule("mf2.sulfur", 3, 6, 4, -60, -32, 1.0);
        MF2[6] = rule("mf2.mythic", 1, 2, 6, -63, -58, 0.05);
        config.save();
    }

    private static Rule rule(String category, int count, int size, int minY, int maxY, double chance) {
        return rule(category, count, category.equals("bop.gems") ? 8 : count,
                size, minY, maxY, chance);
    }

    private static Rule rule(String category, int count, int maxCount, int size,
                             int minY, int maxY, double chance) {
        Rule result = new Rule();
        result.count = integer(category, "veinsPerChunk", count, "Minimum placement attempts per chunk.");
        result.maxCount = integer(category, "maxVeinsPerChunk", maxCount, "Maximum placement attempts per chunk.");
        result.size = integer(category, "veinSize", size, "Maximum blocks per vein.");
        result.minY = integer(category, "minY", minY, "Lowest absolute Y.");
        result.maxY = integer(category, "maxY", maxY, "Highest absolute Y.");
        Property chanceProperty = config.get(category, "chunkChance", chance,
                "Chance to run generation in a chunk, 0..1.");
        result.chance = chanceProperty.getDouble(chance);
        if (category.startsWith("mf2.") && result.chance == OLD_INHERIT) {
            chanceProperty.set(chance);
            result.chance = chance;
        }
        return result;
    }

    private static int integer(String category, String name, int defaultValue, String comment) {
        Property property = config.get(category, name, defaultValue, comment);
        int value = property.getInt(defaultValue);
        if (category.startsWith("mf2.") && value == OLD_INHERIT) {
            property.set(defaultValue);
            return defaultValue;
        }
        return value;
    }

    public static boolean enabled(Rule rule) {
        return rule != null && rule.count >= 0 && rule.maxCount > 0 && rule.maxCount >= rule.count
                && rule.size > 0 && rule.minY >= -63
                && rule.maxY <= -1 && rule.minY <= rule.maxY && rule.chance > 0;
    }

    public static int attempts(Rule rule, java.util.Random random) {
        return rule.count + random.nextInt(rule.maxCount - rule.count + 1);
    }

    public static final class Rule {
        public int count, maxCount, size, minY, maxY;
        public double chance;
    }
}
