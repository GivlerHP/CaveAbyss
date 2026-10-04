package ru.givler.caveabyss.core;

import java.util.Random;
import net.minecraft.world.World;
import ru.givler.caveabyss.data.MinusOneLayer;

public final class DeepSpawnHooks {
    private DeepSpawnHooks() { }

    public static int randomHeight(Random random, int upperExclusive, World world) {
        return sampleHeight(random, upperExclusive, world.provider.dimensionId);
    }

    static int sampleHeight(Random random, int upperExclusive, int dimension) {
        int minimum = dimension == 0 ? MinusOneLayer.MIN_Y : 0;
        return minimum + random.nextInt(upperExclusive - minimum);
    }
}
