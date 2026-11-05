package ru.givler.caveabyss.config;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

public final class DeepOreConfigSmoke {
    public static void check() throws Exception {
        java.lang.reflect.Field home = cpw.mods.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true); home.set(null, new File("."));
        java.lang.reflect.Field side = cpw.mods.fml.relauncher.FMLRelaunchLog.class.getDeclaredField("side");
        side.setAccessible(true); side.set(null, cpw.mods.fml.relauncher.Side.SERVER);
        File folder = new File("build/ore-config-smoke"); folder.mkdirs();
        File fresh = File.createTempFile("fresh-", ".cfg", folder);
        DeepOreConfig.load(fresh);
        DeepOreConfig.Rule diamond = DeepOreConfig.VANILLA[5], deralium = DeepOreConfig.MF2[7];
        if (diamond.count != deralium.count || diamond.size != deralium.size || diamond.chance != deralium.chance
                || deralium.minY != -62 || deralium.maxY != -33)
            throw new AssertionError("Deralium must match deep diamond abundance without moving upward");
        File old = File.createTempFile("legacy-", ".cfg", folder);
        preset(old, 2, .08);
        DeepOreConfig.load(old);
        if (DeepOreConfig.MF2[7].size != 5 || DeepOreConfig.MF2[7].chance != .25)
            throw new AssertionError("Old deralium defaults were not upgraded");
        DeepOreConfig.load(old);
        if (DeepOreConfig.MF2[7].size != 5 || DeepOreConfig.MF2[7].chance != .25)
            throw new AssertionError("Deralium migration did not persist");
        File custom = File.createTempFile("custom-", ".cfg", folder);
        preset(custom, 7, .4);
        DeepOreConfig.load(custom);
        if (DeepOreConfig.MF2[7].size != 7 || DeepOreConfig.MF2[7].chance != .4)
            throw new AssertionError("Custom ore settings were overwritten");
        System.out.println("Deep ores: diamond-like deralium defaults, unchanged depth, migration and custom settings PASS");
    }

    private static void preset(File file, int size, double chance) {
        Configuration config = new Configuration(file);
        config.get("mf2.deralium", "veinsPerChunk", 1).set(1);
        config.get("mf2.deralium", "maxVeinsPerChunk", 1).set(1);
        config.get("mf2.deralium", "veinSize", size).set(size);
        config.get("mf2.deralium", "minY", -62).set(-62);
        config.get("mf2.deralium", "maxY", -33).set(-33);
        config.get("mf2.deralium", "chunkChance", chance).set(chance);
        config.save();
    }
}
