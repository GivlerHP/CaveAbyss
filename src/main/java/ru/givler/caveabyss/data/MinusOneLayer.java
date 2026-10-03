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
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import java.util.Arrays;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import ru.givler.caveabyss.network.MinusOneNetwork;
import ru.givler.caveabyss.block.CaveBlocks;
import ru.givler.caveabyss.world.DeepWorldGenerator;

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
        return layer(chunk);
    }

    private static Layer layer(Chunk chunk) {
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

    /** Chunk hooks must not request their own chunk again while it is loading. */
    public static Block getBlock(Chunk chunk, int x, int y, int z) {
        Layer data = LAYERS.get(chunk);
        if (data == null || y < MIN_Y || y >= 0) return Blocks.air;
        Block block = Block.getBlockById(state(data, index(x, y, z)) & 65535);
        return block == null ? Blocks.air : block;
    }

    public static int getMetadata(World world, int x, int y, int z) {
        return valid(x, y, z) ? state(layer(world, x, z), index(x, y, z)) >>> 16 & 15 : 0;
    }

    public static int getMetadata(Chunk chunk, int x, int y, int z) {
        Layer data = LAYERS.get(chunk);
        return data == null || y < MIN_Y || y >= 0 ? 0 : state(data, index(x, y, z)) >>> 16 & 15;
    }

    public static boolean setBlock(World world, int x, int y, int z, Block block, int metadata, int flags) {
        if (!valid(x, y, z) || block == null || metadata < 0 || metadata > 15) return false;
        return setBlock(world.getChunkFromBlockCoords(x, z), x, y, z, block, metadata, flags);
    }

    public static boolean setBlock(Chunk chunk, int x, int y, int z, Block block, int metadata, int flags) {
        World world = chunk.worldObj;
        if (!valid(x, y, z) || block == null || metadata < 0 || metadata > 15) return false;
        int id = Block.getIdFromBlock(block);
        if (id < 0 || id > 4095) return false;
        int next = id == 0 ? 0 : id | (metadata << 16);
        Layer layer = layer(chunk);
        int index = index(x, y, z);
        int previous = state(layer, index);
        if (previous == next) return false;
        setState(layer, index, next);
        Block oldBlock = Block.getBlockById(previous & 65535);
        if (block.getTickRandomly() || oldBlock != null && oldBlock.getTickRandomly())
            layer.randomTickSections[(y - MIN_Y) >> 4] = 0;
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
        Arrays.fill(layer.randomTickSections, (byte)0);
        chunk.setChunkModified();
    }

    /** Terrain is written without callbacks, so exposed source liquids need their first scheduled tick. */
    public static void activateGeneratedLiquids(Chunk chunk) {
        if (!(chunk.worldObj instanceof WorldServer) || chunk.worldObj.provider.dimensionId != 0) return;
        Layer layer = LAYERS.get(chunk);
        if (layer == null || layer.ids == null) return;
        WorldServer world = (WorldServer)chunk.worldObj;
        int water = Block.getIdFromBlock(Blocks.water);
        int lava = Block.getIdFromBlock(Blocks.lava);
        int baseX = chunk.xPosition << 4, baseZ = chunk.zPosition << 4;
        boolean changed = false;
        for (int y = MIN_Y; y < 0; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int index = index(x, y, z);
            int id = state(layer, index) & 65535;
            if (id != water && id != lava) continue;
            boolean exposed = x == 0 || x == 15 || z == 0 || z == 15
                    || y == -1 && chunk.getBlock(x, 0, z) == Blocks.air
                    || y < -1 && (state(layer, index + 256) & 65535) == 0
                    || y > MIN_Y && (state(layer, index - 256) & 65535) == 0
                    || x > 0 && (state(layer, index - 1) & 65535) == 0
                    || x < 15 && (state(layer, index + 1) & 65535) == 0
                    || z > 0 && (state(layer, index - 16) & 65535) == 0
                    || z < 15 && (state(layer, index + 16) & 65535) == 0;
            if (!exposed) continue;
            Block flowing = id == water ? Blocks.flowing_water : Blocks.flowing_lava;
            setState(layer, index, Block.getIdFromBlock(flowing));
            changed = true;
            world.func_147446_b(baseX + x, y, baseZ + z, flowing, flowing.tickRate(world), 0);
        }
        if (changed) chunk.setChunkModified();
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        activateGeneratedLiquids(event.getChunk());
    }

    /** Mirrors ExtendedBlockStorage.getNeedsRandomTick without scanning on every tick. */
    public static boolean needsRandomTick(Chunk chunk, int section) {
        if (section < 0 || section >= 4) return false;
        Layer layer = LAYERS.get(chunk);
        if (layer == null || layer.ids == null) return false;
        byte cached = layer.randomTickSections[section];
        if (cached == 0) {
            cached = 1;
            int start = section << 12;
            for (int index = start; index < start + 4096; index++) {
                int id = state(layer, index) & 65535;
                Block block = Block.getBlockById(id);
                if (block != null && block.getTickRandomly()) {
                    cached = 2;
                    break;
                }
            }
            layer.randomTickSections[section] = cached;
        }
        return cached == 2;
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
        int baseX = chunkX << 4, baseZ = chunkZ << 4;
        int stoneId = Block.getIdFromBlock(Blocks.stone);
        int deepslateId = Block.getIdFromBlock(CaveBlocks.deepslate);
        for (int dy = 0; dy < 16; dy++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int y = MIN_Y + section * 16 + dy;
            int state = state(layer, index(x, y, z));
            int id = state & 65535;
            if (id == 0 || id == stoneId || id == deepslateId) continue;
            Block block = Block.getBlockById(id);
            if (block != null && block.hasTileEntity(state >>> 16 & 15))
                world.getTileEntity(baseX + x, y, baseZ + z);
        }
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

    public static int getLight(Chunk chunk, EnumSkyBlock type, int x, int y, int z) {
        Layer data = LAYERS.get(chunk);
        if (data == null || y < MIN_Y || y >= 0) return 0;
        return nibble(type == EnumSkyBlock.Sky ? data.skyLight : data.blockLight, index(x, y, z));
    }

    public static void setLight(World world, EnumSkyBlock type, int x, int y, int z, int value) {
        if (!valid(x, y, z) || !world.getChunkProvider().chunkExists(x >> 4, z >> 4)) return;
        setLight(world.getChunkFromBlockCoords(x, z), type, x, y, z, value);
    }

    public static void setLight(Chunk chunk, EnumSkyBlock type, int x, int y, int z, int value) {
        if (!valid(x, y, z)) return;
        Layer layer = layer(chunk);
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
        if (!chunk.worldObj.isRemote) chunk.setChunkModified();
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
        if (!event.getChunk().worldObj.isRemote
                && !ru.givler.caveabyss.world.abyss.AbyssDimensions.isAbyss(event.getChunk().worldObj)
                && event.getChunk().worldObj.provider.dimensionId == 0 && needsMigration(data))
            DeepWorldGenerator.migrateChunk(event.getChunk());
    }

    static boolean needsMigration(NBTTagCompound data) {
        if (data.hasKey(OLD_TAG, 10)) {
            int[] blocks = data.getCompoundTag(OLD_TAG).getIntArray("Blocks");
            for (int block : blocks) if (block != 0) return false;
            return true;
        }
        if (!data.hasKey(TAG, 10)) return true;
        NBTTagCompound saved = data.getCompoundTag(TAG);
        if (saved.getByte("Format") != 2) return saved.getIntArray("Blocks").length == 0;
        byte[] ids = saved.getByteArray("Ids");
        if (ids.length != SIZE) return true;
        for (byte id : ids) if (id != 0) return false;
        byte[] add = saved.getByteArray("Add");
        for (byte value : add) if (value != 0) return false;
        return true;
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
        private final byte[] randomTickSections = new byte[4];
    }
}
