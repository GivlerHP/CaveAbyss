package ru.givler.caveabyss.world;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import ru.givler.caveabyss.block.CaveBlocks;

/** Deterministic cross-chunk geodes, written while the negative terrain is created. */
public final class AmethystGeodes {
    private static final long SALT = 0x5AE0DE6942L;
    private AmethystGeodes() { }

    public static void generate(long seed, int chunkX, int chunkZ, int[] terrain) {
        for (int cx = chunkX - 1; cx <= chunkX + 1; cx++)
            for (int cz = chunkZ - 1; cz <= chunkZ + 1; cz++) {
                Random random = new Random(seed ^ SALT ^ (cx * 341873128712L) ^ (cz * 132897987541L));
                // All geodes live in just 64 negative blocks; vanilla's 1/24
                // rate looks crowded here.
                if (random.nextInt(64) != 0) continue;
                int x = (cx << 4) + random.nextInt(16);
                int y = -48 + random.nextInt(36);
                int z = (cz << 4) + random.nextInt(16);
                double rx = 6.0 + random.nextDouble() * 2.5;
                double ry = 5.0 + random.nextDouble() * 2.0;
                double rz = 6.0 + random.nextDouble() * 2.5;
                long shapeSeed = random.nextLong();
                int opening = random.nextInt(5) == 0 ? -1 : random.nextInt(6);
                place(seed, shapeSeed, x, y, z, rx, ry, rz, opening, chunkX, chunkZ, terrain);
            }
    }

    private static void place(long seed, long salt, int cx, int cy, int cz,
                              double rx, double ry, double rz, int opening,
                              int chunkX, int chunkZ, int[] terrain) {
        int minX = Math.max(chunkX << 4, cx - 16), maxX = Math.min((chunkX << 4) + 15, cx + 16);
        int minZ = Math.max(chunkZ << 4, cz - 16), maxZ = Math.min((chunkZ << 4) + 15, cz + 16);
        int bedrock = Block.getIdFromBlock(Blocks.bedrock);
        int basalt = Block.getIdFromBlock(CaveBlocks.smoothBasalt);
        int calcite = Block.getIdFromBlock(CaveBlocks.calcite);
        int amethyst = Block.getIdFromBlock(CaveBlocks.amethystBlock);
        int budding = Block.getIdFromBlock(CaveBlocks.buddingAmethyst);
        for (int wx = minX; wx <= maxX; wx++) for (int wz = minZ; wz <= maxZ; wz++)
            for (int y = Math.max(-62, cy - 15); y <= Math.min(-2, cy + 15); y++) {
                int index = ((y + 64) << 8) | ((wz & 15) << 4) | (wx & 15);
                int old = terrain[index] & 65535;
                Block existing = Block.getBlockById(old);
                // A cave is already air: do not build a free-standing shell in it.
                // Only replace the underground rock (including modded ores).
                if (old == bedrock || existing == null || existing.getMaterial() != Material.rock)
                    continue;
                double d = distance(salt, wx, y, wz, cx, cy, cz, rx, ry, rz);
                if (d > 1.75) continue;
                if (opening >= 0 && inOpening(opening, wx - cx, y - cy, wz - cz, rx, ry, rz)) {
                    terrain[index] = 0;
                } else if (d > 1.22) continue;
                else if (d > 1.10) terrain[index] = basalt;
                else if (d > 0.98) terrain[index] = calcite;
                else if (d > 0.79) {
                    // Roughly one in twelve inner wall blocks can grow crystals.
                    terrain[index] = hash(salt ^ 0x41554DL, wx, y, wz) % 12 == 0 ? budding : amethyst;
                } else terrain[index] = 0;
            }

        // Seed a few buds on inward faces. Cross-chunk neighbors use the same
        // geometric classifier, so generation order cannot create seams.
        int[] dx = {0, 0, 0, 0, -1, 1};
        int[] dy = {-1, 1, 0, 0, 0, 0};
        int[] dz = {0, 0, -1, 1, 0, 0};
        for (int wx = minX; wx <= maxX; wx++) for (int wz = minZ; wz <= maxZ; wz++)
            for (int y = Math.max(-62, cy - 15); y <= Math.min(-2, cy + 15); y++) {
                double d = distance(salt, wx, y, wz, cx, cy, cz, rx, ry, rz);
                if (d >= 0.79 || d < 0.60) continue;
                int index = ((y + 64) << 8) | ((wz & 15) << 4) | (wx & 15);
                if (terrain[index] != 0) continue;
                for (int face = 0; face < 6; face++) {
                    int bx = wx - dx[face], by = y - dy[face], bz = wz - dz[face];
                    if (bx < (chunkX << 4) || bx >= (chunkX << 4) + 16
                            || bz < (chunkZ << 4) || bz >= (chunkZ << 4) + 16) continue;
                    int supportIndex = ((by + 64) << 8) | ((bz & 15) << 4) | (bx & 15);
                    if (by < -64 || by >= 0 || (terrain[supportIndex] & 65535) != budding)
                        continue;
                    double supportDistance = distance(salt, bx, by, bz, cx, cy, cz, rx, ry, rz);
                    if (by < -63 || by >= 0 || supportDistance <= 0.79
                            || supportDistance > 0.98
                            || opening >= 0 && inOpening(opening, bx - cx, by - cy, bz - cz, rx, ry, rz)
                            || hash(salt ^ 0x41554DL, bx, by, bz) % 12 != 0) continue;
                    if (hash(seed ^ salt, wx, y, wz) % 3 == 0) {
                        int stage = (int)(hash(salt, wx, y, wz) % 4);
                        terrain[index] = Block.getIdFromBlock(CaveBlocks.amethystBuds[stage]) | (face << 16);
                    }
                    break;
                }
            }
    }

    private static boolean inOpening(int face, int x, int y, int z,
                                     double rx, double ry, double rz) {
        double along = face == 0 ? -y / ry : face == 1 ? y / ry
                : face == 2 ? -z / rz : face == 3 ? z / rz
                : face == 4 ? -x / rx : x / rx;
        double across = face < 2 ? x * x / (rx * rx) + z * z / (rz * rz)
                : face < 4 ? x * x / (rx * rx) + y * y / (ry * ry)
                : y * y / (ry * ry) + z * z / (rz * rz);
        return along > 0.48 && across < 0.07;
    }

    private static double distance(long seed, int x, int y, int z, int cx, int cy, int cz,
                                   double rx, double ry, double rz) {
        double nx = (x - cx) / rx, ny = (y - cy) / ry, nz = (z - cz) / rz;
        double rough = ((hash(seed, x >> 1, y >> 1, z >> 1) & 255) / 255.0 - 0.5) * 0.10;
        return Math.sqrt(nx * nx + ny * ny + nz * nz) + rough;
    }

    private static long hash(long seed, int x, int y, int z) {
        long h = seed ^ (x * 0x632BE59BD9B4E019L) ^ (y * 0x9E3779B97F4A7C15L)
                ^ (z * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        return (h ^ (h >>> 31)) & Long.MAX_VALUE;
    }
}
