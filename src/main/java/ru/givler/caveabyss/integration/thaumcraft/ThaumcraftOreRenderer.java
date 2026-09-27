package ru.givler.caveabyss.integration.thaumcraft;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.renderers.block.BlockRenderer;

/** Stone base plus the colored crystal layer, as in Thaumcraft's ore renderer. */
public final class ThaumcraftOreRenderer extends BlockRenderer implements ISimpleBlockRenderingHandler {
    private final int renderId;

    private ThaumcraftOreRenderer(int renderId) {
        this.renderId = renderId;
    }

    public static void register() {
        int id = RenderingRegistry.getNextAvailableRenderId();
        BlockThaumcraftDeepslateOre.setRenderId(id);
        RenderingRegistry.registerBlockHandler(new ThaumcraftOreRenderer(id));
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        BlockThaumcraftDeepslateOre ore = (BlockThaumcraftDeepslateOre) block;
        block.setBlockBounds(0, 0, 0, 1, 1, 1);
        renderer.setRenderBoundsFromBlock(block);
        drawFaces(renderer, block, block.getIcon(0, metadata), false);
        if (metadata >= 1 && metadata <= 6) {
            int color = BlockThaumcraftDeepslateOre.INFUSED_COLORS[metadata];
            GL11.glColor3f((color >> 16 & 255) / 255F,
                    (color >> 8 & 255) / 255F, (color & 255) / 255F);
            block.setBlockBounds(-0.002F, -0.002F, -0.002F, 1.002F, 1.002F, 1.002F);
            renderer.setRenderBoundsFromBlock(block);
            drawFaces(renderer, block, ore.getInfusedCracks(), false);
            GL11.glColor3f(1, 1, 1);
            block.setBlockBounds(0, 0, 0, 1, 1, 1);
            renderer.setRenderBoundsFromBlock(block);
        }
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z,
                                    Block block, int modelId, RenderBlocks renderer) {
        int metadata = world.getBlockMetadata(x, y, z);
        int brightness = setBrightness(world, x, y, z, block);
        block.setBlockBounds(0, 0, 0, 1, 1, 1);
        renderer.setRenderBoundsFromBlock(block);
        renderer.renderStandardBlock(block, x, y, z);
        if (metadata >= 1 && metadata <= 6) {
            Tessellator tessellator = Tessellator.instance;
            tessellator.setColorOpaque_I(BlockThaumcraftDeepslateOre.INFUSED_COLORS[metadata]);
            tessellator.setBrightness(Math.max(brightness, 160));
            renderAllSides(world, x, y, z, block, renderer,
                    ((BlockThaumcraftDeepslateOre) block).getInfusedCracks(), false);
        }
        renderer.clearOverrideBlockTexture();
        return true;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    @Override
    public int getRenderId() {
        return renderId;
    }
}
