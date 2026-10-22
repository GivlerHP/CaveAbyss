package ru.givler.caveabyss.block;

import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;
import ru.givler.caveabyss.integration.mbo.DripstoneCauldrons;
import sun.misc.Unsafe;

/** Exercises actual block ticks against a small in-memory world at negative Y. */
public final class DripstoneSmoke {
    private static final Random ALWAYS = new Random() {
        @Override public int nextInt(int bound) { return 0; }
        @Override public float nextFloat() { return 0; }
        @Override public boolean nextBoolean() { return true; }
    };

    public static void check() throws Exception {
        Field access = Unsafe.class.getDeclaredField("theUnsafe"); access.setAccessible(true);
        Unsafe unsafe = (Unsafe)access.get(null);
        String[] names = {"air", "water", "flowing_water", "lava", "flowing_lava", "cauldron", "stone"};
        Object[] saved = new Object[names.length];
        Block oldPoint = CaveBlocks.pointedDripstone, oldRock = CaveBlocks.dripstoneBlock;
        try {
            for (int i = 0; i < names.length; i++) {
                Field field = Blocks.class.getDeclaredField(names[i]);
                saved[i] = unsafe.getObject(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field));
                Material material = i == 0 ? Material.air : i <= 2 ? Material.water : i <= 4 ? Material.lava : Material.rock;
                unsafe.putObject(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field), new Block(material) { });
            }
            BlockPointedDripstone point = new BlockPointedDripstone();
            CaveBlocks.pointedDripstone = point;
            CaveBlocks.dripstoneBlock = Blocks.stone;
            TestWorld world = (TestWorld)unsafe.allocateInstance(TestWorld.class);
            Field provider = World.class.getDeclaredField("provider"); provider.setAccessible(true);
            provider.set(world, new WorldProviderSurface());
            world.blocks = new HashMap<String, Block>();
            world.metadata = new HashMap<String, Integer>();
            world.falling = new ArrayList<Entity>();
            reset(world, point);
            point.randomDisplayTick(world, 0, -20, 0, ALWAYS);
            if (!"dripWater".equals(world.lastParticle) || world.lastParticleY >= -20
                    || !world.isAirBlock(0, (int)Math.floor(world.lastParticleY), 0))
                throw new AssertionError("Water particle spawned inside solid dripstone and would die immediately");
            reset(world, point);
            world.put(-18, Blocks.lava, 0);
            point.randomDisplayTick(world, 0, -20, 0, ALWAYS);
            if (!"dripLava".equals(world.lastParticle) || world.lastParticleY >= -20)
                throw new AssertionError("Lava particle did not spawn below the tip");
            reset(world, point);
            world.put(-21, point, 0);
            point.randomDisplayTick(world, 0, -20, 0, ALWAYS);
            if (world.lastParticle != null) throw new AssertionError("Merged tips emitted particles");
            reset(world, point);
            point.updateTick(world, 0, -20, 0, ALWAYS);
            if (world.getBlockMetadata(0, -26, 0) != 1 || world.getBlock(0, -21, 0) != point)
                throw new AssertionError("Water drip did not fill cauldron and grow stalactite below zero");
            reset(world, point);
            world.put(-23, Blocks.stone, 0);
            point.updateTick(world, 0, -20, 0, ALWAYS);
            if (world.getBlockMetadata(0, -26, 0) != 0)
                throw new AssertionError("Drip passed through a solid obstruction");
            reset(world, point);
            world.put(-18, Blocks.water, 1);
            point.updateTick(world, 0, -20, 0, ALWAYS);
            if (world.getBlockMetadata(0, -26, 0) != 0 || world.getBlock(0, -21, 0) != Blocks.air)
                throw new AssertionError("Flowing liquid was treated as a source");
            reset(world, point);
            world.put(-18, Blocks.lava, 0);
            point.updateTick(world, 0, -20, 0, ALWAYS);
            if (world.getBlock(0, -21, 0) != Blocks.air || world.getBlockMetadata(0, -26, 0) != 0)
                throw new AssertionError("Lava grew dripstone or filled a vanilla cauldron without MBO");
            reset(world, point);
            world.put(-21, point, 8);
            point.updateTick(world, 0, -21, 0, ALWAYS);
            if (world.getBlockMetadata(0, -26, 0) != 0)
                throw new AssertionError("Non-root segment multiplied the drip rate");
            world.put(-19, Blocks.air, 0);
            point.onNeighborBlockChange(world, 0, -20, 0, Blocks.air);
            point.onNeighborBlockChange(world, 0, -21, 0, Blocks.air);
            if (world.falling.size() != 2 || world.getBlock(0, -20, 0) != Blocks.air
                    || world.getBlock(0, -21, 0) != Blocks.air)
                throw new AssertionError("Detached stalactite chain did not fall");
            // Opposing tips form a column and must not drip through one another.
            reset(world, point);
            world.put(-21, point, 0);
            point.updateTick(world, 0, -20, 0, ALWAYS);
            if (world.getBlockMetadata(0, -26, 0) != 0)
                throw new AssertionError("Merged dripstone tips continued dripping");
            checkCauldronFluids();
            checkMboConnected(world, unsafe);
        } finally {
            CaveBlocks.pointedDripstone = oldPoint;
            CaveBlocks.dripstoneBlock = oldRock;
            for (int i = 0; i < names.length; i++) {
                Field field = Blocks.class.getDeclaredField(names[i]);
                unsafe.putObject(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field), saved[i]);
            }
        }
        System.out.println("Dripstone sources, blocked drips, growth, chain collapse and cauldron fluids passed");
    }

    private static void checkCauldronFluids() {
        if (DripstoneCauldrons.filledMetadata(0, true, true) != 7
                || DripstoneCauldrons.filledMetadata(0, true, false) != 0)
            throw new AssertionError("MBO lava cauldron encoding or optional fallback failed");
        for (int meta = 0; meta < 8; meta++) {
            int water = DripstoneCauldrons.filledMetadata(meta, false, true);
            int lava = DripstoneCauldrons.filledMetadata(meta, true, true);
            if ((meta & 3) > 0 && ((meta & 4) != 0 ? water != meta : lava != meta))
                throw new AssertionError("Drips mixed lava and water");
            if ((meta & 3) == 3 && (water != meta || lava != meta))
                throw new AssertionError("Cauldron overflowed");
        }
    }

    private static void reset(TestWorld world, Block point) {
        world.blocks.clear(); world.metadata.clear(); world.falling.clear();
        world.lastParticle = null;
        world.put(-18, Blocks.water, 0);
        world.put(-19, Blocks.stone, 0);
        world.put(-20, point, 8);
        world.put(-26, Blocks.cauldron, 0);
    }

    private static void checkMboConnected(TestWorld world, Unsafe unsafe) throws Exception {
        Class<?> hooks = Class.forName("ru.givler.mbo.core.CauldronHooks");
        Field connected = DripstoneCauldrons.class.getDeclaredField("connectedMetadata");
        Field normalize = DripstoneCauldrons.class.getDeclaredField("normalize");
        connected.setAccessible(true); normalize.setAccessible(true);
        Object oldConnected = connected.get(null), oldNormalize = normalize.get(null);
        Field cauldron = Blocks.class.getDeclaredField("cauldron");
        Object oldCauldron = unsafe.getObject(unsafe.staticFieldBase(cauldron), unsafe.staticFieldOffset(cauldron));
        Constructor<?> ctor = net.minecraft.block.BlockCauldron.class.getDeclaredConstructor(); ctor.setAccessible(true);
        try {
            unsafe.putObject(unsafe.staticFieldBase(cauldron), unsafe.staticFieldOffset(cauldron), ctor.newInstance());
            connected.set(null, hooks.getMethod("getConnectedMetadata", net.minecraft.world.IBlockAccess.class,
                    int.class, int.class, int.class));
            normalize.set(null, hooks.getMethod("normalizeConnected", World.class, int.class, int.class, int.class));
            world.blocks.clear(); world.metadata.clear();
            world.put(-26, Blocks.cauldron, 0);
            world.setBlock(1, -26, 0, Blocks.cauldron, 0, 3);
            DripstoneCauldrons.fill(world, 0, -26, 0, true);
            if (world.getBlockMetadata(0, -26, 0) != 7 || world.getBlockMetadata(1, -26, 0) != 7)
                throw new AssertionError("Drip did not fill connected MBO lava cauldrons");
            DripstoneCauldrons.fill(world, 0, -26, 0, false);
            if (world.getBlockMetadata(0, -26, 0) != 7)
                throw new AssertionError("Water replaced connected lava");
            world.setBlockMetadataWithNotify(0, -26, 0, 0, 3);
            world.setBlockMetadataWithNotify(1, -26, 0, 2, 3);
            DripstoneCauldrons.fill(world, 0, -26, 0, true);
            if (world.getBlockMetadata(1, -26, 0) != 2 || world.getBlockMetadata(0, -26, 0) != 0)
                throw new AssertionError("Lava mixed with water through an empty cauldron");
            DripstoneCauldrons.fill(world, 0, -26, 0, false);
            if (world.getBlockMetadata(0, -26, 0) != 3 || world.getBlockMetadata(1, -26, 0) != 3)
                throw new AssertionError("Water drip did not synchronize MBO cauldrons");
        } finally {
            connected.set(null, oldConnected); normalize.set(null, oldNormalize);
            unsafe.putObject(unsafe.staticFieldBase(cauldron), unsafe.staticFieldOffset(cauldron), oldCauldron);
        }
    }

    public static final class TestWorld extends WorldServer {
        private TestWorld() { super(null, null, "test", 0, null, null); }
        Map<String, Block> blocks;
        Map<String, Integer> metadata;
        List<Entity> falling;
        String lastParticle;
        double lastParticleY;
        @Override public void spawnParticle(String name, double x, double y, double z, double vx, double vy, double vz) {
            lastParticle = name; lastParticleY = y;
        }
        private String key(int x, int y, int z) { return x + ":" + y + ":" + z; }
        void put(int y, Block block, int meta) { setBlock(0, y, 0, block, meta, 3); }
        @Override public Block getBlock(int x, int y, int z) {
            String key = key(x, y, z); return blocks.containsKey(key) ? blocks.get(key) : Blocks.air;
        }
        @Override public int getBlockMetadata(int x, int y, int z) {
            String key = key(x, y, z); return metadata.containsKey(key) ? metadata.get(key) : 0;
        }
        @Override public boolean isAirBlock(int x, int y, int z) { return getBlock(x, y, z) == Blocks.air; }
        @Override public boolean setBlock(int x, int y, int z, Block block, int meta, int flags) {
            String key = key(x, y, z); blocks.put(key, block); metadata.put(key, meta); return true;
        }
        @Override public boolean setBlockToAir(int x, int y, int z) { put(y, Blocks.air, 0); return true; }
        @Override public boolean setBlockMetadataWithNotify(int x, int y, int z, int meta, int flags) {
            metadata.put(key(x, y, z), meta); return true;
        }
        @Override public boolean isSideSolid(int x, int y, int z, ForgeDirection side) { return getBlock(x, y, z) == Blocks.stone; }
        @Override public boolean spawnEntityInWorld(Entity entity) { falling.add(entity); return true; }
        @Override public void markBlockRangeForRenderUpdate(int x, int y, int z, int xx, int yy, int zz) { }
        @Override public void func_147453_f(int x, int y, int z, Block block) { }
    }
}
