package ru.givler.caveabyss.world.abyss;

import java.util.Random;
import net.minecraft.world.gen.NoiseGeneratorPerlin;

/** Coordinate-only density: chunk order never changes the cave network. */
public final class FractalCaves {
    public static final int SHAFT_SPACING = 192;
    private final NoiseGeneratorPerlin noise;
    private final double offset;

    public FractalCaves(long seed, int depth) {
        noise = new NoiseGeneratorPerlin(new Random(seed), 4);
        offset = depth * 73.719;
    }

    private double fbm(double x, double z) {
        return noise.func_151601_a(x, z) / 15.0;
    }

    public double moisture(int x, int z) {
        return fbm(x / 110.0 + offset, z / 110.0 - offset);
    }

    public boolean isShaft(int x, int z) {
        int dx = Math.floorMod(x, SHAFT_SPACING) - 96;
        int dz = Math.floorMod(z, SHAFT_SPACING) - 96;
        return dx * dx + dz * dz <= 16;
    }

    /** A continuous walkable spiral joins every stratum, including transfer landings. */
    public int rampFloor(int x, int z) {
        int dx = Math.floorMod(x, SHAFT_SPACING) - 96;
        int dz = Math.floorMod(z, SHAFT_SPACING) - 96;
        return 16 + (int) Math.floor((Math.atan2(dz, dx) + Math.PI) / (2 * Math.PI) * 32);
    }

    public double shaftRadius(int x, int z) {
        double dx = Math.floorMod(x, SHAFT_SPACING) - 96;
        double dz = Math.floorMod(z, SHAFT_SPACING) - 96;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public Column column(int x, int z) {
        double wx = x + 34 * fbm(x / 180.0 + offset, z / 180.0);
        double wz = z + 34 * fbm(x / 180.0, z / 180.0 - offset + 37);
        return new Column(x, z, wx, wz, moisture(x, z));
    }

    public final class Column {
        public final double moisture;
        private final int x, z;
        private final double radius;
        private final double[] centers = new double[7], heights = new double[7], passages = new double[7];
        private Column(int x, int z, double wx, double wz, double moisture) {
            this.x = x; this.z = z; this.moisture = moisture;
            radius = shaftRadius(x, z);
            for (int layer = 0; layer < 7; layer++) {
                double shift = offset + layer * 19.37;
                centers[layer] = 28 + layer * 32 + 22 * fbm(wx / 65.0 + shift, wz / 65.0);
                double chamber = fbm(wx / 48.0 + shift + 53, wz / 48.0 - shift);
                heights[layer] = 6 + 9 * Math.abs(chamber) + Math.max(0, chamber - 0.15) * 34;
                passages[layer] = Math.abs(fbm(wx / 36.0 + shift, wz / 36.0 - shift));
            }
        }

        public boolean air(int y) {
            if (y < 4 || y > 251) return false;
            // Shaft collar and spiral are independent of depth, so both ends align.
            if (radius <= 4) return true;
            if (radius < 13) {
                int floor = rampFloor(x, z);
                int phase = Math.floorMod(y - floor, 32);
                return phase >= 1 && phase <= 23;
            }
            if (y < 16 || y > 239) return false;
            for (int layer = 0; layer < 7; layer++) {
                double vertical = Math.abs(y - centers[layer]) / heights[layer];
                if (vertical < 1 && passages[layer] < 0.28 * Math.sqrt(1 - vertical * vertical)) return true;
            }
            double ridges = Math.abs(fbm(x / 62.0 + y / 81.0 + offset,
                    z / 62.0 - y / 97.0));
            return ridges < 0.025;
        }
    }
}
