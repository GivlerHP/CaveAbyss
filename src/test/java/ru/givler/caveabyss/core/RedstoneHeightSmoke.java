package ru.givler.caveabyss.core;

import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldServer;
import sun.misc.Unsafe;

/** Exercises the vanilla wire algorithm and World power queries with negative coordinates. */
public final class RedstoneHeightSmoke {
    private RedstoneHeightSmoke() { }
    public static void check() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true);
        Unsafe unsafe = (Unsafe)f.get(null);
        Constructor<BlockRedstoneWire> ctor = BlockRedstoneWire.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        BlockRedstoneWire wire = ctor.newInstance();
        Field field = Blocks.class.getDeclaredField("redstone_wire");
        Object previous = unsafe.getObject(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field));
        unsafe.putObject(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field), wire);
        try {
            for (int height : new int[] {20, -1, -30, -62}) {
                TestWorld world = (TestWorld)unsafe.allocateInstance(TestWorld.class);
                world.height = height; world.wire = wire; world.levels = new HashMap<Integer, Integer>();
                world.powered = true;
                world.levels.put(1, 0); world.levels.put(2, 0); world.levels.put(3, 0);
                for (int pass = 0; pass < 4; pass++)
                    for (int x = 1; x <= 3; x++) wire.onNeighborBlockChange(world, x, height, 0, SOURCE);
                for (int x = 1; x <= 3; x++)
                    if (world.levels.get(x) != 16 - x)
                        throw new AssertionError("Wire attenuation at Y=" + height + ", x=" + x + ": " + world.levels);
                if (!world.isBlockIndirectlyGettingPowered(1, height, 0))
                    throw new AssertionError("World power query failed at Y=" + height);
                world.powered = false;
                for (int pass = 0; pass < 20; pass++)
                    for (int x = 1; x <= 3; x++) wire.onNeighborBlockChange(world, x, height, 0, SOURCE);
                for (int level : world.levels.values()) if (level != 0)
                    throw new AssertionError("Wire failed to turn off at Y=" + height);
            }
        } finally { unsafe.putObject(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field), previous); }
        System.out.println("Vanilla redstone propagation, attenuation and switching at negative heights passed");
    }
    private static final Block AIR = new Block(Material.air) {
        @Override public boolean isOpaqueCube() { return false; }
        @Override public boolean renderAsNormalBlock() { return false; }
    };
    private static final Block GROUND = new Block(Material.rock) { };
    private static final Block SOURCE = new Block(Material.rock) {
        @Override public boolean canProvidePower() { return true; }
        @Override public int isProvidingStrongPower(IBlockAccess w, int x, int y, int z, int side) {
            return ((TestWorld)w).powered ? 15 : 0;
        }
        @Override public int isProvidingWeakPower(IBlockAccess w, int x, int y, int z, int side) {
            return ((TestWorld)w).powered ? 15 : 0;
        }
    };
    public static final class TestWorld extends WorldServer {
        private TestWorld() { super(null, null, "test", 0, null, null); }
        int height; Block wire; boolean powered; Map<Integer, Integer> levels;
        @Override public Block getBlock(int x, int y, int z) {
            if (y == height - 1) return GROUND;
            if (y == height && z == 0) {
                if (x == 0) return SOURCE;
                if (levels.containsKey(x)) return wire;
            }
            return AIR;
        }
        @Override public int getBlockMetadata(int x, int y, int z) {
            return y == height && z == 0 && levels.containsKey(x) ? levels.get(x) : 0;
        }
        @Override public boolean setBlockMetadataWithNotify(int x, int y, int z, int meta, int flags) {
            levels.put(x, meta); return true;
        }
        @Override public void notifyBlockOfNeighborChange(int x, int y, int z, Block block) { }
        @Override public void notifyBlocksOfNeighborChange(int x, int y, int z, Block block) { }
        @Override public boolean blockExists(int x, int y, int z) { return y >= -64 && y < 256; }
    }
}

