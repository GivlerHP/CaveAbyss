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
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.event.world.ChunkWatchEvent;

public final class MinusOneNetwork {
    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("caveabyss_v2");

    public static void init() {
        CHANNEL.registerMessage(Snapshot.Handler.class, Snapshot.class, 0, Side.CLIENT);
        CHANNEL.registerMessage(Delta.Handler.class, Delta.class, 1, Side.CLIENT);
        CHANNEL.registerMessage(LightSnapshot.Handler.class, LightSnapshot.class, 2, Side.CLIENT);
    }

    @SubscribeEvent
    public void onWatch(ChunkWatchEvent.Watch event) {
        Chunk chunk = event.player.worldObj.getChunkFromChunkCoords(event.chunk.chunkXPos, event.chunk.chunkZPos);
        for (int section = 0; section < 4; section++) {
            CHANNEL.sendTo(new Snapshot(event.player.dimension, event.chunk.chunkXPos, event.chunk.chunkZPos,
                    section, section == 0, MinusOneLayer.copySection(chunk, section, 0),
                    MinusOneLayer.copySection(chunk, section, 1), MinusOneLayer.copySection(chunk, section, 2)), event.player);
        }
        for (int section = 0; section < 4; section++) {
            byte[] sky = MinusOneLayer.copyLightSection(chunk, EnumSkyBlock.Sky, section);
            byte[] block = MinusOneLayer.copyLightSection(chunk, EnumSkyBlock.Block, section);
            if (hasLight(sky, block))
                CHANNEL.sendTo(new LightSnapshot(event.player.dimension, event.chunk.chunkXPos, event.chunk.chunkZPos,
                        section, sky, block), event.player);
        }
        for (Object value : chunk.chunkTileEntityMap.values()) {
            TileEntity tile = (TileEntity) value;
            if (!tile.isInvalid() && tile.yCoord >= -64 && tile.yCoord < 0) {
                Packet packet = tile.getDescriptionPacket();
                if (packet != null) event.player.playerNetServerHandler.sendPacket(packet);
            }
        }
    }

    private static boolean hasLight(byte[] sky, byte[] block) {
        for (int i = 0; i < 2048; i++)
            if (sky[i] != 0 || block[i] != 0) return true;
        return false;
    }

    public static void broadcast(World world, int x, int y, int z, int state) {
        CHANNEL.sendToAllAround(new Delta(world.provider.dimensionId, x, y, z, state),
                new NetworkRegistry.TargetPoint(world.provider.dimensionId, x + 0.5, y + 0.5, z + 0.5, 128));
    }

    /** Tile entities keep their vanilla description packets and chunk NBT. */
    public static void broadcastTileEntity(World world, TileEntity tile) {
        if (tile.isInvalid()) return;
        Packet packet = tile.getDescriptionPacket();
        if (packet == null) return;
        for (Object value : world.playerEntities) {
            if (!(value instanceof EntityPlayerMP)) continue;
            EntityPlayerMP player = (EntityPlayerMP) value;
            double dx = player.posX - tile.xCoord, dy = player.posY - tile.yCoord, dz = player.posZ - tile.zCoord;
            if (dx * dx + dy * dy + dz * dz <= 128 * 128)
                player.playerNetServerHandler.sendPacket(packet);
        }
    }

    public static final class Snapshot implements IMessage {
        private int dimension, chunkX, chunkZ, section;
        private boolean replace;
        private byte[] ids = new byte[4096], add = new byte[2048], data = new byte[2048];
        public Snapshot() { }
        Snapshot(int dimension, int chunkX, int chunkZ, int section, boolean replace,
                 byte[] ids, byte[] add, byte[] data) {
            this.dimension = dimension; this.chunkX = chunkX; this.chunkZ = chunkZ;
            this.section = section; this.replace = replace;
            this.ids = ids; this.add = add; this.data = data;
        }
        @Override public void fromBytes(ByteBuf buffer) {
            dimension = buffer.readInt(); chunkX = buffer.readInt(); chunkZ = buffer.readInt();
            section = buffer.readUnsignedByte(); replace = buffer.readBoolean();
            if (section > 3 || buffer.readableBytes() < 8192) throw new IllegalArgumentException("Bad negative chunk snapshot");
            buffer.readBytes(ids); buffer.readBytes(add); buffer.readBytes(data);
        }
        @Override public void toBytes(ByteBuf buffer) {
            buffer.writeInt(dimension); buffer.writeInt(chunkX); buffer.writeInt(chunkZ);
            buffer.writeByte(section); buffer.writeBoolean(replace);
            buffer.writeBytes(ids); buffer.writeBytes(add); buffer.writeBytes(data);
        }
        public static final class Handler implements IMessageHandler<Snapshot, IMessage> {
            @Override public IMessage onMessage(final Snapshot message, MessageContext context) {
                Minecraft.getMinecraft().func_152344_a(new Runnable() {
                    @Override public void run() {
                        World world = Minecraft.getMinecraft().theWorld;
                        if (world != null && world.provider.dimensionId == message.dimension)
                            MinusOneLayer.applySection(world, message.chunkX, message.chunkZ, message.section,
                                    message.replace, message.ids, message.add, message.data);
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
                            Chunk chunk = world.getChunkFromBlockCoords(message.x, message.z);
                            int x = message.x & 15, z = message.z & 15;
                            Block block = Block.getBlockById(message.state & 65535);
                            if (block == null) block = Blocks.air;
                            int metadata = message.state >>> 16 & 15;
                            if (chunk.getBlock(x, message.y, z) == block)
                                chunk.setBlockMetadata(x, message.y, z, metadata);
                            else
                                chunk.func_150807_a(x, message.y, z, block, metadata);
                            world.markBlockForUpdate(message.x, message.y, message.z);
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
        private byte[] sky = new byte[2048], block = new byte[2048];
        public LightSnapshot() { }
        LightSnapshot(int dimension, int chunkX, int chunkZ, int section, byte[] sky, byte[] block) {
            this.dimension = dimension; this.chunkX = chunkX; this.chunkZ = chunkZ; this.section = section;
            this.sky = sky; this.block = block;
        }
        @Override public void fromBytes(ByteBuf buffer) {
            dimension = buffer.readInt(); chunkX = buffer.readInt(); chunkZ = buffer.readInt(); section = buffer.readUnsignedByte();
            if (section > 3 || buffer.readableBytes() < 4096) throw new IllegalArgumentException("Bad negative light snapshot");
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
