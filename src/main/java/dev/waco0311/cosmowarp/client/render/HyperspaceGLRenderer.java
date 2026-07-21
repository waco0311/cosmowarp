package dev.waco0311.cosmowarp.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import dev.waco0311.cosmowarp.Cosmonauticswarpdrive;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Draws the hyperspace warp overlay using raw LWJGL/OpenGL calls instead of Mojang's
 * ShaderInstance/PostChain system. This is intentional: Veil (bundled with Create Aeronautics)
 * mixes directly into GameRenderer and ShaderInstance, which silently swallowed our shader when
 * it went through GameRenderer#loadEffect(). Compiling and running the program ourselves via
 * GL20/GL30 calls never touches those classes, so Veil has nothing to intercept.
 *
 * Not a general-purpose renderer -- this is intentionally minimal and single-purpose (one
 * fullscreen quad, one program, one offscreen target sized to the window).
 */
public class HyperspaceGLRenderer {

    private static final ResourceLocation VSH =
            ResourceLocation.fromNamespaceAndPath(Cosmonauticswarpdrive.MODID, "shaders/raw/hyperspace.vsh");
    private static final ResourceLocation FSH =
            ResourceLocation.fromNamespaceAndPath(Cosmonauticswarpdrive.MODID, "shaders/raw/hyperspace.fsh");
    private static final ResourceLocation BLIT_FSH =
            ResourceLocation.fromNamespaceAndPath(Cosmonauticswarpdrive.MODID, "shaders/raw/blit.fsh");

    private static int program = -1;
    private static int uDiffuseSamplerLoc = -1;
    private static int uGameTimeLoc = -1;
    private static int uChargeProgressLoc = -1;

    // Separate, distortion-free program used only to copy the finished result back onto the
    // main render target. Reusing `program` for that step would apply the pull/streak distortion
    // a second time (double-warping).
    private static int blitProgram = -1;
    private static int blitDiffuseSamplerLoc = -1;

    private static int vao = -1;
    private static int vbo = -1;

    private static RenderTarget offscreenTarget;

    private static float timeAccumulator = 0f;

    private static long startNanos = -1L;

    /** Called once per frame; no-ops entirely unless a warp is currently charging. */
    public static void renderIfActive() {
        if (!dev.waco0311.cosmowarp.client.WarpEffectClient.isActive()) return;

        ensureInitialized();
        if (program < 0) return; // compile failed; already logged, just skip silently each frame

        if (startNanos < 0) startNanos = System.nanoTime();
        timeAccumulator = (System.nanoTime() - startNanos) / 1_000_000_000f;

        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();

        if (offscreenTarget.width != width || offscreenTarget.height != height) {
            offscreenTarget.resize(width, height, false);
        }

        RenderTarget mainTarget = mc.getMainRenderTarget();

        // Render the distorted result into our offscreen target, sampling the already-drawn
        // main framebuffer as input.
        offscreenTarget.bindWrite(true);
        GlStateManager._disableDepthTest();
        GlStateManager._disableCull();
        GlStateManager._depthMask(false);

        GL20.glUseProgram(program);
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        GlStateManager._bindTexture(mainTarget.getColorTextureId());
        GL20.glUniform1i(uDiffuseSamplerLoc, 0);
        GL20.glUniform1f(uGameTimeLoc, timeAccumulator);
        GL20.glUniform1f(uChargeProgressLoc, dev.waco0311.cosmowarp.client.WarpEffectClient.getChargeProgress());

        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);

        GL20.glUseProgram(0);
        offscreenTarget.unbindWrite();

        // Copy the distorted result back onto the main render target so the rest of the frame
        // (GUI etc.) draws on top of it as usual.
        mainTarget.bindWrite(true);
        blitColorOnly(offscreenTarget);

        GlStateManager._depthMask(true);
        GlStateManager._enableDepthTest();
    }

    /** Simple textured-quad copy of the offscreen target's color buffer onto whatever is currently bound. */
    private static void blitColorOnly(RenderTarget source) {
        GL20.glUseProgram(blitProgram);
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        GlStateManager._bindTexture(source.getColorTextureId());
        GL20.glUniform1i(blitDiffuseSamplerLoc, 0);

        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
        GL20.glUseProgram(0);
    }

    private static void ensureInitialized() {
        if (program >= 0 || vao >= 0) return; // already tried (success or a prior failed attempt)

        try {
            String vshSource = readResource(VSH);
            int vs = compileShader(GL20.GL_VERTEX_SHADER, vshSource);
            int fs = compileShader(GL20.GL_FRAGMENT_SHADER, readResource(FSH));

            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, vs);
            GL20.glAttachShader(program, fs);
            GL20.glBindAttribLocation(program, 0, "aPos");
            GL20.glLinkProgram(program);

            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
                String log = GL20.glGetProgramInfoLog(program, 4096);
                Cosmonauticswarpdrive.LOGGER.error("cosmowarp: hyperspace raw-GL program link failed: {}", log);
                program = -1;
                return;
            }

            GL20.glDeleteShader(fs);
            uDiffuseSamplerLoc = GL20.glGetUniformLocation(program, "uDiffuseSampler");
            uGameTimeLoc = GL20.glGetUniformLocation(program, "uGameTime");
            uChargeProgressLoc = GL20.glGetUniformLocation(program, "uChargeProgress");

            // Second program: same vertex stage, distortion-free fragment stage, used only for
            // the final copy-back so the effect isn't applied twice.
            int vs2 = compileShader(GL20.GL_VERTEX_SHADER, vshSource);
            int blitFs = compileShader(GL20.GL_FRAGMENT_SHADER, readResource(BLIT_FSH));

            blitProgram = GL20.glCreateProgram();
            GL20.glAttachShader(blitProgram, vs2);
            GL20.glAttachShader(blitProgram, blitFs);
            GL20.glBindAttribLocation(blitProgram, 0, "aPos");
            GL20.glLinkProgram(blitProgram);

            if (GL20.glGetProgrami(blitProgram, GL20.GL_LINK_STATUS) == 0) {
                String log = GL20.glGetProgramInfoLog(blitProgram, 4096);
                Cosmonauticswarpdrive.LOGGER.error("cosmowarp: hyperspace raw-GL blit program link failed: {}", log);
                program = -1;
                blitProgram = -1;
                return;
            }

            GL20.glDeleteShader(vs);
            GL20.glDeleteShader(vs2);
            GL20.glDeleteShader(blitFs);

            blitDiffuseSamplerLoc = GL20.glGetUniformLocation(blitProgram, "uDiffuseSampler");

            setupFullscreenQuad();

            Minecraft mc = Minecraft.getInstance();
            offscreenTarget = new TextureTarget(mc.getWindow().getWidth(), mc.getWindow().getHeight(), false, false);

        } catch (Exception e) {
            Cosmonauticswarpdrive.LOGGER.error("cosmowarp: failed to initialize hyperspace raw-GL renderer", e);
            program = -1;
        }
    }

    private static void setupFullscreenQuad() {
        // Two triangles covering NDC space (-1..1), position-only (vec2).
        float[] verts = {
                -1f, -1f,
                1f, -1f,
                1f,  1f,

                -1f, -1f,
                1f,  1f,
                -1f,  1f,
        };

        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();

        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        FloatBuffer buffer = ByteBuffer
                .allocateDirect(verts.length * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.put(verts).flip();

        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 2, GL20.GL_FLOAT, false, 2 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    private static int compileShader(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            String log = GL20.glGetShaderInfoLog(shader, 4096);
            throw new RuntimeException("Shader compile failed (" + type + "): " + log);
        }
        return shader;
    }

    private static String readResource(ResourceLocation location) throws IOException {
        Resource resource = Minecraft.getInstance().getResourceManager().getResourceOrThrow(location);
        try (InputStream stream = resource.open()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}