package ru.givler.caveabyss.integration.mbo;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import ru.givler.caveabyss.world.DeepWorldGenerator;
import ru.givler.caveabyss.block.CaveBlocks;

/** Optional registry-only bridge: CaveAbyss also runs without MBO. */
public final class MboIntegration {
    private static Block tuff;
    private static final int[] DX = {0, 0, 0, 0, -1, 1};
    private static final int[] DY = {-1, 1, 0, 0, 0, 0};
    private static final int[] DZ = {0, 0, -1, 1, 0, 0};

    private MboIntegration() { }

    public static void postInit() {
        tuff = Loader.isModLoaded("mbo") ? GameRegistry.findBlock("mbo", "BlockTuff") : null;
    }

    private static boolean supportsCrystal(int[] terrain, int x, int y, int z) {
        Block cluster = CaveBlocks.aquamarineBuds[3];
        if (cluster == null) return false;
        int crystal = Block.getIdFromBlock(cluster);
        for (int side = 0; side < 6; side++) {
            int nx = x + DX[side], ny = y + DY[side], nz = z + DZ[side];
            if (nx < 0 || nx >= 16 || nz < 0 || nz >= 16 || ny < -64 || ny >= 0) continue;
            if ((terrain[((ny + 64) << 8) | (nz << 4) | nx] & 65535) == crystal) return true;
        }
        return false;
    }

    public static void generate(long seed, int chunkX, int chunkZ, int[] terrain,
                                int stone, int deepslate) {
        if (tuff == null) return;
        int id = Block.getIdFromBlock(tuff);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int wx = (chunkX << 4) + x, wz = (chunkZ << 4) + z;
            double coverage = DeepWorldGenerator.noise(seed ^ 0x7AFF1L, wx / 48D, 0, wz / 48D);
            if (coverage < .57D) continue;
            double center = -47 + 18 * DeepWorldGenerator.noise(seed ^ 0x7AFF2L, wx / 32D, 0, wz / 32D);
            double thickness = 3 + 5 * DeepWorldGenerator.noise(seed ^ 0x7AFF3L, wx / 16D, 0, wz / 16D);
            for (int y = -55; y <= -22; y++) {
                if (Math.abs(y - center) > thickness / 2) continue;
                int index = ((y + 64) << 8) | (z << 4) | x;
                if ((terrain[index] == stone || terrain[index] == deepslate)
                        && !supportsCrystal(terrain, x, y, z)) terrain[index] = id;
            }
        }
    }
}
