package ru.givler.caveabyss.world.abyss;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.NetworkHandshakeEstablished;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.network.ForgeMessage;
import net.minecraftforge.common.network.ForgeRuntimeCodec;
import net.minecraftforge.event.world.WorldEvent;

public final class AbyssDimensions {
    private static int providerId;
    private static volatile int[] dimensions = new int[0];
    private static Registry registry;
    public static AbyssWorldType worldType;

    private AbyssDimensions() { }

    public static void init(java.io.File configFile) {
        Configuration config = new Configuration(configFile);
        config.load();
        providerId = config.get("dimensions", "providerId", 18645,
                "Provider type only; depth dimension IDs are allocated and saved automatically.").getInt();
        config.save();
        if (!DimensionManager.registerProviderType(providerId, AbyssWorldProvider.class, false))
            throw new IllegalStateException("CaveAbyss provider ID is occupied: " + providerId);
        worldType = new AbyssWorldType();
        AbyssDimensions events = new AbyssDimensions();
        MinecraftForge.EVENT_BUS.register(events);
        FMLCommonHandler.instance().bus().register(events);
    }

    public static void installHandshake() {
        cpw.mods.fml.common.network.FMLEmbeddedChannel channel = NetworkRegistry.INSTANCE.getChannel("FORGE", Side.SERVER);
        String codec = channel.findChannelHandlerNameForType(ForgeRuntimeCodec.class);
        channel.pipeline().addAfter(codec, "CaveAbyssDimensions", new ChannelInboundHandlerAdapter() {
            @Override public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
                if (event instanceof NetworkHandshakeEstablished)
                    for (int id : dimensions) ctx.writeAndFlush(new ForgeMessage.DimensionRegisterMessage(id, providerId));
                ctx.fireUserEventTriggered(event);
            }
        });
    }

    public static boolean isAbyss(World world) {
        return world.provider instanceof AbyssWorldProvider
                || worldType != null && world.provider.dimensionId == 0
                && world.getWorldInfo().getTerrainType() == worldType;
    }

    public static boolean isDepthDimension(int id) {
        return DimensionManager.isDimensionRegistered(id) && DimensionManager.getProviderType(id) == providerId;
    }

    public static int depth(int dimension) {
        if (dimension == 0) return 0;
        int[] snapshot = dimensions;
        for (int i = 0; i < snapshot.length; i++) if (snapshot[i] == dimension) return i + 1;
        throw new IllegalStateException("Unknown abyss dimension: " + dimension);
    }

    public static void start(MinecraftServer server) {
        World world = server.worldServerForDimension(0);
        registry = (Registry) world.mapStorage.loadData(Registry.class, "CaveAbyssDepths");
        if (registry == null) {
            registry = new Registry("CaveAbyssDepths");
            world.mapStorage.setData("CaveAbyssDepths", registry);
        }
        dimensions = registry.ids.clone();
        for (int id : dimensions) {
            if (DimensionManager.isDimensionRegistered(id)) {
                if (DimensionManager.getProviderType(id) != providerId)
                    throw new IllegalStateException("Saved abyss dimension conflicts with another mod: " + id);
            } else DimensionManager.registerDimension(id, providerId);
        }
    }

    public static void stop() {
        for (int id : dimensions)
            if (DimensionManager.isDimensionRegistered(id) && DimensionManager.getProviderType(id) == providerId)
                DimensionManager.unregisterDimension(id);
        dimensions = new int[0];
        registry = null;
    }

    private static int dimensionAt(int depth) {
        if (depth == 0) return 0;
        if (registry == null || depth < 0) throw new IllegalStateException("Abyss registry is not ready");
        while (dimensions.length < depth) {
            int id = DimensionManager.getNextFreeDimId();
            DimensionManager.registerDimension(id, providerId);
            int[] next = java.util.Arrays.copyOf(dimensions, dimensions.length + 1);
            next[next.length - 1] = id;
            registry.ids = next;
            registry.markDirty();
            // Persist the mapping before players or terrain can be saved in the new dimension.
            MinecraftServer.getServer().worldServerForDimension(0).mapStorage.saveAllData();
            dimensions = next;
        }
        return dimensions[depth - 1];
    }

    @SubscribeEvent public void createSpawn(WorldEvent.CreateSpawnPosition event) {
        if (event.world.provider.dimensionId == 0 && isAbyss(event.world)) {
            event.world.setSpawnLocation(104, 225, 96);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent public void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if (!isAbyss(player.worldObj) || player.isDead || player.ridingEntity != null
                || player.riddenByEntity != null) return;
        NBTTagCompound data = player.getEntityData();
        long now = player.worldObj.getTotalWorldTime();
        if (now < data.getLong("AbyssTransferAfter")) return;
        int currentDepth = depth(player.dimension);
        boolean down = player.posY < 12;
        boolean up = player.posY > 244 && currentDepth > 0;
        if (!down && !up) return;
        FractalCaves caves = new FractalCaves(player.worldObj.getSeed(), currentDepth);
        int x = net.minecraft.util.MathHelper.floor_double(player.posX);
        int z = net.minecraft.util.MathHelper.floor_double(player.posZ);
        if (!caves.isShaft(x, z)) return;
        int targetId = dimensionAt(currentDepth + (down ? 1 : -1));
        int centerX = Math.floorDiv(x, FractalCaves.SHAFT_SPACING) * FractalCaves.SHAFT_SPACING + 96;
        int centerZ = Math.floorDiv(z, FractalCaves.SHAFT_SPACING) * FractalCaves.SHAFT_SPACING + 96;
        WorldServer target = MinecraftServer.getServer().worldServerForDimension(targetId);
        double y = down ? 225 : 33;
        // Search existing terrain. Never clear a player's construction to create a landing.
        double[] landing = findLanding(target, centerX + 8, (int)y, centerZ);
        if (landing == null) {
            player.playerNetServerHandler.setPlayerLocation(centerX + 3.5, 128, centerZ + 0.5,
                    player.rotationYaw, player.rotationPitch);
            player.fallDistance = 0;
            data.setLong("AbyssTransferAfter", now + 60);
            return;
        }
        if (targetId != 0) player.playerNetServerHandler.sendPacket(NetworkRegistry.INSTANCE
                .getChannel("FORGE", Side.SERVER).generatePacketFrom(
                        new ForgeMessage.DimensionRegisterMessage(targetId, providerId)));
        data.setLong("AbyssTransferAfter", now + 60);
        player.fallDistance = 0;
        MinecraftServer.getServer().getConfigurationManager().transferPlayerToDimension(player, targetId,
                new LandingTeleporter(target, landing));
        player.playerNetServerHandler.setPlayerLocation(landing[0], landing[1], landing[2],
                player.rotationYaw, player.rotationPitch);
        player.motionX = player.motionY = player.motionZ = 0;
        player.fallDistance = 0;
    }

    private static double[] findLanding(WorldServer world, int x, int y, int z) {
        for (int distance = 0; distance < 12; distance++)
            for (int dx = -distance; dx <= distance; dx++) for (int dz = -distance; dz <= distance; dz++)
                for (int dy = 0; dy < 32; dy++) {
                    int px = x + dx, py = y - dy, pz = z + dz;
                    if (py < 5 || !world.isAirBlock(px, py, pz) || !world.isAirBlock(px, py + 1, pz)) continue;
                    if (!world.getBlock(px, py - 1, pz).isNormalCube()) continue;
                    if (world.getBlock(px, py - 1, pz) == ru.givler.caveabyss.block.CaveBlocks.magma) continue;
                    return new double[] {px + 0.5, py, pz + 0.5};
                }
        return null;
    }

    private static final class LandingTeleporter extends Teleporter {
        private final double[] landing;
        LandingTeleporter(WorldServer world, double[] landing) { super(world); this.landing = landing; }
        @Override public void placeInPortal(Entity entity, double x, double y, double z, float yaw) {
            entity.setLocationAndAngles(landing[0], landing[1], landing[2], yaw, entity.rotationPitch);
            entity.motionX = entity.motionY = entity.motionZ = 0;
        }
    }

    public static final class Registry extends WorldSavedData {
        private int[] ids = new int[0];
        public Registry(String name) { super(name); }
        @Override public void readFromNBT(NBTTagCompound tag) {
            ids = tag.getIntArray("Dimensions");
            java.util.Set<Integer> seen = new java.util.HashSet<Integer>();
            for (int id : ids) if (id <= 1 || !seen.add(id))
                throw new IllegalStateException("Invalid saved abyss dimension registry");
        }
        @Override public void writeToNBT(NBTTagCompound tag) { tag.setIntArray("Dimensions", ids); }
    }
}
