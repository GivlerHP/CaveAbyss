package ru.givler.caveabyss.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** Metadata 0..5 is the outward-facing side, as in vanilla's six directions. */
public final class BlockAmethystBud extends Block {
    private static int renderId = -1;
    private static final int[] DX = {0, 0, 0, 0, -1, 1};
    private static final int[] DY = {-1, 1, 0, 0, 0, 0};
    private static final int[] DZ = {0, 0, -1, 1, 0, 0};
    private final int stage;

    public static void setRenderId(int id) {
        renderId = id;
    }

    public int getStage() {
        return stage;
    }

    public BlockAmethystBud(String name, int stage) {
        super(Material.glass);
        this.stage = stage;
        setBlockName(name);
        setBlockTextureName("caveabyss:amethyst_cluster");
        setHardness(1.5F);
        setResistance(1.5F);
        setStepSound(soundTypeGlass);
        setLightLevel(stage == 3 ? 0.3125F : stage == 2 ? 0.25F : stage == 1 ? 0.125F : 0.0625F);
        setHarvestLevel("pickaxe", 0);
        bounds(1);
    }

    private void bounds(int face) {
        float half = (stage == 0 ? 5 : stage == 1 ? 6 : stage == 2 ? 7 : 8) / 16F;
        float length = (stage == 0 ? 6 : stage == 1 ? 10 : stage == 2 ? 13 : 16) / 16F;
        float lo = 0.5F - half, hi = 0.5F + half;
        if (face == 0) setBlockBounds(lo, 1 - length, lo, hi, 1, hi);
        else if (face == 1) setBlockBounds(lo, 0, lo, hi, length, hi);
        else if (face == 2) setBlockBounds(lo, lo, 1 - length, hi, hi, 1);
        else if (face == 3) setBlockBounds(lo, lo, 0, hi, hi, length);
        else if (face == 4) setBlockBounds(1 - length, lo, lo, 1, hi, hi);
        else setBlockBounds(0, lo, lo, length, hi, hi);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        bounds(world.getBlockMetadata(x, y, z));
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public boolean isOpaqueCube() { return false; }

    @Override
    public boolean renderAsNormalBlock() { return false; }

    @Override
    public int getRenderType() { return renderId; }

    @Override
    public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side) {
        return supported(world, x, y, z, side);
    }

    private boolean supported(World world, int x, int y, int z, int side) {
        return side >= 0 && side < 6 && world.getBlock(x - DX[side], y - DY[side], z - DZ[side])
                == CaveBlocks.buddingAmethyst;
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side,
                             float hitX, float hitY, float hitZ, int metadata) {
        return side;
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        return supported(world, x, y, z, world.getBlockMetadata(x, y, z));
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!canBlockStay(world, x, y, z)) {
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlockToAir(x, y, z);
        }
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return stage == 3 ? CaveBlocks.amethystShard : null;
    }

    @Override
    public int quantityDropped(int metadata, int fortune, Random random) {
        return stage == 3 ? 2 : 0;
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int metadata) {
        ItemStack tool = player.getCurrentEquippedItem();
        boolean silk = tool != null && net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(
                net.minecraft.enchantment.Enchantment.silkTouch.effectId, tool) > 0;
        if (silk) {
            spawnDrop(world, x, y, z, new ItemStack(this));
            return;
        }
        if (stage == 3 && tool != null && tool.getItem().getHarvestLevel(tool, "pickaxe") >= 0) {
            int fortune = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(
                    net.minecraft.enchantment.Enchantment.fortune.effectId, tool);
            int count = 4 + (fortune > 0 ? randomFortune(world.rand, fortune) : 0);
            spawnDrop(world, x, y, z, new ItemStack(CaveBlocks.amethystShard, count));
        } else {
            super.harvestBlock(world, player, x, y, z, metadata);
        }
    }

    private int randomFortune(Random random, int fortune) {
        return 4 * random.nextInt(fortune + 1);
    }

    private void spawnDrop(World world, int x, int y, int z, ItemStack stack) {
        if (!world.isRemote)
            world.spawnEntityInWorld(new net.minecraft.entity.item.EntityItem(
                    world, x + 0.5, y + 0.5, z + 0.5, stack));
    }
}
