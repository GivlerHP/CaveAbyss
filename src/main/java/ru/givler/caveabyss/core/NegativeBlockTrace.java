package ru.givler.caveabyss.core;

import net.minecraft.block.Block;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Temporary diagnostics for block interactions across the Y=0 boundary. */
public final class NegativeBlockTrace {
    private static final Logger LOG = LogManager.getLogger("CaveAbyss/NegativeBlocks");
    private static volatile int targetX, targetY, targetZ;
    private static volatile long targetTime;

    private NegativeBlockTrace() { }

    public static void packet(C07PacketPlayerDigging packet, boolean read) {
        int action = packet.func_149506_g();
        if (action > 2) return;
        int x = packet.func_149505_c(), y = packet.func_149503_d(), z = packet.func_149502_e();
        if (y >= 0 && y < 192) return;
        remember(x, y, z);
        LOG.info("{} dig action={} x={} y={} z={} face={}", read ? "RECV" : "SEND", action,
                x, y, z, packet.func_149501_f());
    }

    public static void correction(S23PacketBlockChange packet, boolean read) {
        int x = packet.func_148879_d(), y = packet.func_148878_e(), z = packet.func_148877_f();
        if (y >= 0 && y < 192) return;
        LOG.info("{} BLOCK_CHANGE x={} y={} z={} block={} meta={}", read ? "RECV" : "SEND",
                x, y, z, Block.getIdFromBlock(packet.func_148880_c()), packet.func_148881_g());
    }

    public static void serverDig(NetHandlerPlayServer handler, C07PacketPlayerDigging packet, boolean after) {
        int action = packet.func_149506_g();
        if (action > 2) return;
        int x = packet.func_149505_c(), y = packet.func_149503_d(), z = packet.func_149502_e();
        if (y >= 0 && y < 192) return;
        remember(x, y, z);
        World world = handler.playerEntity.worldObj;
        Block block = world.getBlock(x, y, z);
        LOG.info("SERVER {} action={} x={} y={} z={} block={} meta={} playerY={}",
                after ? "END" : "BEGIN", action, x, y, z,
                Block.getIdFromBlock(block), world.getBlockMetadata(x, y, z), handler.playerEntity.posY);
    }

    public static void mutation(World world, int x, int y, int z, int oldState, int newState) {
        if (!recent(x, y, z)) return;
        LOG.info("STORAGE {} x={} y={} z={} old={} new={}", world.isRemote ? "CLIENT" : "SERVER",
                x, y, z, oldState, newState);
    }

    public static void delta(World world, int x, int y, int z, int state) {
        if (!recent(x, y, z)) return;
        LOG.info("CLIENT DELTA x={} y={} z={} state={} before={}", x, y, z, state,
                Block.getIdFromBlock(world.getBlock(x, y, z)));
    }

    public static void snapshot(World world, int x, int y, int z, int state, boolean after) {
        if (!recent(x, y, z)) return;
        LOG.info("CLIENT SNAPSHOT {} x={} y={} z={} state={}", after ? "AFTER" : "BEFORE",
                x, y, z, state);
    }

    public static boolean recent(int x, int y, int z) {
        return x == targetX && y == targetY && z == targetZ
                && System.currentTimeMillis() - targetTime < 20000;
    }

    public static int recentX() { return targetX; }
    public static int recentY() { return targetY; }
    public static int recentZ() { return targetZ; }

    private static void remember(int x, int y, int z) {
        targetX = x; targetY = y; targetZ = z;
        targetTime = System.currentTimeMillis();
    }
}
