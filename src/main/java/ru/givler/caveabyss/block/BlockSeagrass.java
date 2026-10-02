package ru.givler.caveabyss.block;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.world.World;

/** Water-filled seagrass; metadata selects an animated variant or a tall half. */
public final class BlockSeagrass extends Block {
    private static final String[] VARIANTS = {"seagrass", "seagrass1", "seagrass2"};
    private final boolean tall;
    private IIcon[] icons;
    private static int renderId = 1;

    public static void setRenderId(int id) { renderId = id; }

    public BlockSeagrass(boolean tall) {
        super(Material.water);
        this.tall = tall;
        setBlockName(tall ? "tall_seagrass" : "seagrass");
        setHardness(0.0F);
        setStepSound(soundTypeGrass);
        setCreativeTab(CreativeTabs.tabDecorations);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        icons = new IIcon[tall ? 2 : VARIANTS.length];
        for (int i = 0; i < icons.length; i++)
            icons[i] = register.registerIcon("caveabyss:" + (tall
                    ? (i == 0 ? "tall_seagrass_bottom" : "tall_seagrass_top") : VARIANTS[i]));
        blockIcon = icons[0];
    }

    @Override
    public IIcon getIcon(int side, int metadata) {
        return icons == null ? blockIcon : icons[Math.max(0, Math.min(metadata & 7, icons.length - 1))];
    }

    @Override public int getRenderType() { return renderId; }
    @Override public int getRenderBlockPass() { return 1; }
    @Override public boolean isOpaqueCube() { return false; }
    @Override public boolean renderAsNormalBlock() { return false; }
    @Override public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) { return null; }
    @Override public boolean isReplaceable(net.minecraft.world.IBlockAccess world, int x, int y, int z) { return false; }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        if (!sourceWater(world, x, y, z)) return false;
        if (world.getBlock(x, y - 1, z) == CaveBlocks.tallSeagrass) return false;
        if (world.getBlock(x, y - 1, z) == CaveBlocks.seagrass) return true;
        return (!tall || sourceWater(world, x, y + 1, z))
                && world.getBlock(x, y - 1, z).isSideSolid(world, x, y - 1, z,
                        net.minecraftforge.common.util.ForgeDirection.UP);
    }

    private static boolean sourceWater(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return (block == Blocks.water || block == Blocks.flowing_water)
                && world.getBlockMetadata(x, y, z) == 0;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        if (world.getBlock(x, y - 1, z) == CaveBlocks.seagrass) {
            world.setBlock(x, y, z, CaveBlocks.tallSeagrass, 9, 2);
            world.setBlock(x, y - 1, z, CaveBlocks.tallSeagrass, 8, 3);
        } else if (tall && sourceWater(world, x, y + 1, z)) {
            world.setBlock(x, y + 1, z, this, 9, 3);
        }
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        if (tall) {
            int metadata = world.getBlockMetadata(x, y, z) & 7;
            if (metadata == 0)
                return world.getBlock(x, y - 1, z).isSideSolid(world, x, y - 1, z,
                        net.minecraftforge.common.util.ForgeDirection.UP);
            return world.getBlock(x, y - 1, z) == this
                    && (world.getBlockMetadata(x, y - 1, z) & 7) == 0;
        }
        return world.getBlock(x, y - 1, z).isSideSolid(world, x, y - 1, z,
                net.minecraftforge.common.util.ForgeDirection.UP);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        int metadata = world.getBlockMetadata(x, y, z);
        if (metadata < 8) world.setBlockMetadataWithNotify(x, y, z, metadata + 8, 2);
        if (!canBlockStay(world, x, y, z)) {
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlock(x, y, z, Blocks.water, 0, 3);
        } else if (tall && (world.getBlockMetadata(x, y, z) & 7) == 0
                && world.getBlock(x, y + 1, z) != this) {
            world.setBlock(x, y, z, CaveBlocks.seagrass, 8, 3);
        }
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z,
                                   boolean willHarvest) {
        return world.setBlock(x, y, z, Blocks.water, 0, 3);
    }

    @Override public int damageDropped(int metadata) { return tall ? 0 : metadata & 7; }
    @Override public int quantityDropped(int metadata, int fortune, java.util.Random random) {
        return tall && (metadata & 7) != 0 ? 0 : 1;
    }
    @Override public Item getItemDropped(int metadata, java.util.Random random, int fortune) {
        return Item.getItemFromBlock(this);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (int i = 0; i < (tall ? 1 : VARIANTS.length); i++)
            list.add(new ItemStack(item, 1, i));
    }
}
