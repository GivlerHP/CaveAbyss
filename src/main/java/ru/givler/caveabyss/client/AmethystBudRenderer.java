package ru.givler.caveabyss.client;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.GL11;
import ru.givler.caveabyss.block.BlockAmethystBud;

/** Two crossed, double-sided planes growing away from the attached face. */
public final class AmethystBudRenderer implements ISimpleBlockRenderingHandler {
    private final int renderId;

    private AmethystBudRenderer(int renderId) {
        this.renderId = renderId;
    }

    public static void register() {
        int id = RenderingRegistry.getNextAvailableRenderId();
        BlockAmethystBud.setRenderId(id);
        RenderingRegistry.registerBlockHandler(new AmethystBudRenderer(id));
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.setBrightness(0xF000F0);
        tess.setColorOpaque_F(1, 1, 1);
        draw(tess, block.getIcon(0, metadata), ((BlockAmethystBud)block).getStage(), 1, 0, 0, 0);
        tess.draw();
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z,
                                    Block block, int modelId, RenderBlocks renderer) {
        Tessellator tess = Tessellator.instance;
        tess.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        tess.setColorOpaque_F(1, 1, 1);
        draw(tess, block.getIcon(0, world.getBlockMetadata(x, y, z)),
                ((BlockAmethystBud)block).getStage(), world.getBlockMetadata(x, y, z), x, y, z);
        return true;
    }

    private static void draw(Tessellator tess, IIcon icon, int stage, int face, double x, double y, double z) {
        // Local U/V span the support face; W points away from the support.
        double[] origin = {x + .5, y + .5, z + .5};
        double[] u, v, w;
        switch (face) {
            case 0: origin[1] += .5; u = vec(1, 0, 0); v = vec(0, 0, -1); w = vec(0, -1, 0); break;
            case 2: origin[2] += .5; u = vec(-1, 0, 0); v = vec(0, 1, 0); w = vec(0, 0, -1); break;
            case 3: origin[2] -= .5; u = vec(1, 0, 0); v = vec(0, 1, 0); w = vec(0, 0, 1); break;
            case 4: origin[0] += .5; u = vec(0, 0, 1); v = vec(0, 1, 0); w = vec(-1, 0, 0); break;
            case 5: origin[0] -= .5; u = vec(0, 0, -1); v = vec(0, 1, 0); w = vec(1, 0, 0); break;
            default: origin[1] -= .5; u = vec(1, 0, 0); v = vec(0, 0, 1); w = vec(0, 1, 0); break;
        }
        double half = (stage == 0 ? 5 : stage == 1 ? 6 : stage == 2 ? 7 : 8) / 16.0;
        double length = (stage == 0 ? 6 : stage == 1 ? 10 : stage == 2 ? 13 : 16) / 16.0;
        for (int diagonal = 0; diagonal < 2; diagonal++) {
            double[] a = vertex(origin, u, v, w, -half, diagonal == 0 ? -half : half, 0);
            double[] b = vertex(origin, u, v, w, half, diagonal == 0 ? half : -half, 0);
            double[] c = vertex(origin, u, v, w, half, diagonal == 0 ? half : -half, length);
            double[] d = vertex(origin, u, v, w, -half, diagonal == 0 ? -half : half, length);
            quad(tess, a, b, c, d, icon, false);
            quad(tess, d, c, b, a, icon, true);
        }
    }

    private static double[] vec(double x, double y, double z) {
        return new double[] {x, y, z};
    }

    private static double[] vertex(double[] o, double[] u, double[] v, double[] w,
                                   double a, double b, double c) {
        return new double[] {o[0] + a*u[0] + b*v[0] + c*w[0],
                o[1] + a*u[1] + b*v[1] + c*w[1], o[2] + a*u[2] + b*v[2] + c*w[2]};
    }

    private static void quad(Tessellator t, double[] a, double[] b, double[] c, double[] d,
                             IIcon icon, boolean back) {
        double left = back ? icon.getMaxU() : icon.getMinU();
        double right = back ? icon.getMinU() : icon.getMaxU();
        t.addVertexWithUV(a[0], a[1], a[2], left, icon.getMaxV());
        t.addVertexWithUV(b[0], b[1], b[2], right, icon.getMaxV());
        t.addVertexWithUV(c[0], c[1], c[2], right, icon.getMinV());
        t.addVertexWithUV(d[0], d[1], d[2], left, icon.getMinV());
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) { return true; }

    @Override
    public int getRenderId() { return renderId; }
}
