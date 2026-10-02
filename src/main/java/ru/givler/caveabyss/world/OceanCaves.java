package ru.givler.caveabyss.world;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import ru.givler.caveabyss.block.CaveBlocks;

/** Water-filled lower-layer caves under ocean biomes. All decisions use world coordinates. */
public final class OceanCaves {
    private OceanCaves() { }

    public static void generate(World world, long seed, int chunkX, int chunkZ,
                                int[] terrain, Block[] upper) {
        int stone = Block.getIdFromBlock(Blocks.stone);
        int deep = Block.getIdFromBlock(CaveBlocks.deepslate);
        int water = Block.getIdFromBlock(Blocks.water);
        int magma = Block.getIdFromBlock(CaveBlocks.magma);
        int grass = Block.getIdFromBlock(CaveBlocks.seagrass);
        int tall = Block.getIdFromBlock(CaveBlocks.tallSeagrass);
        int kelp = Block.getIdFromBlock(CaveBlocks.kelp);
        int crystal = CaveBlocks.aquamarineBuds[3] == null ? 0
                : Block.getIdFromBlock(CaveBlocks.aquamarineBuds[3]);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int wx = (chunkX << 4) + x, wz = (chunkZ << 4) + z;
            if (!DeepWorldGenerator.isOcean(world.getBiomeGenForCoords(wx, wz))) continue;
            for (int y = -51; y <= 14; y++) {
                if (y >= 0 && upper == null) break;
                int upperIndex = ((x * 16 + z) << 8) | y;
                if (y >= 0 && upper[upperIndex] != Blocks.stone) continue;
                if (y == 0 && terrain[index(x, -1, z)] != water) continue;
                if (y > 0 && upper[upperIndex - 1] != Blocks.water) continue;
                int i = y < 0 ? index(x, y, z) : -1;
                double broad = noise(seed, wx / 23.0, y / 16.0, wz / 23.0);
                double detail = noise(seed ^ 0xA510C41L, wx / 9.0, y / 8.0, wz / 9.0);
                double threshold = 0.60 + Math.max(0, y + 8) * 0.006;
                if (y >= -48 && (y >= 0 || terrain[i] == stone || terrain[i] == deep)
                        && (y == 0 || broad * 0.72 + detail * 0.28 > threshold)) {
                    if (y < 0) terrain[i] = water;
                    else upper[upperIndex] = Blocks.water;
                }
                if (y < 0 && terrain[i] == 0) terrain[i] = water;
            }
        }
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int wx = (chunkX << 4) + x, wz = (chunkZ << 4) + z;
            if (!DeepWorldGenerator.isOcean(world.getBiomeGenForCoords(wx, wz))) continue;
            for (int y = -50; y <= -9; y++) {
                int i = index(x, y, z);
                if (terrain[i] != deep || terrain[i + 256] != water) continue;
                long h = hash(seed ^ 0x0CEA6L, wx, y, wz);
                if (h % 37 == 0) {
                    terrain[i] = magma;
                } else if (h % 13 == 0) {
                    int above = i + 256;
                    if (h % 5 == 0 && y < -10 && terrain[above + 256] == water) {
                        terrain[above] = tall | (8 << 16);
                        terrain[above + 256] = tall | (9 << 16);
                    } else if (h % 3 == 0 && y < -12 && terrain[above + 256] == water) {
                        int height = 2 + (int)(h % 5);
                        int top = 0;
                        for (int n = 1; n <= height && y + n < 0
                                && terrain[i + n * 256] == water; n++) {
                            terrain[i + n * 256] = kelp | (10 << 16);
                            top = n;
                        }
                        if (top > 0) terrain[i + top * 256] = kelp | ((8 + (int)(h & 1)) << 16);
                    } else terrain[above] = grass | ((8 + (int)(h % 3)) << 16);
                } else if (crystal != 0 && h % 503 == 0) {
                    terrain[i + 256] = crystal | (1 << 16);
                }
            }
        }
    }

    public static void fillCavities(World world, int chunkX, int chunkZ, int[] terrain) {
        int water = Block.getIdFromBlock(Blocks.water);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            if (!DeepWorldGenerator.isOcean(world.getBiomeGenForCoords((chunkX << 4) + x,
                    (chunkZ << 4) + z))) continue;
            for (int y = -51; y < 0; y++) {
                int i = index(x, y, z);
                if (terrain[i] == 0) terrain[i] = water;
            }
        }
    }

    private static int index(int x, int y, int z) {
        return ((y + 64) << 8) | (z << 4) | x;
    }

    private static double noise(long seed, double x, double y, double z) {
        int ix = (int)Math.floor(x), iy = (int)Math.floor(y), iz = (int)Math.floor(z);
        double fx = fade(x - ix), fy = fade(y - iy), fz = fade(z - iz), result = 0;
        for (int dx = 0; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++)
            for (int dz = 0; dz <= 1; dz++)
                result += (dx == 0 ? 1 - fx : fx) * (dy == 0 ? 1 - fy : fy)
                        * (dz == 0 ? 1 - fz : fz)
                        * ((hash(seed, ix + dx, iy + dy, iz + dz) >>> 10) * 0x1.0p-53);
        return result;
    }

    private static double fade(double value) { return value * value * (3 - 2 * value); }

    private static long hash(long seed, int x, int y, int z) {
        long h = seed ^ (x * 0x632BE59BD9B4E019L) ^ (y * 0x9E3779B97F4A7C15L)
                ^ (z * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 30; h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27; h *= 0x94D049BB133111EBL;
        return (h ^ h >>> 31) & Long.MAX_VALUE;
    }
}
