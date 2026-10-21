package ru.givler.caveabyss.config;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

/** Applies only when a saved overworld chunk has no lower layer yet. */
public final class MigrationConfig {
    public static final int PRESERVE = 0, BEDROCK_ONLY = 1, SAFE = 2, FULL = 3;
    public static int strength = SAFE;

    private MigrationConfig() { }

    public static void load(File file) {
        Configuration config = new Configuration(file);
        config.load();
        strength = config.getInt("strength", "migration", SAFE, PRESERVE, FULL,
                "0: preserve every block at Y>=0, including bedrock. "
                + "1: only replace old bedrock at Y=0..4 with stone. "
                + "2 (default): blend caves at Y=0..14 through ordinary stone and old bedrock only; "
                + "player-placed ordinary stone cannot be distinguished from natural stone. "
                + "3: blend through any blocks at Y=0..14, including ores, structures and containers. "
                + "Use 3 for worlds without player builds. All modes generate the missing lower layer. "
                + "Only first migration is affected; existing lower layers and new chunk generation are unchanged. "
                + "Restart the server/game after changing this setting.");
        if (config.hasChanged()) config.save();
    }
}
