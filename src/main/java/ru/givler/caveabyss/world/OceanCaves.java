package ru.givler.caveabyss.world;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import ru.givler.caveabyss.block.CaveBlocks;

/** Water-filled lower-layer caves under ocean biomes. All decisions use world coordinates. */
public final class OceanCaves {
    private OceanCaves() { }

    /** Shared generation/fauna contract; exact BOP names avoid treating every beach as an ocean. */
    public static boolean isOceanBiome(BiomeGenBase biome) {
        if (biome == null) return false;
        if (biome == BiomeGenBase.ocean || biome == BiomeGenBase.deepOcean || biome == BiomeGenBase.frozenOcean) return true;
        String name = biome.biomeName;
        return name.equalsIgnoreCase("Ocean") || name.equalsIgnoreCase("Deep Ocean")
                || name.equalsIgnoreCase("FrozenOcean") || name.equalsIgnoreCase("Coral Reef")
                || name.equalsIgnoreCase("Kelp Forest");
    }

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
            boolean wet = flooded(seed, wx, wz);
            for (int y = -51; y <= 14; y++) {
                if (y >= 0 && (upper == null || !wet)) break;
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
                if (y < 0 && terrain[i] == 0 && wet) terrain[i] = water;
            }
        }
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int wx = (chunkX << 4) + x, wz = (chunkZ << 4) + z;
            if (!DeepWorldGenerator.isOcean(world.getBiomeGenForCoords(wx, wz))) continue;
            for (int y = -50; y <= -9; y++) {
                int i = index(x, y, z);
                if (terrain[i] != deep || terrain[i + 256] != water) continue;
                long h = hash(seed ^ 0x0CEA6L, wx, y, wz);
                if (magmaPatch(seed, wx, y, wz)) {
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
                }
            }
        }
        if (crystal != 0) {
            int[] dx = {0, 0, 0, 0, -1, 1};
            int[] dy = {-1, 1, 0, 0, 0, 0};
            int[] dz = {0, 0, -1, 1, 0, 0};
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++)
                for (int y = -50; y <= -9; y++) {
                    int i = index(x, y, z);
                    if (terrain[i] != water) continue;
                    int wx = (chunkX << 4) + x, wz = (chunkZ << 4) + z;
                    if (hash(seed ^ 0xAC4A1L, wx, y, wz) % 503 != 0) continue;
                    for (int side = 0; side < 6; side++) {
                        int sx = x - dx[side], sy = y - dy[side], sz = z - dz[side];
                        if (sx < 0 || sx >= 16 || sz < 0 || sz >= 16) continue;
                        if (terrain[index(sx, sy, sz)] == deep) {
                            terrain[i] = crystal | (side << 16);
                            break;
                        }
                    }
                }
        }
    }

    public static boolean flooded(long seed, int x, int z) {
        return noise(seed ^ 0xF100DL, x / 64D, 0, z / 64D) > .53D;
    }

    public static boolean magmaPatch(long seed, int x, int y, int z) {
        return noise(seed ^ 0xBA6A1L, x / 6D, y / 8D, z / 6D) > .91D
                && hash(seed ^ 0xBA6A2L, x, y, z) % 5 != 0;
    }

    public static void fillCavities(World world, int chunkX, int chunkZ, int[] terrain) {
        boolean[] ocean = new boolean[256];
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++)
            ocean[(z << 4) | x] = isOceanBiome(world.getBiomeGenForCoords(
                    (chunkX << 4) + x, (chunkZ << 4) + z));
        fillTerrain(world.getSeed(), chunkX, chunkZ, terrain, ocean,
                Block.getIdFromBlock(Blocks.water), Block.getIdFromBlock(CaveBlocks.deepslate));
    }

    /** Broad noise-carved chambers are already water. Only narrow vanilla passages may stay dry. */
    static void fillTerrain(long seed, int chunkX, int chunkZ, int[] terrain,
                            boolean[] ocean, int water, int deep) {
        int[] original = terrain.clone();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            if (!ocean[(z << 4) | x]) continue;
            int wx = (chunkX << 4) + x, wz = (chunkZ << 4) + z;
            double region = noise(seed ^ 0xF100DL, wx / 64D, 0, wz / 64D);
            for (int y = -51; y < 0; y++) {
                int i = index(x, y, z);
                // Never drain a large chamber or replace its water with a regional barrier.
                if (original[i] != 0) continue;
                terrain[i] = roomy(original, x, y, z, water) || region > .53D ? water
                        : region > .48D ? deep : 0;
            }
        }
        // A rock lip at wet/dry contacts prevents later water ticks flooding dry passages.
        int[] filled = terrain.clone();
        int[] dx = {0, 0, 0, 0, -1, 1}, dy = {-1, 1, 0, 0, 0, 0}, dz = {0, 0, -1, 1, 0, 0};
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            if (!ocean[(z << 4) | x]) continue;
            for (int y = -51; y < 0; y++) {
                int i = index(x, y, z);
                if (filled[i] != 0) continue;
                for (int side = 0; side < 6; side++) {
                    int nx = x + dx[side], ny = y + dy[side], nz = z + dz[side];
                    if (ny < -64 || ny >= 0) continue;
                    boolean wet;
                    if (nx >= 0 && nx < 16 && nz >= 0 && nz < 16)
                        wet = filled[index(nx, ny, nz)] == water;
                    else {
                        int wx = (chunkX << 4) + nx, wz = (chunkZ << 4) + nz;
                        wet = flooded(seed, wx, wz) || broadCavity(seed, wx, ny, wz);
                    }
                    if (wet) { terrain[i] = deep; break; }
                }
            }
        }
    }

    private static boolean broadCavity(long seed, int x, int y, int z) {
        return y >= -48 && y < 0 && noise(seed, x / 23D, y / 16D, z / 23D) * .72
                + noise(seed ^ 0xA510C41L, x / 9D, y / 8D, z / 9D) * .28
                > .60 + Math.max(0, y + 8) * .006;
    }

    private static boolean roomy(int[] terrain, int x, int y, int z, int water) {
        // Five blocks across both horizontal axes distinguish chambers from long narrow tunnels.
        for (int offset = -2; offset <= 2; offset++) {
            int nx = x + offset, nz = z + offset;
            if (nx < 0 || nx >= 16 || nz < 0 || nz >= 16) return false;
            int a = terrain[index(nx, y, z)], b = terrain[index(x, y, nz)];
            if ((a != 0 && a != water) || (b != 0 && b != water)) return false;
        }
        return true;
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
