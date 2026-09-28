package ru.givler.caveabyss.world;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.MapGenCaves;
import net.minecraft.world.gen.MapGenRavine;
import ru.givler.caveabyss.block.CaveBlocks;
import ru.givler.caveabyss.data.MinusOneLayer;
import ru.givler.caveabyss.integration.thaumcraft.ThaumcraftIntegration;
import ru.givler.caveabyss.integration.bop.BopIntegration;
import ru.givler.caveabyss.integration.mf2.Mf2Integration;
import ru.givler.caveabyss.config.DeepOreConfig;

/** Terrain extension called by ChunkProviderGenerate before returning a new chunk. */
public final class DeepWorldGenerator {
    // MapGenBase seeds from chunk coordinates. A fixed offset gives the lower
    // layer its own continuous cave network instead of copying the upper one.
    private static final int DEEP_CAVE_X_OFFSET = 8192;
    private static final int DEEP_CAVE_Z_OFFSET = -8192;
    private DeepWorldGenerator() { }

    public static int[] prepareTerrain(World world, Block[] blocks, int chunkX, int chunkZ) {
        return prepareTerrain(world, blocks, chunkX, chunkZ, true);
    }

    private static int[] prepareTerrain(World world, Block[] blocks, int chunkX, int chunkZ,
                                        boolean connectCaves) {
        if (world.provider.dimensionId != 0 || world.isRemote) return null;
        for (int i = 0; i < blocks.length; i++)
            if ((i & 255) <= 4 && blocks[i] == Blocks.bedrock) blocks[i] = Blocks.stone;
        Random random = new Random(world.getSeed() ^ ((long) chunkX * 341873128712L)
                ^ ((long) chunkZ * 132897987541L) ^ 0xCA7EAB55L);
        int[] terrain = new int[16384];
        int stone = Block.getIdFromBlock(Blocks.stone);
        int deepslate = Block.getIdFromBlock(CaveBlocks.deepslate);
        int bedrock = Block.getIdFromBlock(Blocks.bedrock);
        int baseX = chunkX << 4, baseZ = chunkZ << 4;
        long seed = world.getSeed();

        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            // Keep the uneven vanilla bedrock style, but move its floor to -64.
            int bedrockThickness = 1 + random.nextInt(5);
            for (int y = -64; y < 0; y++) {
                int index = ((y + 64) << 8) | (z << 4) | x;
                if (y < -64 + bedrockThickness) {
                    terrain[index] = bedrock;
                    continue;
                }
                int wx = baseX + x, wz = baseZ + z;
                double transition = Math.max(0.0, Math.min(1.0, (-y - 3.0) / 19.0));
                double variation = noise(seed ^ 0x2A59L, wx / 6.0, y / 5.0, wz / 6.0);
                terrain[index] = variation < transition ? deepslate : stone;
            }
        }

        carveVanillaCaves(world, blocks, terrain, chunkX, chunkZ);
        if (connectCaves) connectToVanillaCave(blocks, terrain, stone, deepslate, seed, chunkX, chunkZ);
        fillDeepLiquids(terrain, random);

        // Keep common utility ores worth mining in the lower layer, while
        // coal and diamonds remain scarce compared with their upper budget.
        configuredVein(random, terrain, stone, deepslate, Blocks.coal_ore, CaveBlocks.coalOre, DeepOreConfig.VANILLA[0]);
        configuredVein(random, terrain, stone, deepslate, Blocks.iron_ore, CaveBlocks.ironOre, DeepOreConfig.VANILLA[1]);
        configuredVein(random, terrain, stone, deepslate, Blocks.gold_ore, CaveBlocks.goldOre, DeepOreConfig.VANILLA[2]);
        configuredVein(random, terrain, stone, deepslate, Blocks.redstone_ore, CaveBlocks.redstoneOre, DeepOreConfig.VANILLA[3]);
        configuredVein(random, terrain, stone, deepslate, Blocks.lapis_ore, CaveBlocks.lapisOre, DeepOreConfig.VANILLA[4]);
        configuredVein(random, terrain, stone, deepslate, Blocks.diamond_ore, CaveBlocks.diamondOre, DeepOreConfig.VANILLA[5]);
        net.minecraft.world.biome.BiomeGenBase biome = world.getBiomeGenForCoords(baseX + 8, baseZ + 8);
        if (biome == net.minecraft.world.biome.BiomeGenBase.extremeHills
                || biome == net.minecraft.world.biome.BiomeGenBase.extremeHillsEdge)
            configuredVein(random, terrain, stone, deepslate, Blocks.emerald_ore, CaveBlocks.emeraldOre, DeepOreConfig.VANILLA[6]);
        ThaumcraftIntegration.generate(world, random, terrain, deepslate, chunkX, chunkZ);
        BopIntegration.generate(world, random, terrain, deepslate, chunkX, chunkZ);
        Mf2Integration.generate(world, random, terrain, deepslate);
        return terrain;
    }

    public static void onProvideChunk(Chunk chunk, int[] terrain) {
        if (terrain != null) MinusOneLayer.fillGeneratedChunk(chunk, terrain);
    }

    /** Generates only the missing lower layer in an existing saved chunk. */
    public static void migrateChunk(Chunk chunk) {
        World world = chunk.worldObj;
        if (world.isRemote || world.provider.dimensionId != 0) return;
        Block[] upper = new Block[65536];
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++)
            for (int y = 0; y < 256; y++)
                upper[((x * 16 + z) << 8) | y] = chunk.getBlock(x, y, z);
        int[] terrain = prepareTerrain(world, upper, chunk.xPosition, chunk.zPosition, false);
        if (terrain == null) return;
        MinusOneLayer.fillGeneratedChunk(chunk, terrain);
        ExtendedBlockStorage bottom = chunk.getBlockStorageArray()[0];
        if (bottom != null) {
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++)
                for (int y = 0; y <= 4; y++)
                    if (bottom.getBlockByExtId(x, y, z) == Blocks.bedrock) {
                        bottom.func_150818_a(x, y, z, Blocks.stone);
                        bottom.setExtBlockMetadata(x, y, z, 0);
                    }
        }
        chunk.setChunkModified();
    }

    private static void vein(Random random, int[] terrain, int stone, int deepslate,
                             Block vanillaOre, Block deepOre, int count, int size, int minY, int maxY) {
        int vanilla = Block.getIdFromBlock(vanillaOre), deep = Block.getIdFromBlock(deepOre);
        for (int n = 0; n < count; n++) {
            int x = random.nextInt(16), z = random.nextInt(16);
            int y = minY + random.nextInt(maxY - minY + 1);
            for (int i = 0; i < size; i++) {
                if (x >= 0 && x < 16 && z >= 0 && z < 16 && y >= -63 && y < 0) {
                    int index = ((y + 64) << 8) | (z << 4) | x;
                    if (terrain[index] == deepslate) terrain[index] = deep;
                    else if (terrain[index] == stone) terrain[index] = vanilla;
                }
                x += random.nextInt(3) - 1;
                y += random.nextInt(3) - 1;
                z += random.nextInt(3) - 1;
            }
        }
    }

    private static void configuredVein(Random random, int[] terrain, int stone, int deepslate,
                                       Block normal, Block deep, DeepOreConfig.Rule rule) {
        if (DeepOreConfig.enabled(rule) && random.nextDouble() < rule.chance)
            vein(random, terrain, stone, deepslate, normal, deep,
                    DeepOreConfig.attempts(rule, random), rule.size, rule.minY, rule.maxY);
    }

    private static void carveVanillaCaves(World world, Block[] upper, int[] lower, int chunkX, int chunkZ) {
        Block[] shifted = new Block[65536];
        int bedrock = Block.getIdFromBlock(Blocks.bedrock);
        for (int column = 0; column < 256; column++) {
            int vanillaColumn = column << 8;
            for (int y = 0; y < 64; y++) {
                int id = lower[lowerIndex(column, y)];
                shifted[vanillaColumn | y] = y < 10 || id == bedrock ? Blocks.bedrock : Blocks.stone;
            }
            for (int y = 64; y < 256; y++)
                shifted[vanillaColumn | y] = upper[vanillaColumn | (y - 64)];
        }
        int deepChunkX = chunkX + DEEP_CAVE_X_OFFSET;
        int deepChunkZ = chunkZ + DEEP_CAVE_Z_OFFSET;
        new MapGenCaves().func_151539_a(null, world, deepChunkX, deepChunkZ, shifted);
        new MapGenRavine().func_151539_a(null, world, deepChunkX, deepChunkZ, shifted);
        copyCarvedCaves(shifted, upper, lower);
    }

    static void copyCarvedCaves(Block[] shifted, Block[] upper, int[] lower) {
        for (int column = 0; column < 256; column++) {
            int vanillaColumn = column << 8;
            for (int y = 10; y < 64; y++) {
                Block result = shifted[vanillaColumn | y];
                if (result == null || result == Blocks.air)
                    lower[lowerIndex(column, y)] = 0;
            }
        }
    }

    /** Vanilla columns are X-major; negative sections are Z-major. */
    static int lowerIndex(int vanillaColumn, int shiftedY) {
        return (shiftedY << 8) | ((vanillaColumn & 15) << 4) | (vanillaColumn >> 4);
    }

    static void connectToVanillaCave(Block[] upper, int[] lower, int stone, int deepslate,
                                     long seed, int chunkX, int chunkZ) {
        int best = Integer.MAX_VALUE;
        int lowerX = 0, lowerY = 0, lowerZ = 0, upperX = 0, upperY = 0, upperZ = 0;
        for (int x = 2; x < 14; x++) for (int z = 2; z < 14; z++) {
            int y = 0;
            for (int candidate = -1; candidate >= -16; candidate--)
                if (lower[((candidate + 64) << 8) | (z << 4) | x] == 0) {
                    y = candidate;
                    break;
                }
            if (y == 0) continue;
            for (int tx = Math.max(2, x - 5); tx <= Math.min(13, x + 5); tx++)
                for (int tz = Math.max(2, z - 5); tz <= Math.min(13, z + 5); tz++) {
                    int column = (tx * 16 + tz) << 8;
                    for (int ty = 1; ty <= 12; ty++) {
                        Block cave = upper[column | ty];
                        if ((cave != null && cave != Blocks.air) || upper[column | (ty + 1)] != Blocks.stone
                                || nearUpperLiquid(upper, tx, ty, tz, 3))
                            continue;
                        int score = (tx - x) * (tx - x) + (tz - z) * (tz - z) + (ty - y) * (ty - y);
                        if (score < best) {
                            best = score;
                            lowerX = x; lowerY = y; lowerZ = z;
                            upperX = tx; upperY = ty; upperZ = tz;
                        }
                    }
                }
        }
        if (best == Integer.MAX_VALUE) return;
        Random shape = new Random(seed ^ ((long) chunkX * 341873128712L)
                ^ ((long) chunkZ * 132897987541L) ^ 0x71A9C43EL);
        double bendX = (shape.nextDouble() - 0.5) * 5.0;
        double bendZ = (shape.nextDouble() - 0.5) * 5.0;
        double phase = shape.nextDouble() * Math.PI * 2.0;
        int steps = Math.max(Math.abs(upperY - lowerY),
                Math.max(Math.abs(upperX - lowerX), Math.abs(upperZ - lowerZ))) * 3;
        for (int step = 0; step <= steps; step++) {
            double t = (double) step / steps;
            double curve = Math.sin(Math.PI * t);
            double cx = lowerX + (upperX - lowerX) * t + 0.5 + bendX * curve;
            double cy = lowerY + (upperY - lowerY) * t + 0.5;
            double cz = lowerZ + (upperZ - lowerZ) * t + 0.5 + bendZ * curve;
            double radius = 1.45 + 0.35 * Math.sin(t * Math.PI * 3.0 + phase);
            for (int x = Math.max(1, (int) Math.floor(cx - radius)); x <= Math.min(14, (int) Math.ceil(cx + radius)); x++)
                for (int z = Math.max(1, (int) Math.floor(cz - radius)); z <= Math.min(14, (int) Math.ceil(cz + radius)); z++)
                    for (int y = Math.max(-54, (int) Math.floor(cy - radius)); y <= Math.min(14, (int) Math.ceil(cy + radius)); y++) {
                        double dx = x + 0.5 - cx, dy = y + 0.5 - cy, dz = z + 0.5 - cz;
                        if (dx * dx + dy * dy + dz * dz > radius * radius) continue;
                        if (y < 0) {
                            int index = ((y + 64) << 8) | (z << 4) | x;
                            if (lower[index] == stone || lower[index] == deepslate) lower[index] = 0;
                        } else {
                            int index = ((x * 16 + z) << 8) | y;
                            if (upper[index] == Blocks.stone && !nearUpperLiquid(upper, x, y, z, 2))
                                upper[index] = Blocks.air;
                        }
                    }
        }
    }

    private static boolean nearUpperLiquid(Block[] upper, int x, int y, int z, int radius) {
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++)
            for (int dy = -radius; dy <= radius; dy++) {
                int bx = x + dx, by = y + dy, bz = z + dz;
                if (bx < 0 || bx >= 16 || bz < 0 || bz >= 16 || by < 0 || by >= 256) continue;
                Block block = upper[((bx * 16 + bz) << 8) | by];
                if (block != null && (block == Blocks.lava || block == Blocks.flowing_lava
                        || block == Blocks.water || block == Blocks.flowing_water)) return true;
            }
        return false;
    }

    private static void fillDeepLiquids(int[] terrain, Random random) {
        int lava = Block.getIdFromBlock(Blocks.lava);
        int water = Block.getIdFromBlock(Blocks.water);
        // Cave air below the lava level becomes a connected, naturally shaped pool.
        for (int y = -54; y <= -52; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int index = ((y + 64) << 8) | (z << 4) | x;
            if (terrain[index] == 0) terrain[index] = lava;
        }
        // Small water pools form on existing cave floors, away from the lava level.
        if (random.nextInt(3) != 0) return;
        for (int attempt = 0; attempt < 48; attempt++) {
            int x = 3 + random.nextInt(10), z = 3 + random.nextInt(10);
            int y = -43 + random.nextInt(29);
            int index = ((y + 64) << 8) | (z << 4) | x;
            if (terrain[index] != 0 || terrain[index - 256] == 0) continue;
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                if (dx * dx + dz * dz > 5) continue;
                int site = ((y + 64) << 8) | ((z + dz) << 4) | (x + dx);
                if (terrain[site] == 0 && terrain[site - 256] != 0) terrain[site] = water;
            }
            return;
        }
    }

    private static double noise(long seed, double x, double y, double z) {
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y), iz = (int) Math.floor(z);
        double fx = smooth(x - ix), fy = smooth(y - iy), fz = smooth(z - iz);
        double result = 0;
        for (int dx = 0; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++) for (int dz = 0; dz <= 1; dz++) {
            double weight = (dx == 0 ? 1 - fx : fx) * (dy == 0 ? 1 - fy : fy) * (dz == 0 ? 1 - fz : fz);
            result += weight * hash(seed, ix + dx, iy + dy, iz + dz);
        }
        return result;
    }

    private static double smooth(double t) { return t * t * (3 - 2 * t); }

    private static double hash(long seed, int x, int y, int z) {
        long n = seed ^ (x * 0x632BE59BD9B4E019L) ^ (y * 0x9E3779B97F4A7C15L)
                ^ (z * 0xC2B2AE3D27D4EB4FL);
        n ^= n >>> 30; n *= 0xBF58476D1CE4E5B9L;
        n ^= n >>> 27; n *= 0x94D049BB133111EBL;
        n ^= n >>> 31;
        return (n >>> 11) * 0x1.0p-53;
    }
}
