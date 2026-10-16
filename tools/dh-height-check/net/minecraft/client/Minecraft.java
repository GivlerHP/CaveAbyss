package net.minecraft.client;

/** Minimal fixture for running the production readiness tracker without Minecraft. */
public class Minecraft {
    private static final Minecraft INSTANCE = new Minecraft();
    public World theWorld = new World();
    public static Minecraft getMinecraft() { return INSTANCE; }
    public static class World extends net.minecraft.world.World {
        public boolean loaded = true;
        public final net.minecraft.world.chunk.Chunk chunk = new net.minecraft.world.chunk.Chunk();
        public World getChunkProvider() { return this; }
        public boolean chunkExists(int x, int z) { return true; }
        public net.minecraft.world.chunk.Chunk provideChunk(int x, int z) {
            chunk.isChunkLoaded = loaded;
            return chunk;
        }
    }
}
