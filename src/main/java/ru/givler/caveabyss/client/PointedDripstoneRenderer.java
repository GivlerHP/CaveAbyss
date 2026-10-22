package ru.givler.caveabyss.client;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.GL11;
import ru.givler.caveabyss.block.BlockPointedDripstone;

/** Crossed planes, with thickness sprites selected from the entire formation. */
public final class PointedDripstoneRenderer implements ISimpleBlockRenderingHandler {
    private final int id;
    private PointedDripstoneRenderer(int id) { this.id = id; }

    public static void register() {
        int id = RenderingRegistry.getNextAvailableRenderId();
        BlockPointedDripstone.setRenderId(id);
        RenderingRegistry.registerBlockHandler(new PointedDripstoneRenderer(id));
    }

    @Override public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z,
                                              Block block, int modelId, RenderBlocks renderer) {
        Tessellator tess = Tessellator.instance;
        tess.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        tess.setColorOpaque_F(1, 1, 1);
        renderer.drawCrossedSquares(renderer.hasOverrideBlockTexture() ? renderer.overrideBlockTexture
                : block.getIcon(world, x, y, z, 0), x, y, z, 1);
        return true;
    }

    @Override public void renderInventoryBlock(Block block, int meta, int modelId, RenderBlocks renderer) {
        GL11.glPushMatrix();
        GL11.glTranslatef(-.5F, -.5F, -.5F);
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.setNormal(0, 1, 0);
        tess.setColorOpaque_F(1, 1, 1);
        renderer.drawCrossedSquares(block.getIcon(0, meta), 0, 0, 0, 1);
        tess.draw();
        GL11.glPopMatrix();
    }

    @Override public boolean shouldRender3DInInventory(int modelId) { return true; }
    @Override public int getRenderId() { return id; }
}
