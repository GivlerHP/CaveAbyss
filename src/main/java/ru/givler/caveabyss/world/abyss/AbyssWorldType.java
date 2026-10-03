package ru.givler.caveabyss.world.abyss;

import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.biome.WorldChunkManagerHell;
import net.minecraft.world.chunk.IChunkProvider;

public final class AbyssWorldType extends WorldType {
    public AbyssWorldType() { super("abyss"); }
    @Override public WorldChunkManager getChunkManager(World world) {
        world.provider.hasNoSky = true;
        return new WorldChunkManagerHell(BiomeGenBase.jungle, 0.8F);
    }
    @Override public IChunkProvider getChunkGenerator(World world, String options) {
        return new AbyssChunkProvider(world, 0);
    }
    @Override public int getMinimumSpawnHeight(World world) { return 225; }
    @Override public int getSpawnFuzz() { return 0; }
    @Override public boolean hasVoidParticles(boolean flag) { return false; }
}
