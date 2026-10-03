package ru.givler.caveabyss.world.abyss;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManagerHell;
import net.minecraft.world.chunk.IChunkProvider;

public final class AbyssWorldProvider extends WorldProvider {
    @Override protected void registerWorldChunkManager() {
        hasNoSky = true;
        worldChunkMgr = new WorldChunkManagerHell(BiomeGenBase.jungle, 0.8F);
    }
    @Override public IChunkProvider createChunkGenerator() {
        return new AbyssChunkProvider(worldObj, AbyssDimensions.depth(dimensionId));
    }
    @Override public String getDimensionName() { return "Fractal Abyss"; }
    @Override public boolean isSurfaceWorld() { return false; }
    @Override public int getActualHeight() { return 256; }
    @Override public boolean canRespawnHere() { return false; }
    @Override public int getRespawnDimension(EntityPlayerMP player) { return 0; }
    @Override public ChunkCoordinates getEntrancePortalLocation() { return new ChunkCoordinates(104, 225, 96); }
    @Override public float calculateCelestialAngle(long time, float partial) { return 0.5F; }
    @Override public Vec3 getFogColor(float angle, float partial) { return Vec3.createVectorHelper(0.035, 0.045, 0.04); }
    @Override public void calculateInitialWeather() { resetRainAndThunder(); }
    @Override public void updateWeather() { resetRainAndThunder(); }
}
