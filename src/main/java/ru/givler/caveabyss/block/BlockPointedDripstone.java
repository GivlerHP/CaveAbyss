package ru.givler.caveabyss.block;

import java.util.Random;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import ru.givler.caveabyss.api.CaveAbyssAPI;
import ru.givler.caveabyss.entity.EntityFallingDripstone;
import ru.givler.caveabyss.integration.mbo.DripstoneCauldrons;

/** Metadata bit 3: hanging. Thickness is derived from neighbours, never saved. */
public final class BlockPointedDripstone extends BlockFalling {
    private static int renderId = 1;
    private final IIcon[][] icons = new IIcon[2][5];

    public BlockPointedDripstone() {
        super(Material.rock);
        setBlockName("pointed_dripstone");
        setBlockTextureName("caveabyss:pointed_dripstone_up_tip");
        setHardness(1.5F);
        setResistance(3F);
        setStepSound(soundTypeStone);
        setTickRandomly(true);
        setCreativeTab(net.minecraft.creativetab.CreativeTabs.tabDecorations);
        setBlockBounds(.3125F, 0, .3125F, .6875F, 1, .6875F);
    }

    @Override public boolean isOpaqueCube() { return false; }
    @Override public boolean renderAsNormalBlock() { return false; }
    public static void setRenderId(int id) { renderId = id; }
    @Override public int getRenderType() { return renderId; }
    @Override public int damageDropped(int meta) { return 0; }
    @Override public boolean canHarvestBlock(net.minecraft.entity.player.EntityPlayer player, int meta) { return true; }
    @Override public int getMobilityFlag() { return 1; }

    @Override @SideOnly(Side.CLIENT) public void registerBlockIcons(IIconRegister register) {
        String[] shapes = {"tip", "tip_merge", "frustum", "middle", "base"};
        for (int d = 0; d < 2; d++) for (int s = 0; s < shapes.length; s++)
            icons[d][s] = register.registerIcon("caveabyss:pointed_dripstone_"
                    + (d == 0 ? "up_" : "down_") + shapes[s]);
        blockIcon = icons[0][0];
    }

    @Override @SideOnly(Side.CLIENT) public IIcon getIcon(int side, int meta) { return icons[(meta & 8) == 0 ? 0 : 1][0]; }

    @Override @SideOnly(Side.CLIENT) public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        int direction = (world.getBlockMetadata(x, y, z) & 8) == 0 ? 1 : -1;
        boolean next = same(world, x, y + direction, z, direction);
        int shape;
        if (!next) shape = world.getBlock(x, y + direction, z) == this ? 1 : 0;
        else if (!same(world, x, y + direction * 2, z, direction)) shape = 2;
        else shape = same(world, x, y - direction, z, direction) ? 3 : 4;
        return icons[direction == 1 ? 0 : 1][shape];
    }

    public boolean same(IBlockAccess world, int x, int y, int z, int direction) {
        return world.getBlock(x, y, z) == this
                && ((world.getBlockMetadata(x, y, z) & 8) == 0 ? 1 : -1) == direction;
    }

    private boolean supports(World world, int x, int y, int z, int direction) {
        if (y < CaveAbyssAPI.getMinY(world) || y > 255) return false;
        return same(world, x, y, z, direction)
                || world.isSideSolid(x, y, z, direction == 1
                ? net.minecraftforge.common.util.ForgeDirection.UP : net.minecraftforge.common.util.ForgeDirection.DOWN);
    }

    @Override public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side) {
        return side == 0 && supports(world, x, y + 1, z, -1)
                || side == 1 && supports(world, x, y - 1, z, 1);
    }

    @Override public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return canPlaceBlockOnSide(world, x, y, z, 0) || canPlaceBlockOnSide(world, x, y, z, 1);
    }

    @Override public int onBlockPlaced(World world, int x, int y, int z, int side,
                                      float hitX, float hitY, float hitZ, int meta) { return side == 0 ? 8 : 0; }

    @Override public void onBlockAdded(World world, int x, int y, int z) {
        world.scheduleBlockUpdate(x, y, z, this, 2);
    }

    @Override public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
        world.markBlockRangeForRenderUpdate(x, y - 2, z, x, y + 2, z);
        if (!world.isRemote) checkSupport(world, x, y, z);
    }

    private boolean checkSupport(World world, int x, int y, int z) {
        int direction = (world.getBlockMetadata(x, y, z) & 8) == 0 ? 1 : -1;
        // Walk to the real anchor so a detached chain cannot support itself.
        int anchor = y - direction;
        while (anchor >= CaveAbyssAPI.getMinY(world) && anchor <= 255
                && same(world, x, anchor, z, direction)) anchor -= direction;
        if (!supports(world, x, anchor, z, direction)) {
            if (direction < 0) {
                world.setBlockToAir(x, y, z);
                world.spawnEntityInWorld(new EntityFallingDripstone(world, x + .5, y + .5, z + .5));
            } else {
                dropBlockAsItem(world, x, y, z, 0, 0);
                world.setBlockToAir(x, y, z);
            }
            return false;
        }
        return true;
    }

    @Override public void updateTick(World world, int x, int y, int z, Random random) {
        if (world.isRemote || !checkSupport(world, x, y, z)) return;
        int direction = (world.getBlockMetadata(x, y, z) & 8) == 0 ? 1 : -1;
        // Only the top segment processes random ticks. One long stalactite must
        // not fill a cauldron faster than a short one.
        if (direction > 0 || same(world, x, y + 1, z, -1)) return;
        int tip = y;
        while (tip > CaveAbyssAPI.getMinY(world) && same(world, x, tip - 1, z, -1)) tip--;
        if (y - tip >= 10 || world.getBlock(x, tip - 1, z) == this) return;
        Block source = world.getBlock(x, y + 2, z);
        boolean water = source == Blocks.water || source == Blocks.flowing_water;
        boolean lava = source == Blocks.lava || source == Blocks.flowing_lava;
        if ((!water && !lava) || world.getBlockMetadata(x, y + 2, z) != 0) return;
        if (random.nextFloat() < (water ? .17578125F : .05859375F))
            fillCauldron(world, x, tip, z, lava);
        if (water && world.getBlock(x, y + 1, z) == CaveBlocks.dripstoneBlock
                && random.nextFloat() < .011377778F) grow(world, x, tip, z, random);
    }

    private void fillCauldron(World world, int x, int tip, int z, boolean lava) {
        for (int y = tip - 1; y >= Math.max(CaveAbyssAPI.getMinY(world), tip - 10); y--) {
            Block block = world.getBlock(x, y, z);
            if (block == Blocks.cauldron) {
                DripstoneCauldrons.fill(world, x, y, z, lava);
                return;
            }
            if (!world.isAirBlock(x, y, z)) return;
        }
    }

    private void grow(World world, int x, int tip, int z, Random random) {
        int min = CaveAbyssAPI.getMinY(world);
        if (tip <= min || !world.isAirBlock(x, tip - 1, z)) return;
        if (random.nextBoolean()) {
            world.setBlock(x, tip - 1, z, this, 8, 3);
            return;
        }
        for (int y = tip - 2; y >= Math.max(min, tip - 10); y--) {
            if (world.isAirBlock(x, y, z)) continue;
            if ((same(world, x, y, z, 1) || supports(world, x, y, z, 1))
                    && world.isAirBlock(x, y + 1, z)) world.setBlock(x, y + 1, z, this, 0, 3);
            return;
        }
    }

    @Override public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (!same(world, x, y, z, -1) || !world.isAirBlock(x, y - 1, z) || random.nextInt(3) != 0) return;
        int root = y;
        while (root < Math.min(254, y + 10) && same(world, x, root + 1, z, -1)) root++;
        Block source = world.getBlock(x, root + 2, z);
        if (source.getMaterial() == Material.lava)
            world.spawnParticle("dripLava", x + .5, y - .05, z + .5, 0, 0, 0);
        else if (source.getMaterial() == Material.water || random.nextInt(3) == 0)
            // EntityDropParticleFX kills itself inside any solid material, even
            // a narrow non-cube. Spawn in the air BELOW the tip, not inside it.
            world.spawnParticle("dripWater", x + .5, y - .05, z + .5, 0, 0, 0);
    }

    @Override public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return AxisAlignedBB.getBoundingBox(x + .3125, y, z + .3125, x + .6875, y + 1, z + .6875);
    }

    /** Falling stalactites shatter rather than becoming hanging blocks on the floor. */
    @Override public void func_149828_a(World world, int x, int y, int z, int meta) {
        world.setBlockToAir(x, y, z);
        if (world.getGameRules().getGameRuleBooleanValue("doTileDrops")) dropBlockAsItem(world, x, y, z, 0, 0);
    }
}
