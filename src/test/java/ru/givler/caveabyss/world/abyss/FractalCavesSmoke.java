package ru.givler.caveabyss.world.abyss;

/** Geometry invariants independent of an OpenGL client or running server. */
public final class FractalCavesSmoke {
    public static void main(String[] args) { check(); }

    public static void check() {
        FractalCaves first = new FractalCaves(7158293, 0);
        FractalCaves same = new FractalCaves(7158293, 0);
        FractalCaves deeper = new FractalCaves(7158293, 23);
        FractalCaves otherSeed = new FractalCaves(998172, 0);
        int open = 0, samples = 0, differentDepth = 0, differentSeed = 0;
        int[] floors = new int[7];
        for (int x = -200; x <= 200; x += 7) for (int z = -200; z <= 200; z += 11) {
            FractalCaves.Column a = first.column(x, z), b = same.column(x, z);
            FractalCaves.Column c = deeper.column(x, z), d = otherSeed.column(x, z);
            for (int y = 0; y < 256; y++) {
                boolean air = a.air(y);
                require(air == b.air(y), "Nondeterministic terrain");
                if (y < 4 || y > 251) require(!air, "Broken boundary");
                if (air) open++;
                samples++;
                if (air != c.air(y)) differentDepth++;
                if (air != d.air(y)) differentSeed++;
                if (y >= 16 && y < 240 && air && !a.air(y - 1)) floors[(y - 16) / 32]++;
            }
        }
        require(open > samples / 8 && open < samples * 3 / 4, "Cave density out of range: " + open + "/" + samples);
        require(differentDepth > samples / 20, "Depths repeat");
        require(differentSeed > samples / 20, "Seed is ignored");
        for (int count : floors) require(count > 30, "Missing cave stratum");
        for (int sx = -2; sx <= 2; sx++) for (int sz = -2; sz <= 2; sz++) {
            int x = sx * 192 + 96, z = sz * 192 + 96;
            require(first.isShaft(x, z), "Missing shaft at negative coordinates");
            for (int y = 4; y <= 251; y++) require(first.column(x, z).air(y), "Blocked transfer shaft");
            for (int y : new int[] {33, 225}) {
                FractalCaves.Column landing = first.column(x + 8, z);
                require(!landing.air(y - 1) && landing.air(y) && landing.air(y + 1), "Unsafe landing");
            }
        }
        System.out.println("Fractal caves OK: air=" + (100 * open / samples) + "%, floors="
                + java.util.Arrays.toString(floors));
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
