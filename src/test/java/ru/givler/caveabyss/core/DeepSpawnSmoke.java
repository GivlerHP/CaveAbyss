package ru.givler.caveabyss.core;

import java.util.BitSet;
import java.util.Random;

public final class DeepSpawnSmoke {
    private DeepSpawnSmoke() { }

    public static void check() {
        for (int upper : new int[] {15, 79, 255, 256}) {
            BitSet seen = new BitSet(upper + 64);
            Random random = new Random(12345);
            for (int i = 0; i < 100000; i++) {
                int y = DeepSpawnHooks.sampleHeight(random, upper, 0);
                if (y < -64 || y >= upper) throw new AssertionError("Spawn outside world: " + y);
                seen.set(y + 64);
            }
            if (seen.cardinality() != upper + 64)
                throw new AssertionError("Some spawn heights are unreachable, upper=" + upper);
        }
        for (int dimension : new int[] {-1, 1, -42}) {
            Random actual = new Random(5678), vanilla = new Random(5678);
            for (int i = 0; i < 1000; i++) {
                if (DeepSpawnHooks.sampleHeight(actual, 128, dimension) != vanilla.nextInt(128))
                    throw new AssertionError("Changed spawn height in dimension " + dimension);
            }
        }
        System.out.println("Negative spawn height coverage and other dimensions passed");
    }
}
