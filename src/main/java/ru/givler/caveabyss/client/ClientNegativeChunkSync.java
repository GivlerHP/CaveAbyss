package ru.givler.caveabyss.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

public final class ClientNegativeChunkSync {
    private static final DeferredChunkUpdates PENDING = new DeferredChunkUpdates();
    private static long key(int x, int z) { return ((long) x << 32) | (z & 0xffffffffL); }
    public static void submit(final World world, int x, int z, Runnable update) {
        PENDING.submit(world, key(x, z), update, chunk -> ready(world, chunk));
    }
    private static boolean ready(World world, long key) {
        if (world == null || Minecraft.getMinecraft().theWorld != world) return false;
        Chunk chunk = world.getChunkProvider().provideChunk((int) (key >> 32), (int) key);
        return chunk != null && !(chunk instanceof EmptyChunk) && chunk.isChunkLoaded;
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        final World world = Minecraft.getMinecraft().theWorld;
        PENDING.drain(world, chunk -> ready(world, chunk));
    }
    @SubscribeEvent public void unload(ChunkEvent.Unload event) {
        if (event.world.isRemote) PENDING.unload(key(event.getChunk().xPosition, event.getChunk().zPosition));
    }
    @SubscribeEvent public void unloadWorld(WorldEvent.Unload event) {
        if (event.world.isRemote) PENDING.drain(null, chunk -> false);
    }
}
