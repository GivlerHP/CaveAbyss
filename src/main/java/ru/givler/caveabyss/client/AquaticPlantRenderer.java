package ru.givler.caveabyss.client;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.GL11;
import ru.givler.caveabyss.block.BlockKelp;
import ru.givler.caveabyss.block.BlockSeagrass;

/** Draw the ordinary water cell and the crossed plant in the same translucent pass. */
public final class AquaticPlantRenderer implements ISimpleBlockRenderingHandler {
    private final int renderId;

    private AquaticPlantRenderer(int renderId) { this.renderId = renderId; }

    public static void register() {
        int id = RenderingRegistry.getNextAvailableRenderId();
        BlockSeagrass.setRenderId(id);
        BlockKelp.setRenderId(id);
        RenderingRegistry.registerBlockHandler(new AquaticPlantRenderer(id));
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z,
                                    Block block, int modelId, RenderBlocks renderer) {
        IBlockAccess original = renderer.blockAccess;
        renderer.blockAccess = new SourceWaterView(original);
        try {
            renderer.renderBlockLiquid(Blocks.water, x, y, z);
        } finally {
            renderer.blockAccess = original;
        }
        int metadata = world.getBlockMetadata(x, y, z);
        if (block instanceof BlockKelp && world.getBlock(x, y + 1, z) == block)
            metadata = 10;
        drawPlant(Tessellator.instance, block.getIcon(0, metadata),
                x, y, z, block.getMixedBrightnessForBlock(world, x, y, z));
        return true;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        drawPlant(tess, block.getIcon(0, metadata), 0, 0, 0, 0xF000F0);
        tess.draw();
        GL11.glPopMatrix();
    }

    private static void drawPlant(Tessellator tess, IIcon icon, double x, double y, double z,
                                  int brightness) {
        tess.setBrightness(brightness);
        tess.setColorOpaque_F(1, 1, 1);
        double u0 = icon.getMinU(), u1 = icon.getMaxU();
        double v0 = icon.getMinV(), v1 = icon.getMaxV();
        for (int diagonal = 0; diagonal < 2; diagonal++) {
            double ax = x + (diagonal == 0 ? 0 : 1);
            double az = z;
            double bx = x + (diagonal == 0 ? 1 : 0);
            double bz = z + 1;
            tess.addVertexWithUV(ax, y, az, u0, v1);
            tess.addVertexWithUV(bx, y, bz, u1, v1);
            tess.addVertexWithUV(bx, y + 1, bz, u1, v0);
            tess.addVertexWithUV(ax, y + 1, az, u0, v0);
            tess.addVertexWithUV(ax, y + 1, az, u0, v0);
            tess.addVertexWithUV(bx, y + 1, bz, u1, v0);
            tess.addVertexWithUV(bx, y, bz, u1, v1);
            tess.addVertexWithUV(ax, y, az, u0, v1);
        }
    }

    @Override public boolean shouldRender3DInInventory(int modelId) { return true; }
    @Override public int getRenderId() { return renderId; }
}
