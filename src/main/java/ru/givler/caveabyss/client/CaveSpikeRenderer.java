package ru.givler.caveabyss.client;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import ru.givler.caveabyss.block.BlockCaveSpike;

/** Frusta taper toward the tip, with opposite orientation on the ceiling. */
public final class CaveSpikeRenderer implements ISimpleBlockRenderingHandler {
    public static void register() {
        BlockCaveSpike.renderId = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new CaveSpikeRenderer());
    }
    @Override public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z,
                                               Block block, int modelId, RenderBlocks renderer) {
        Tessellator t = Tessellator.instance;
        t.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        draw(t, block.getIcon(0, 0), x, y, z, world.getBlockMetadata(x, y, z));
        return true;
    }
    private static void draw(Tessellator t, IIcon icon, double x, double y, double z, int meta) {
        double base = 0.44 - (meta & 3) * 0.105;
        double tip = Math.max(0.02, base - 0.105);
        double bottom = (meta & 4) == 0 ? base : tip, top = (meta & 4) == 0 ? tip : base;
        double[][] points = {{-1,-1}, {-1,1}, {1,1}, {1,-1}};
        for (int side = 0; side < 4; side++) {
            double[] a = points[side], b = points[(side + 1) & 3];
            float light = side % 2 == 0 ? 0.8F : 0.65F;
            t.setColorOpaque_F(light, light, light);
            t.addVertexWithUV(x + .5 + a[0]*bottom, y, z + .5 + a[1]*bottom, icon.getMinU(), icon.getMaxV());
            t.addVertexWithUV(x + .5 + b[0]*bottom, y, z + .5 + b[1]*bottom, icon.getMaxU(), icon.getMaxV());
            t.addVertexWithUV(x + .5 + b[0]*top, y + 1, z + .5 + b[1]*top, icon.getMaxU(), icon.getMinV());
            t.addVertexWithUV(x + .5 + a[0]*top, y + 1, z + .5 + a[1]*top, icon.getMinU(), icon.getMinV());
        }
        for (int cap = 0; cap < 2; cap++) {
            double radius = cap == 0 ? bottom : top;
            t.setColorOpaque_F(0.85F, 0.85F, 0.85F);
            for (int i = 0; i < 4; i++) {
                double[] p = points[cap == 0 ? 3 - i : i];
                t.addVertexWithUV(x + .5 + p[0]*radius, y + cap, z + .5 + p[1]*radius,
                        p[0] < 0 ? icon.getMinU() : icon.getMaxU(), p[1] < 0 ? icon.getMinV() : icon.getMaxV());
            }
        }
    }
    @Override public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        draw(t, block.getIcon(0, 0), -.5, -.5, -.5, 2);
        t.draw();
    }
    @Override public boolean shouldRender3DInInventory(int id) { return true; }
    @Override public int getRenderId() { return BlockCaveSpike.renderId; }
}
