package ru.givler.caveabyss.world;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import ru.givler.caveabyss.block.CaveBlocks;

/** Layered chambers, narrow rising passages and occasional tall halls. */
public final class MountainCaves {
    private MountainCaves() { }

    public static boolean isMountain(BiomeGenBase biome) {
        return biome != null && !OceanCaves.isOceanBiome(biome)
                && (biome == BiomeGenBase.extremeHills || biome == BiomeGenBase.extremeHillsEdge
                || biome.biomeID == BiomeGenBase.extremeHills.biomeID + 128
                || BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.MOUNTAIN));
    }

    public static void carve(World world, int cx, int cz, int[] terrain) {
        boolean[] mountains = new boolean[256];
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++)
            mountains[z * 16 + x] = isMountain(world.getBiomeGenForCoords(cx * 16 + x, cz * 16 + z));
        carve(world.getSeed(), cx, cz, terrain, mountains,
                Block.getIdFromBlock(Blocks.stone), Block.getIdFromBlock(CaveBlocks.deepslate));
    }

    static void carve(long seed, int cx, int cz, int[] terrain, boolean[] mountains, int stone, int deep) {
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            if (!mountains[z * 16 + x]) continue;
            int wx = cx * 16 + x, wz = cz * 16 + z;
            Profile profile = profile(seed, wx, wz);
            for (int y = -51; y <= -3; y++) {
                int i = index(x, y, z);
                double vertical = profile.vertical(y);
                boolean tunnel = profile.tunnel(y);
                if (vertical > 1 && !tunnel) continue;
                if (vertical <= 1 && pillar(seed, wx, wz, vertical)) {
                    // Repair earlier vanilla carving through a pillar's core.
                    // This runs only while generating an absent lower layer.
                    if (terrain[i] == 0) terrain[i] = deep;
                } else if (terrain[i] == stone || terrain[i] == deep) {
                    terrain[i] = 0;
                }
            }
        }
    }

    static double chamberCenter(long seed, int x, int z) {
        return -29 + 14 * (DeepWorldGenerator.noise(seed ^ 0xD21CL, x / 36D, 0, z / 36D) - .5)
                + 6 * (DeepWorldGenerator.noise(seed ^ 0xD21DL, x / 13D, 0, z / 13D) - .5);
    }

    static double chamberHalfHeight(long seed, int x, int z) {
        return layerHalfHeight(seed, x, z);
    }

    private static double layerHalfHeight(long seed, int x, int z) {
        double height = 4 + 2.5 * DeepWorldGenerator.noise(seed ^ 0xD21AL, x / 25D, 0, z / 25D);
        double coverage = DeepWorldGenerator.noise(seed ^ 0xD219L, x / 32D, 0, z / 32D);
        return height * Math.min(1, Math.max(0, (coverage - .44) / .16));
    }

    static Profile profile(long seed, int x, int z) {
        Profile profile = new Profile();
        double middle = chamberCenter(seed, x, z);
        for (int layer = 0; layer < 3; layer++) {
            long layerSeed = seed ^ (layer - 1) * 0x7139B5L;
            profile.centers[layer] = middle + (layer - 1) * 15;
            profile.halves[layer] = layerHalfHeight(layerSeed, x, z);
        }
        // Tall halls are local exceptions, rather than merging every tier.
        double tall = DeepWorldGenerator.noise(seed ^ 0x7A11L, x / 48D, 0, z / 48D);
        profile.centers[3] = middle;
        profile.halves[3] = 18 * Math.min(1, Math.max(0, (tall - .78) / .10));

        int cellX = Math.floorDiv(x, 64), cellZ = Math.floorDiv(z, 64);
        long hash = mix(seed ^ 0x7A99L, cellX, 0, cellZ);
        double along = x - cellX * 64 - 8;
        if (hash % 3 != 0 && along >= 0 && along <= 48) {
            double pathZ = cellZ * 64 + 16 + ((hash >>> 4) & 31)
                    + 4 * Math.sin(along / 9D + (hash & 7));
            profile.tunnelDistance = (z + .5 - pathZ) / 1.6;
            profile.tunnelY = -44 + along * (30D / 48D)
                    + 1.2 * Math.sin(along / 11D);
        }
        return profile;
    }

    static final class Profile {
        final double[] centers = new double[4], halves = new double[4];
        double tunnelDistance = Double.POSITIVE_INFINITY, tunnelY;

        double vertical(int y) {
            double result = Double.POSITIVE_INFINITY;
            for (int i = 0; i < halves.length; i++)
                if (halves[i] >= 2) result = Math.min(result, Math.abs(y - centers[i]) / halves[i]);
            return result;
        }

        boolean tunnel(int y) {
            double dy = (y - tunnelY) / 1.8;
            return tunnelDistance * tunnelDistance + dy * dy <= 1;
        }
    }

    /** Jittered world-space anchors: broad feet/caps, narrower solid waists. */
    static boolean pillar(long seed, int x, int z, double vertical) {
        int gx = Math.floorDiv(x, 20), gz = Math.floorDiv(z, 20);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            long hash = mix(seed ^ 0xC011AL, gx + dx, 0, gz + dz);
            double px = (gx + dx) * 20 + 4 + (hash & 15) * .75;
            double pz = (gz + dz) * 20 + 4 + ((hash >>> 4) & 15) * .75;
            double waist = 1.5 + ((hash >>> 8) & 7) * .12;
            double radius = waist + 1.6 * Math.pow(Math.min(1, vertical), 3);
            double sx = x + .5 - px, sz = z + .5 - pz;
            if (sx * sx + sz * sz <= radius * radius) return true;
        }
        return false;
    }

    /** Establish host rock BEFORE ore generation, which only replaces stone/deepslate. */
    public static void line(World world, int cx, int cz, int[] terrain) {
        boolean[] mountains = new boolean[256];
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++)
            mountains[z * 16 + x] = isMountain(world.getBiomeGenForCoords(cx * 16 + x, cz * 16 + z));
        line(world.getSeed(), cx, cz, terrain, mountains, Block.getIdFromBlock(Blocks.stone),
                Block.getIdFromBlock(CaveBlocks.deepslate), Block.getIdFromBlock(CaveBlocks.dripstoneBlock),
                Block.getIdFromBlock(CaveBlocks.calcite));
    }

    static void line(long seed, int cx, int cz, int[] terrain, boolean[] mountains,
                     int stone, int deep, int rock, int calcite) {
        int[] original = terrain.clone();
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            if (!mountains[z * 16 + x]) continue;
            int wx = cx * 16 + x, wz = cz * 16 + z;
            for (int y = -53; y <= -2; y++) {
                int i = index(x, y, z);
                if (original[i] != stone && original[i] != deep) continue;
                boolean surface = original[i - 256] == 0 || original[i + 256] == 0
                        || original[i - 512] == 0 || y < -2 && original[i + 512] == 0;
                if (!surface) continue;
                int coating = coating(seed, wx, y, wz);
                if (coating != 0) terrain[i] = coating == 2 ? rock : calcite;
            }
        }
    }

    /** Local three-dimensional patches, rather than coating every tier together. */
    static int coating(long seed, int x, int y, int z) {
        double patch = .72 * DeepWorldGenerator.noise(seed ^ 0xD219BL, x / 14D, y / 9D, z / 14D)
                + .28 * DeepWorldGenerator.noise(seed ^ 0xD219CL, x / 6D, y / 5D, z / 6D);
        return patch < .62 ? 0 : patch < .66 ? 1 : 2;
    }

    public static void decorate(World world, int cx, int cz, int[] terrain) {
        int rock = Block.getIdFromBlock(CaveBlocks.dripstoneBlock);
        int calcite = Block.getIdFromBlock(CaveBlocks.calcite);
        int point = Block.getIdFromBlock(CaveBlocks.pointedDripstone);
        int stone = Block.getIdFromBlock(Blocks.stone), deep = Block.getIdFromBlock(CaveBlocks.deepslate);
        boolean[] mountains = new boolean[256];
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++)
            mountains[z * 16 + x] = isMountain(world.getBiomeGenForCoords(cx * 16 + x, cz * 16 + z));
        decorate(world.getSeed(), cx, cz, terrain, mountains, stone, deep, rock, calcite, point,
                Block.getIdFromBlock(Blocks.water));
    }

    static void decorate(long seed, int cx, int cz, int[] terrain, boolean[] mountains,
                         int stone, int deep, int rock, int calcite, int point, int water) {
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            if (!mountains[z * 16 + x]) continue;
            int wx = cx * 16 + x, wz = cz * 16 + z;
            for (int y = -53; y <= -2; y++) {
                int i = index(x, y, z);
                // Use only the host patches laid down before ores. Decoration
                // must not turn the remaining natural rock into a second coat.
                if (terrain[i] != rock) continue;
                boolean ceiling = terrain[i - 256] == 0, floor = terrain[i + 256] == 0;
                if (!ceiling && !floor) continue;
                long hash = mix(seed, wx, y, wz);
                // Small pointed decorations are accents, not a forest of spikes.
                if ((hash & 31) != 0) continue;
                int direction = ceiling ? -1 : 1;
                int space = 0;
                while (space < 12 && y + direction * (space + 1) >= -52
                        && y + direction * (space + 1) <= -2
                        && terrain[index(x, y + direction * (space + 1), z)] == 0) space++;
                if (space < 2) continue;
                int length = Math.min(space - 1, 1 + (int)((hash >>> 5) % 3));
                int endY = y + direction * (space + 1);
                if (space <= 3 && endY >= -52 && endY <= -2 && (hash & 32) == 0) {
                    int end = terrain[index(x, endY, z)];
                    if ((end & 65535) == point && ((end & (8 << 16)) != 0) != (direction < 0))
                        length = space; // Opposing formations sometimes meet as a column.
                }
                for (int n = 1; n <= length; n++)
                    terrain[index(x, y + direction * n, z)] = point | (direction < 0 ? 8 << 16 : 0);
                // Some ceilings hold a source behind the limestone: real water,
                // not an unconditional drip flag, so mining changes the system.
                if (ceiling && (hash & 511) == 0 && y < -4 && terrain[i + 256] == deep)
                    terrain[i + 256] = water;
            }
        }
    }

    private static long mix(long seed, int x, int y, int z) {
        long n = seed ^ (x * 341873128712L) ^ (z * 132897987541L) ^ (y * 73428767L);
        n = (n ^ n >>> 30) * 0xbf58476d1ce4e5b9L;
        return (n ^ n >>> 27) & Long.MAX_VALUE;
    }

    private static int index(int x, int y, int z) { return ((y + 64) << 8) | (z << 4) | x; }
}
