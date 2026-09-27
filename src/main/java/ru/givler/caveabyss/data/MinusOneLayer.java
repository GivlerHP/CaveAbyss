package ru.givler.caveabyss.data;

import ru.givler.caveabyss.network.MinusOneNetwork;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkDataEvent;

/** Experimental negative block storage, Y=-64 through -1. */
public final class MinusOneLayer {
    public static final int MIN_Y = -64;
    private static final String TAG = "CaveAbyssNegative";
    private static final String OLD_TAG = "CaveAbyssMinusOne";
    private static final Map<Chunk, Layer> LAYERS = Collections.synchronizedMap(new WeakHashMap<Chunk, Layer>());

    public MinusOneLayer() { }

    private static Layer layer(World world, int x, int z) {
        Chunk chunk = world.getChunkFromBlockCoords(x, z);
        Layer layer = LAYERS.get(chunk);
        if (layer == null) {
            layer = new Layer();
            LAYERS.put(chunk, layer);
        }
        return layer;
    }

    private static int index(int x, int y, int z) {
        return ((y - MIN_Y) << 8) | ((z & 15) << 4) | (x & 15);
    }

    private static boolean valid(int x, int y, int z) {
        return y >= MIN_Y && y < 0 && Math.abs((long)x) < 30000000 && Math.abs((long)z) < 30000000;
    }

    public static Block getBlock(World world, int x, int y, int z) {
        if (!valid(x, y, z)) return Blocks.air;
        Integer state = layer(world, x, z).blocks.get(index(x, y, z));
        Block block = Block.getBlockById(state == null ? 0 : state & 65535);
        return block == null ? Blocks.air : block;
    }

    public static int getMetadata(World world, int x, int y, int z) {
        if (!valid(x, y, z)) return 0;
        Integer state = layer(world, x, z).blocks.get(index(x, y, z));
        return state == null ? 0 : state >>> 16 & 15;
    }

    public static boolean setBlock(World world, int x, int y, int z, Block block, int metadata, int flags) {
        if (!valid(x, y, z) || block == null || metadata < 0 || metadata > 15) return false;
        int id = Block.getIdFromBlock(block);
        if (id < 0 || id > 65535) return false;
        int state = id == 0 ? 0 : id | (metadata << 16);
        Chunk chunk = world.getChunkFromBlockCoords(x, z);
        Layer layer = layer(world, x, z);
        int index = index(x, y, z);
        Integer previous = layer.blocks.get(index);
        if ((previous == null ? 0 : previous) == state) return false;
        if (state == 0) layer.blocks.remove(index);
        else layer.blocks.put(index, state);
        chunk.setChunkModified();
        if (!world.isRemote) MinusOneNetwork.broadcast(world, x, y, z, state);
        return true;
    }

    public static int[] copyChunk(Chunk chunk) {
        Layer layer = LAYERS.get(chunk);
        if (layer == null) return new int[0];
        int[] entries = new int[layer.blocks.size() * 2];
        int i = 0;
        for (Map.Entry<Integer, Integer> entry : layer.blocks.entrySet()) {
            entries[i++] = entry.getKey();
            entries[i++] = entry.getValue();
        }
        return entries;
    }

    public static void applyChunk(World world, int chunkX, int chunkZ, int[] entries, boolean replace) {
        if (!world.getChunkProvider().chunkExists(chunkX, chunkZ)) return;
        Layer layer = layer(world, chunkX << 4, chunkZ << 4);
        if (replace) {
            layer.blocks.clear();
            layer.skyLight = null;
            layer.blockLight = null;
        }
        readEntries(layer, entries);
    }

    public static void applyBlock(World world, int x, int y, int z, int state) {
        if (!valid(x, y, z) || !world.getChunkProvider().chunkExists(x >> 4, z >> 4)) return;
        Layer layer = layer(world, x, z);
        if (state == 0) layer.blocks.remove(index(x, y, z));
        else layer.blocks.put(index(x, y, z), state);
    }

    public static Map<Integer, Integer> visibleBlocks(Chunk chunk) {
        Layer layer = LAYERS.get(chunk);
        return layer == null ? Collections.<Integer, Integer>emptyMap() : layer.blocks;
    }

    public static int getLight(World world, EnumSkyBlock type, int x, int y, int z) {
        if (!valid(x, y, z) || !world.getChunkProvider().chunkExists(x >> 4, z >> 4)) return 0;
        Layer layer = layer(world, x, z);
        byte[] light = type == EnumSkyBlock.Sky ? layer.skyLight : layer.blockLight;
        return light == null ? 0 : light[index(x, y, z)] & 15;
    }

    public static void setLight(World world, EnumSkyBlock type, int x, int y, int z, int value) {
        if (!valid(x, y, z) || !world.getChunkProvider().chunkExists(x >> 4, z >> 4)) return;
        Layer layer = layer(world, x, z);
        byte[] light = type == EnumSkyBlock.Sky ? layer.skyLight : layer.blockLight;
        if (light == null && value == 0) return;
        if (light == null) {
            light = new byte[16384];
            if (type == EnumSkyBlock.Sky) layer.skyLight = light;
            else layer.blockLight = light;
        }
        int index = index(x, y, z);
        if (light[index] == (byte)value) return;
        light[index] = (byte)value;
        if (!world.isRemote) world.getChunkFromBlockCoords(x, z).setChunkModified();
    }

    public static byte[] copyLight(Chunk chunk, EnumSkyBlock type) {
        Layer layer = LAYERS.get(chunk);
        byte[] light = layer == null ? null : type == EnumSkyBlock.Sky ? layer.skyLight : layer.blockLight;
        return light == null ? new byte[16384] : light.clone();
    }

    public static void applyLight(World world, int chunkX, int chunkZ, EnumSkyBlock type, int section, byte[] data) {
        if (data.length != 4096 || section < 0 || section > 3 || !world.getChunkProvider().chunkExists(chunkX, chunkZ)) return;
        Layer layer = layer(world, chunkX << 4, chunkZ << 4);
        byte[] light = type == EnumSkyBlock.Sky ? layer.skyLight : layer.blockLight;
        if (light == null) {
            light = new byte[16384];
            if (type == EnumSkyBlock.Sky) layer.skyLight = light;
            else layer.blockLight = light;
        }
        System.arraycopy(data, 0, light, section * 4096, 4096);
    }

    private static void readEntries(Layer layer, int[] entries) {
        if ((entries.length & 1) != 0 || entries.length > 32768) return;
        for (int i = 0; i < entries.length; i += 2) {
            int index = entries[i], state = entries[i + 1];
            if (index >= 0 && index < 16384 && (state & 65535) != 0 && Block.getBlockById(state & 65535) != null)
                layer.blocks.put(index, state);
        }
    }

    @SubscribeEvent
    public void onLoad(ChunkDataEvent.Load event) {
        Layer layer = new Layer();
        NBTTagCompound data = event.getData();
        if (data.hasKey(TAG, 10)) {
            NBTTagCompound saved = data.getCompoundTag(TAG);
            readEntries(layer, saved.getIntArray("Blocks"));
            byte[] sky = saved.getByteArray("SkyLight");
            byte[] block = saved.getByteArray("BlockLight");
            if (sky.length == 16384) layer.skyLight = sky;
            if (block.length == 16384) layer.blockLight = block;
        } else if (data.hasKey(OLD_TAG, 10)) {
            NBTTagCompound saved = data.getCompoundTag(OLD_TAG);
            int[] ids = saved.getIntArray("Blocks");
            byte[] metadata = saved.getByteArray("Data");
            if (ids.length == 256 && metadata.length == 256)
                for (int i = 0; i < 256; i++)
                    if (ids[i] != 0) layer.blocks.put((63 << 8) | i, ids[i] | ((metadata[i] & 15) << 16));
        }
        LAYERS.put(event.getChunk(), layer);
    }

    @SubscribeEvent
    public void onSave(ChunkDataEvent.Save event) {
        NBTTagCompound saved = new NBTTagCompound();
        saved.setIntArray("Blocks", copyChunk(event.getChunk()));
        Layer layer = LAYERS.get(event.getChunk());
        if (layer != null && layer.skyLight != null) saved.setByteArray("SkyLight", layer.skyLight);
        if (layer != null && layer.blockLight != null) saved.setByteArray("BlockLight", layer.blockLight);
        event.getData().setTag(TAG, saved);
    }

    private static final class Layer {
        private final Map<Integer, Integer> blocks = new HashMap<Integer, Integer>();
        private byte[] skyLight;
        private byte[] blockLight;
    }
}
