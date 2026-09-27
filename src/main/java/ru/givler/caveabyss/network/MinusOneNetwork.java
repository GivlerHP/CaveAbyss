package ru.givler.caveabyss.network;

import ru.givler.caveabyss.data.MinusOneLayer;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkWatchEvent;

public final class MinusOneNetwork {
    private static final int BATCH_ENTRIES = 1000;
    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("caveabyss_y1");

    public static void init() {
        CHANNEL.registerMessage(Snapshot.Handler.class, Snapshot.class, 0, Side.CLIENT);
        CHANNEL.registerMessage(Delta.Handler.class, Delta.class, 1, Side.CLIENT);
        CHANNEL.registerMessage(LightSnapshot.Handler.class, LightSnapshot.class, 2, Side.CLIENT);
    }

    @SubscribeEvent
    public void onWatch(ChunkWatchEvent.Watch event) {
        Chunk chunk = event.player.worldObj.getChunkFromChunkCoords(event.chunk.chunkXPos, event.chunk.chunkZPos);
        int[] entries = MinusOneLayer.copyChunk(chunk);
        for (int start = 0; start == 0 || start < entries.length; start += BATCH_ENTRIES * 2) {
            int end = Math.min(start + BATCH_ENTRIES * 2, entries.length);
            CHANNEL.sendTo(new Snapshot(event.player.dimension, event.chunk.chunkXPos, event.chunk.chunkZPos,
                    start == 0, Arrays.copyOfRange(entries, start, end)), event.player);
        }
        byte[] sky = MinusOneLayer.copyLight(chunk, EnumSkyBlock.Sky);
        byte[] block = MinusOneLayer.copyLight(chunk, EnumSkyBlock.Block);
        for (int section = 0; section < 4; section++)
            if (hasLight(sky, block, section))
                CHANNEL.sendTo(new LightSnapshot(event.player.dimension, event.chunk.chunkXPos, event.chunk.chunkZPos,
                        section, Arrays.copyOfRange(sky, section * 4096, (section + 1) * 4096),
                        Arrays.copyOfRange(block, section * 4096, (section + 1) * 4096)), event.player);
    }

    private static boolean hasLight(byte[] sky, byte[] block, int section) {
        for (int i = section * 4096; i < (section + 1) * 4096; i++)
            if (sky[i] != 0 || block[i] != 0) return true;
        return false;
    }

    public static void broadcast(World world, int x, int y, int z, int state) {
        CHANNEL.sendToAllAround(new Delta(world.provider.dimensionId, x, y, z, state),
                new NetworkRegistry.TargetPoint(world.provider.dimensionId, x + 0.5, y + 0.5, z + 0.5, 128));
    }

    public static final class Snapshot implements IMessage {
        private int dimension, chunkX, chunkZ;
        private boolean replace;
        private int[] entries = new int[0];
        public Snapshot() { }
        Snapshot(int dimension, int chunkX, int chunkZ, boolean replace, int[] entries) {
            this.dimension = dimension; this.chunkX = chunkX; this.chunkZ = chunkZ;
            this.replace = replace; this.entries = entries;
        }
        @Override public void fromBytes(ByteBuf buffer) {
            dimension = buffer.readInt(); chunkX = buffer.readInt(); chunkZ = buffer.readInt();
            replace = buffer.readBoolean();
            int count = buffer.readUnsignedShort();
            if (count > BATCH_ENTRIES || buffer.readableBytes() < count * 8) throw new IllegalArgumentException("Bad negative chunk snapshot");
            entries = new int[count * 2];
            for (int i = 0; i < entries.length; i++) entries[i] = buffer.readInt();
        }
        @Override public void toBytes(ByteBuf buffer) {
            buffer.writeInt(dimension); buffer.writeInt(chunkX); buffer.writeInt(chunkZ);
            buffer.writeBoolean(replace); buffer.writeShort(entries.length / 2);
            for (int entry : entries) buffer.writeInt(entry);
        }
        public static final class Handler implements IMessageHandler<Snapshot, IMessage> {
            @Override public IMessage onMessage(final Snapshot message, MessageContext context) {
                Minecraft.getMinecraft().func_152344_a(new Runnable() {
                    @Override public void run() {
                        World world = Minecraft.getMinecraft().theWorld;
                        if (world != null && world.provider.dimensionId == message.dimension)
                            MinusOneLayer.applyChunk(world, message.chunkX, message.chunkZ, message.entries, message.replace);
                    }
                });
                return null;
            }
        }
    }

    public static final class Delta implements IMessage {
        private int dimension, x, y, z, state;
        public Delta() { }
        Delta(int dimension, int x, int y, int z, int state) {
            this.dimension = dimension; this.x = x; this.y = y; this.z = z; this.state = state;
        }
        @Override public void fromBytes(ByteBuf buffer) {
            dimension = buffer.readInt(); x = buffer.readInt(); y = buffer.readByte(); z = buffer.readInt(); state = buffer.readInt();
        }
        @Override public void toBytes(ByteBuf buffer) {
            buffer.writeInt(dimension); buffer.writeInt(x); buffer.writeByte(y); buffer.writeInt(z); buffer.writeInt(state);
        }
        public static final class Handler implements IMessageHandler<Delta, IMessage> {
            @Override public IMessage onMessage(final Delta message, MessageContext context) {
                Minecraft.getMinecraft().func_152344_a(new Runnable() {
                    @Override public void run() {
                        World world = Minecraft.getMinecraft().theWorld;
                        if (world != null && world.provider.dimensionId == message.dimension)
                        {
                            MinusOneLayer.applyBlock(world, message.x, message.y, message.z, message.state);
                            world.func_147451_t(message.x, message.y, message.z);
                        }
                    }
                });
                return null;
            }
        }
    }

    public static final class LightSnapshot implements IMessage {
        private int dimension, chunkX, chunkZ, section;
        private byte[] sky = new byte[4096], block = new byte[4096];
        public LightSnapshot() { }
        LightSnapshot(int dimension, int chunkX, int chunkZ, int section, byte[] sky, byte[] block) {
            this.dimension = dimension; this.chunkX = chunkX; this.chunkZ = chunkZ; this.section = section;
            this.sky = sky; this.block = block;
        }
        @Override public void fromBytes(ByteBuf buffer) {
            dimension = buffer.readInt(); chunkX = buffer.readInt(); chunkZ = buffer.readInt(); section = buffer.readUnsignedByte();
            if (section > 3 || buffer.readableBytes() < 8192) throw new IllegalArgumentException("Bad negative light snapshot");
            buffer.readBytes(sky); buffer.readBytes(block);
        }
        @Override public void toBytes(ByteBuf buffer) {
            buffer.writeInt(dimension); buffer.writeInt(chunkX); buffer.writeInt(chunkZ); buffer.writeByte(section);
            buffer.writeBytes(sky); buffer.writeBytes(block);
        }
        public static final class Handler implements IMessageHandler<LightSnapshot, IMessage> {
            @Override public IMessage onMessage(final LightSnapshot message, MessageContext context) {
                Minecraft.getMinecraft().func_152344_a(new Runnable() {
                    @Override public void run() {
                        World world = Minecraft.getMinecraft().theWorld;
                        if (world == null || world.provider.dimensionId != message.dimension) return;
                        MinusOneLayer.applyLight(world, message.chunkX, message.chunkZ, EnumSkyBlock.Sky, message.section, message.sky);
                        MinusOneLayer.applyLight(world, message.chunkX, message.chunkZ, EnumSkyBlock.Block, message.section, message.block);
                    }
                });
                return null;
            }
        }
    }
}
