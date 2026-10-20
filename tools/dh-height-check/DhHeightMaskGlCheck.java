package com.seibel.distanthorizons.common.render.openGl.terrain;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import net.minecraft.client.renderer.WorldRenderer;

/** Exercises the production mask upload and fragment shader on a real OpenGL driver. */
public class DhHeightMaskGlCheck {
    private static int shader(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source); GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0)
            throw new AssertionError(GL20.glGetShaderInfoLog(shader, 8192));
        return shader;
    }
    private static int program(String vertex, String fragment) {
        int p = GL20.glCreateProgram();
        int v = shader(GL20.GL_VERTEX_SHADER, vertex), f = shader(GL20.GL_FRAGMENT_SHADER, fragment);
        GL20.glAttachShader(p, v); GL20.glAttachShader(p, f);
        GL20.glBindAttribLocation(p, 0, "position"); GL20.glLinkProgram(p);
        if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == 0)
            throw new AssertionError(GL20.glGetProgramInfoLog(p, 8192));
        GL20.glDeleteShader(v); GL20.glDeleteShader(f);
        return p;
    }
    private static void draw(int p, ReadyChunkMask mask, int start, boolean left, boolean right) {
        GL13.glActiveTexture(GL13.GL_TEXTURE3); mask.bind();
        GL20.glUniform2i(GL20.glGetUniformLocation(p, "uReadyChunkOrigin"), mask.originX, mask.originZ);
        GL20.glUniform1f(GL20.glGetUniformLocation(p, "start"), start);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        ByteBuffer pixel = BufferUtils.createByteBuffer(4);
        GL11.glReadPixels(8, 4, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        if ((pixel.get(0) != 0) != left) throw new AssertionError("left LOD visibility");
        GL11.glReadPixels(24, 4, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        if ((pixel.get(0) != 0) != right) throw new AssertionError("right LOD visibility");
        if (GL11.glGetError() != GL11.GL_NO_ERROR) throw new AssertionError("GL error");
    }
    public static void main(String[] args) throws Exception {
        Pbuffer context = new Pbuffer(32, 8, new PixelFormat(), null, null);
        try {
            context.makeCurrent();
            com.seibel.distanthorizons.common.render.openGl.glObject.GlCapabilities caps =
                com.seibel.distanthorizons.common.render.openGl.glObject.GlCapabilities.current();
            if (caps.OpenGL32 != GLContext.getCapabilities().OpenGL32 || caps.OpenGL44 != GLContext.getCapabilities().OpenGL44)
                throw new AssertionError("LWJGL 2 capability snapshot mismatch");
            String base = args[0] + "/src/main/resources/assets/ariamisdh/shaders/shared/gl/";
            String fragment = new String(Files.readAllBytes(Paths.get(base + "flat_shaded.frag")), StandardCharsets.UTF_8);
            int actual = program(new String(Files.readAllBytes(Paths.get(base + "standard.vert")), StandardCharsets.UTF_8), fragment);
            GL20.glDeleteProgram(actual);
            int p = program("#version 150\nin vec2 position; uniform float start; uniform float testHeight; uniform bool topFace; out vec4 vertexColor; out vec3 vertexWorldPos; "
                + "out vec4 vPos; out vec2 chunkRelativePos; out float vertexYPos; void main(){gl_Position=vec4(position,0,1); "
                + "vertexYPos=testHeight; vertexColor=vec4(1); vertexWorldPos=vec3(position,10); vPos=topFace ? vec4(position.x,0,-position.y,1) : vec4(vertexWorldPos,1); "
                + "chunkRelativePos=vec2((position.x+1)*16+start,8);}", fragment);
            GL20.glUseProgram(p);
            GL20.glUniform1f(GL20.glGetUniformLocation(p, "testHeight"), 48);
            GL20.glUniform1i(GL20.glGetUniformLocation(p, "uUseReadyChunks"), 1);
            GL20.glUniform1i(GL20.glGetUniformLocation(p, "uReadyChunks"), 3);
            GL20.glUniform1i(GL20.glGetUniformLocation(p, "uDitherDhRendering"), 1);
            GL20.glUniform1f(GL20.glGetUniformLocation(p, "uClipDistance"), 200);
            GL20.glUniform2f(GL20.glGetUniformLocation(p, "uCameraChunkOffset"), 0, 0);
            GL30.glBindVertexArray(GL30.glGenVertexArrays());
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GL15.glGenBuffers());
            FloatBuffer vertices = BufferUtils.createFloatBuffer(8);
            vertices.put(new float[]{-1,-1,1,-1,-1,1,1,1}).flip();
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);
            GL20.glEnableVertexAttribArray(0); GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 0, 0L);
            GL11.glViewport(0, 0, 32, 8); GL11.glClearColor(0, 0, 0, 1);
            ReadyChunkMask mask = new ReadyChunkMask();
            OptifineVanillaChunkTracker.update(null);
            WorldRenderer[] sections = new WorldRenderer[20];
            for (int i=0;i<20;i++) sections[i]=new WorldRenderer(0,(i-4)*16,0,true);

            OptifineVanillaChunkTracker.update(sections);
            for (int i=0;i<20;i++) {
                int worldY=(i-4)*16;
                GL20.glUniform1f(GL20.glGetUniformLocation(p,"testHeight"),worldY+8);
                draw(p,mask,0,false,true);
                sections[i]=new WorldRenderer(0,worldY,0,false);
                OptifineVanillaChunkTracker.update(sections);
                draw(p,mask,0,true,true);
                sections[i]=new WorldRenderer(0,worldY,0,true);

                OptifineVanillaChunkTracker.update(sections);
            }
            sections[0]=new WorldRenderer(0,-64,0,false);
            OptifineVanillaChunkTracker.update(sections);
            GL20.glUniform1i(GL20.glGetUniformLocation(p,"topFace"),1);
            GL20.glUniform1f(GL20.glGetUniformLocation(p,"testHeight"),-48);
            draw(p,mask,0,true,true);
            mask.free();
            System.out.println("PASS: production GL shaders and 20-layer mask at Y=-64..255; missing section retains LOD; negative top-face boundary");
        } finally { context.destroy(); }
    }
}
