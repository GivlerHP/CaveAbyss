package ru.givler.caveabyss.data;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkDataEvent;
import ru.givler.caveabyss.network.MinusOneNetwork;

/** Four negative sections in the byte/nibble layout used by vanilla chunks. */
public final class MinusOneLayer {
    public static final int MIN_Y = -64;
    private static final int SIZE = 16384;
    private static final int NIBBLE_SIZE = SIZE / 2;
    private static final String TAG = "CaveAbyssNegative";
    private static final String OLD_TAG = "CaveAbyssMinusOne";
    private static final Map<Chunk, Layer> LAYERS = Collections.synchronizedMap(new WeakHashMap<Chunk, Layer>());

    public MinusOneLayer() { }

    private static Layer layer(World world, int x, int z) {
        Chunk chunk = world.getChunkFromBlockCoords(x, z);
        Layer result = LAYERS.get(chunk);
        if (result == null) {
            result = new Layer();
            LAYERS.put(chunk, result);
        }
        return result;
    }

    private static int index(int x, int y, int z) {
        return ((y - MIN_Y) << 8) | ((z & 15) << 4) | (x & 15);
    }

    private static boolean valid(int x, int y, int z) {
        return y >= MIN_Y && y < 0 && Math.abs((long)x) < 30000000 && Math.abs((long)z) < 30000000;
    }

    private static int nibble(byte[] array, int index) {
        if (array == null) return 0;
        int packed = array[index >> 1] & 255;
        return (index & 1) == 0 ? packed & 15 : packed >>> 4;
    }

    private static void setNibble(byte[] array, int index, int value) {
        int offset = index >> 1;
        int packed = array[offset] & 255;
        array[offset] = (byte)((index & 1) == 0 ? (packed & 240) | value : (packed & 15) | (value << 4));
    }

    private static int state(Layer layer, int index) {
        if (layer.ids == null) return 0;
        int id = (layer.ids[index] & 255) | (nibble(layer.add, index) << 8);
        return id == 0 ? 0 : id | (nibble(layer.data, index) << 16);
    }

    private static void setState(Layer layer, int index, int state) {
        int id = state & 65535;
        if (id > 4095) throw new IllegalArgumentException("Block ID exceeds 1.7.10 range: " + id);
        if (layer.ids == null) {
            if (id == 0) return;
            layer.ids = new byte[SIZE];
        }
        layer.ids[index] = (byte)id;
        int high = id >>> 8;
        if (layer.add != null || high != 0) {
            if (layer.add == null) layer.add = new byte[NIBBLE_SIZE];
            setNibble(layer.add, index, high);
        }
        int metadata = id == 0 ? 0 : state >>> 16 & 15;
        if (layer.data != null || metadata != 0) {
            if (layer.data == null) layer.data = new byte[NIBBLE_SIZE];
            setNibble(layer.data, index, metadata);
        }
    }

    public static Block getBlock(World world, int x, int y, int z) {
        if (!valid(x, y, z)) return Blocks.air;
        Block block = Block.getBlockById(state(layer(world, x, z), index(x, y, z)) & 65535);
        return block == null ? Blocks.air : block;
    }

    public static int getMetadata(World world, int x, int y, int z) {
        return valid(x, y, z) ? state(layer(world, x, z), index(x, y, z)) >>> 16 & 15 : 0;
    }

    public static boolean setBlock(World world, int x, int y, int z, Block block, int metadata, int flags) {
        if (!valid(x, y, z) || block == null || metadata < 0 || metadata > 15) return false;
        int id = Block.getIdFromBlock(block);
        if (id < 0 || id > 4095) return false;
        int next = id == 0 ? 0 : id | (metadata << 16);
        Chunk chunk = world.getChunkFromBlockCoords(x, z);
        Layer layer = layer(world, x, z);
        int index = index(x, y, z);
        if (state(layer, index) == next) return false;
        setState(layer, index, next);
        chunk.setChunkModified();
        if (!world.isRemote) MinusOneNetwork.broadcast(world, x, y, z, next);
        return true;
    }

    /** Initial terrain only: no block callbacks or per-block packets. */
    public static void fillGeneratedChunk(Chunk chunk, int[] states) {
        if (states.length != SIZE) throw new IllegalArgumentException("Invalid negative terrain size");
        Layer layer = LAYERS.get(chunk);
        if (layer == null) {
            layer = new Layer();
            LAYERS.put(chunk, layer);
        }
        for (int i = 0; i < SIZE; i++)
            if (state(layer, i) == 0 && states[i] != 0) setState(layer, i, states[i]);
        chunk.setChunkModified();
    }

    public static byte[] copySection(Chunk chunk, int section, int part) {
        if (section < 0 || section > 3 || part < 0 || part > 2) throw new IllegalArgumentException("Invalid section");
        Layer layer = LAYERS.get(chunk);
        byte[] source = layer == null ? null : part == 0 ? layer.ids : part == 1 ? layer.add : layer.data;
        int length = part == 0 ? 4096 : 2048;
        byte[] copy = new byte[length];
        if (source != null) System.arraycopy(source, section * length, copy, 0, length);
        return copy;
    }

    public static void applySection(World world, int chunkX, int chunkZ, int section, boolean replace,
                                    byte[] ids, byte[] add, byte[] data) {
        if (section < 0 || section > 3 || ids.length != 4096 || add.length != 2048 || data.length != 2048
                || !world.getChunkProvider().chunkExists(chunkX, chunkZ)) return;
        Layer layer = layer(world, chunkX << 4, chunkZ << 4);
        if (replace) {
            layer.ids = layer.add = layer.data = layer.skyLight = layer.blockLight = null;
        }
        if (layer.ids == null) layer.ids = new byte[SIZE];
        System.arraycopy(ids, 0, layer.ids, section * 4096, 4096);
        layer.add = applyNibbleSection(layer.add, add, section);
        layer.data = applyNibbleSection(layer.data, data, section);
        world.markBlockRangeForRenderUpdate(chunkX << 4, MIN_Y + section * 16, chunkZ << 4,
                (chunkX << 4) + 15, MIN_Y + section * 16 + 15, (chunkZ << 4) + 15);
    }

    private static byte[] applyNibbleSection(byte[] destination, byte[] source, int section) {
        if (destination == null && allZero(source)) return null;
        if (destination == null) destination = new byte[NIBBLE_SIZE];
        System.arraycopy(source, 0, destination, section * 2048, 2048);
        return destination;
    }

    private static boolean allZero(byte[] values) {
        for (byte value : values) if (value != 0) return false;
        return true;
    }

    public static void applyBlock(World world, int x, int y, int z, int state) {
        if (!valid(x, y, z) || !world.getChunkProvider().chunkExists(x >> 4, z >> 4)
                || (state & 65535) > 4095) return;
        setState(layer(world, x, z), index(x, y, z), state);
        world.markBlockForUpdate(x, y, z);
    }

    public static int getLight(World world, EnumSkyBlock type, int x, int y, int z) {
        if (!valid(x, y, z) || !world.getChunkProvider().chunkExists(x >> 4, z >> 4)) return 0;
        Layer layer = layer(world, x, z);
        return nibble(type == EnumSkyBlock.Sky ? layer.skyLight : layer.blockLight, index(x, y, z));
    }

    public static void setLight(World world, EnumSkyBlock type, int x, int y, int z, int value) {
        if (!valid(x, y, z) || !world.getChunkProvider().chunkExists(x >> 4, z >> 4)) return;
        Layer layer = layer(world, x, z);
        byte[] light = type == EnumSkyBlock.Sky ? layer.skyLight : layer.blockLight;
        value &= 15;
        if (light == null && value == 0) return;
        if (light == null) {
            light = new byte[NIBBLE_SIZE];
            if (type == EnumSkyBlock.Sky) layer.skyLight = light;
            else layer.blockLight = light;
        }
        int index = index(x, y, z);
        if (nibble(light, index) == value) return;
        setNibble(light, index, value);
        if (!world.isRemote) world.getChunkFromBlockCoords(x, z).setChunkModified();
    }

    public static byte[] copyLightSection(Chunk chunk, EnumSkyBlock type, int section) {
        Layer layer = LAYERS.get(chunk);
        byte[] source = layer == null ? null : type == EnumSkyBlock.Sky ? layer.skyLight : layer.blockLight;
        byte[] copy = new byte[2048];
        if (source != null) System.arraycopy(source, section * 2048, copy, 0, 2048);
        return copy;
    }

    public static void applyLight(World world, int chunkX, int chunkZ, EnumSkyBlock type, int section, byte[] data) {
        if (data.length != 2048 || section < 0 || section > 3
                || !world.getChunkProvider().chunkExists(chunkX, chunkZ)) return;
        Layer layer = layer(world, chunkX << 4, chunkZ << 4);
        if (type == EnumSkyBlock.Sky) layer.skyLight = applyNibbleSection(layer.skyLight, data, section);
        else layer.blockLight = applyNibbleSection(layer.blockLight, data, section);
        world.markBlockRangeForRenderUpdate(chunkX << 4, MIN_Y + section * 16, chunkZ << 4,
                (chunkX << 4) + 15, MIN_Y + section * 16 + 15, (chunkZ << 4) + 15);
    }

    private static void readLegacyEntries(Layer layer, int[] entries) {
        if ((entries.length & 1) != 0 || entries.length > SIZE * 2) return;
        for (int i = 0; i < entries.length; i += 2) {
            int index = entries[i], state = entries[i + 1], id = state & 65535;
            if (index >= 0 && index < SIZE && id > 0 && id <= 4095 && Block.getBlockById(id) != null)
                setState(layer, index, state);
        }
    }

    private static byte[] readLegacyLight(byte[] old) {
        if (old.length == NIBBLE_SIZE) return old;
        if (old.length != SIZE) return null;
        byte[] packed = new byte[NIBBLE_SIZE];
        for (int i = 0; i < SIZE; i++) setNibble(packed, i, old[i] & 15);
        return packed;
    }

    @SubscribeEvent
    public void onLoad(ChunkDataEvent.Load event) {
        Layer layer = new Layer();
        NBTTagCompound data = event.getData();
        if (data.hasKey(TAG, 10)) {
            NBTTagCompound saved = data.getCompoundTag(TAG);
            if (saved.getByte("Format") == 2) {
                byte[] ids = saved.getByteArray("Ids");
                byte[] add = saved.getByteArray("Add");
                byte[] metadata = saved.getByteArray("Data");
                if (ids.length == SIZE) layer.ids = ids;
                if (add.length == NIBBLE_SIZE) layer.add = add;
                if (metadata.length == NIBBLE_SIZE) layer.data = metadata;
            } else readLegacyEntries(layer, saved.getIntArray("Blocks"));
            layer.skyLight = readLegacyLight(saved.getByteArray("SkyLight"));
            layer.blockLight = readLegacyLight(saved.getByteArray("BlockLight"));
        } else if (data.hasKey(OLD_TAG, 10)) {
            NBTTagCompound saved = data.getCompoundTag(OLD_TAG);
            int[] ids = saved.getIntArray("Blocks");
            byte[] metadata = saved.getByteArray("Data");
            if (ids.length == 256 && metadata.length == 256)
                for (int i = 0; i < 256; i++)
                    if (ids[i] > 0 && ids[i] <= 4095)
                        setState(layer, (63 << 8) | i, ids[i] | ((metadata[i] & 15) << 16));
        }
        LAYERS.put(event.getChunk(), layer);
    }

    @SubscribeEvent
    public void onSave(ChunkDataEvent.Save event) {
        NBTTagCompound saved = new NBTTagCompound();
        saved.setByte("Format", (byte)2);
        Layer layer = LAYERS.get(event.getChunk());
        if (layer != null) {
            if (layer.ids != null) saved.setByteArray("Ids", layer.ids);
            if (layer.add != null) saved.setByteArray("Add", layer.add);
            if (layer.data != null) saved.setByteArray("Data", layer.data);
            if (layer.skyLight != null) saved.setByteArray("SkyLight", layer.skyLight);
            if (layer.blockLight != null) saved.setByteArray("BlockLight", layer.blockLight);
        }
        event.getData().setTag(TAG, saved);
    }

    private static final class Layer {
        private byte[] ids;
        private byte[] add;
        private byte[] data;
        private byte[] skyLight;
        private byte[] blockLight;
    }
}
