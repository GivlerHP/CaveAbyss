package ru.givler.caveabyss.client;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import ru.givler.caveabyss.block.CaveBlocks;

public final class FallingDripstoneRenderer extends Render {
    private final RenderBlocks blocks = new RenderBlocks();

    @Override public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        bindEntityTexture(entity);
        GL11.glDisable(GL11.GL_LIGHTING);
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.setBrightness(entity.getBrightnessForRender(partialTicks));
        tess.setColorOpaque_F(1, 1, 1);
        blocks.drawCrossedSquares(CaveBlocks.pointedDripstone.getIcon(0, 8), -.5, -.5, -.5, 1);
        tess.draw();
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    @Override protected ResourceLocation getEntityTexture(Entity entity) { return TextureMap.locationBlocksTexture; }
}
