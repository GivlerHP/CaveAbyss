package ru.givler.caveabyss.block;

import java.util.List;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/** A water-filled kelp column. Metadata 0/1 is a tip variant; 2 is a stem. */
public final class BlockKelp extends Block {
    private IIcon[] icons;
    private static int renderId = 1;

    public static void setRenderId(int id) { renderId = id; }

    public BlockKelp() {
        super(Material.water);
        setBlockName("kelp");
        setHardness(0.0F);
        setStepSound(soundTypeGrass);
        setCreativeTab(CreativeTabs.tabDecorations);
        setTickRandomly(true);
        setBlockBounds(0, 0, 0, 1, 1, 1);
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        icons = new IIcon[] {register.registerIcon("caveabyss:kelp"),
                register.registerIcon("caveabyss:kelp1"),
                register.registerIcon("caveabyss:kelp_plant")};
        blockIcon = icons[0];
    }

    @Override public IIcon getIcon(int side, int metadata) {
        return icons == null ? blockIcon : icons[Math.max(0, Math.min(metadata & 7, 2))];
    }
    @Override public int getRenderType() { return renderId; }
    @Override public int getRenderBlockPass() { return 1; }
    @Override public boolean isOpaqueCube() { return false; }
    @Override public boolean renderAsNormalBlock() { return false; }
    @Override public boolean isReplaceable(IBlockAccess world, int x, int y, int z) { return false; }
    @Override public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) { return null; }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return sourceWater(world, x, y, z) && supported(world, x, y, z);
    }

    private boolean supported(World world, int x, int y, int z) {
        Block below = world.getBlock(x, y - 1, z);
        return below == this || below.isSideSolid(world, x, y - 1, z, ForgeDirection.UP);
    }

    private static boolean sourceWater(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return (block == Blocks.water || block == Blocks.flowing_water)
                && world.getBlockMetadata(x, y, z) == 0;
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        int metadata = world.getBlockMetadata(x, y, z);
        if (metadata < 8) world.setBlockMetadataWithNotify(x, y, z, metadata + 8, 2);
        if (world.getBlock(x, y - 1, z) == this)
            world.setBlockMetadataWithNotify(x, y - 1, z, 10, 3);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (world.isRemote || (world.getBlockMetadata(x, y, z) & 7) == 2
                || !sourceWater(world, x, y + 1, z) || random.nextInt(10) != 0) return;
        int height = 1;
        while (height < 16 && world.getBlock(x, y - height, z) == this) height++;
        if (height < 16) world.setBlock(x, y + 1, z, this, 8 + random.nextInt(2), 3);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        int metadata = world.getBlockMetadata(x, y, z);
        if (metadata < 8) world.setBlockMetadataWithNotify(x, y, z, metadata + 8, 2);
        if (!supported(world, x, y, z)) {
            dropBlockAsItem(world, x, y, z, 0, 0);
            world.setBlock(x, y, z, Blocks.water, 0, 3);
        } else if ((world.getBlockMetadata(x, y, z) & 7) == 2
                && world.getBlock(x, y + 1, z) != this) {
            world.setBlockMetadataWithNotify(x, y, z, 8 + world.rand.nextInt(2), 3);
        }
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z,
                                   boolean willHarvest) {
        return world.setBlock(x, y, z, Blocks.water, 0, 3);
    }

    @Override public int damageDropped(int metadata) { return 0; }
    @Override public Item getItemDropped(int metadata, Random random, int fortune) {
        return Item.getItemFromBlock(this);
    }
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, 0));
    }
}
