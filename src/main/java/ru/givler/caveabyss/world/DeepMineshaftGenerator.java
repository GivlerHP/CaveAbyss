package ru.givler.caveabyss.world;

import net.minecraft.world.gen.structure.MapGenMineshaft;
import net.minecraft.world.gen.structure.StructureMineshaftStart;
import net.minecraft.world.gen.structure.StructureStart;

/** A separate persistent structure map for mineshafts below Y=0. */
public final class DeepMineshaftGenerator extends MapGenMineshaft {
    @Override
    public String func_143025_a() {
        return "CaveAbyssDeepMineshaft";
    }

    @Override
    protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ) {
        return rand.nextDouble() < 0.0015D;
    }

    @Override
    protected StructureStart getStructureStart(int chunkX, int chunkZ) {
        StructureMineshaftStart start = new StructureMineshaftStart(worldObj, rand, chunkX, chunkZ);
        DeepStructureHooks.placeDeepMineshaft(start, rand);
        return start;
    }

    public void releaseWorld() {
        worldObj = null;
    }
}
