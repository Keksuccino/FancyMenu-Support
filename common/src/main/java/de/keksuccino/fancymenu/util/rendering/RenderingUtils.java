package de.keksuccino.fancymenu.util.rendering;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinGameRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import java.awt.*;
import java.util.function.Function;

public class RenderingUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final DrawableColor MISSING_TEXTURE_COLOR_MAGENTA = DrawableColor.of(Color.MAGENTA);
    public static final DrawableColor MISSING_TEXTURE_COLOR_BLACK = DrawableColor.BLACK;
    public static final ResourceLocation FULLY_TRANSPARENT_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/fully_transparent.png");
    public static final ResourceLocation BLUR_LOCATION = ResourceLocation.parse("shaders/post/blur.json");

    public static PostChain blurEffect = null;

    //Generated with GPT-o1 for testing purposes
    public static void renderBlurredArea(GuiGraphics graphics, int x, int y, int width, int height, float partialTicks, int blurRadius) {

        Minecraft minecraft = Minecraft.getInstance();

        RenderSystem.backupProjectionMatrix();

        // Define margin based on blur radius
        int margin = blurRadius * 2;

        // Adjusted area to capture
        int captureX = x - margin;
        int captureY = y - margin;
        int captureWidth = width + margin * 2;
        int captureHeight = height + margin * 2;

        // Ensure the capture area is within screen bounds
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        captureX = Math.max(0, captureX);
        captureY = Math.max(0, captureY);
        captureWidth = Math.min(captureWidth, screenWidth - captureX);
        captureHeight = Math.min(captureHeight, screenHeight - captureY);

        // Step 1: Create a TextureTarget for the blur effect
        TextureTarget blurRenderTarget = new TextureTarget(captureWidth, captureHeight, true, Minecraft.ON_OSX);
        blurRenderTarget.setClearColor(0, 0, 0, 0);
        blurRenderTarget.clear(true);

        // Step 2: Bind the RenderTarget
        blurRenderTarget.bindWrite(true);

        // Step 3: Set up the projection matrix for rendering to the RenderTarget
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, (float)captureWidth, (float)captureHeight, 0.0F, 1000.0F, 3000.0F), VertexSorting.ORTHOGRAPHIC_Z);
        RenderSystem.applyModelViewMatrix();

        // Step 4: Copy the area from the main RenderTarget to the blurRenderTarget
        // Bind the main RenderTarget's texture
        RenderSystem.setShaderTexture(0, minecraft.getMainRenderTarget().getColorTextureId());
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        // Draw the textured quad to the blurRenderTarget
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        float u0 = (float)captureX / (float)screenWidth;
        float v0 = (float)captureY / (float)screenHeight;
        float u1 = (float)(captureX + captureWidth) / (float)screenWidth;
        float v1 = (float)(captureY + captureHeight) / (float)screenHeight;

        // Add vertices
        bufferBuilder.addVertex(0.0F, (float)captureHeight, 0.0F).setUv(u0, v1);
        bufferBuilder.addVertex((float)captureWidth, (float)captureHeight, 0.0F).setUv(u1, v1);
        bufferBuilder.addVertex((float)captureWidth, 0.0F, 0.0F).setUv(u1, v0);
        bufferBuilder.addVertex(0.0F, 0.0F, 0.0F).setUv(u0, v0);

        // Build the mesh and draw it
        MeshData meshData = bufferBuilder.buildOrThrow();
        BufferUploader.drawWithShader(meshData);

        // Step 5: Apply the blur shader to the RenderTarget
        PostChain blurEffect = ((IMixinGameRenderer)Minecraft.getInstance().gameRenderer).getBlurEffect_FancyMenu();
        if (blurEffect != null) {
            blurEffect.resize(captureWidth, captureHeight);
            blurEffect.setUniform("Radius", (float)blurRadius);
            blurEffect.process(partialTicks);
        }

        // Step 6: Unbind the RenderTarget
        blurRenderTarget.unbindWrite();

        // Restore the original projection matrix
        Window window = minecraft.getWindow();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, (float)window.getGuiScaledWidth(), (float)window.getGuiScaledHeight(), 0.0F, 1000.0F, 3000.0F), VertexSorting.ORTHOGRAPHIC_Z);
        RenderSystem.applyModelViewMatrix();

        // Step 7: Render the blurred texture back onto the screen at (x, y)
        minecraft.getMainRenderTarget().bindWrite(false);

        // Set up rendering state
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Bind the blurred texture
        RenderSystem.setShaderTexture(0, blurRenderTarget.getColorTextureId());
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        // Draw the textured quad at (x, y) with size (width, height)
        // Using texture coordinates to exclude the margins
        float texU0 = (float)margin / (float)captureWidth;
        float texV0 = (float)margin / (float)captureHeight;
        float texU1 = (float)(margin + width) / (float)captureWidth;
        float texV1 = (float)(margin + height) / (float)captureHeight;

        // Prepare for rendering on the main screen
        bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate((float)x, (float)y, 0.0F);

        Matrix4f screenMatrix = poseStack.last().pose();

        bufferBuilder.addVertex(screenMatrix, 0.0F, (float)height, 0.0F).setUv(texU0, texV1);
        bufferBuilder.addVertex(screenMatrix, (float)width, (float)height, 0.0F).setUv(texU1, texV1);
        bufferBuilder.addVertex(screenMatrix, (float)width, 0.0F, 0.0F).setUv(texU1, texV0);
        bufferBuilder.addVertex(screenMatrix, 0.0F, 0.0F, 0.0F).setUv(texU0, texV0);

        MeshData screenMeshData = bufferBuilder.buildOrThrow();
        BufferUploader.drawWithShader(screenMeshData);

        poseStack.popPose();

        // Cleanup
        RenderSystem.disableBlend();
        blurRenderTarget.destroyBuffers();

        RenderSystem.restoreProjectionMatrix();
        RenderSystem.applyModelViewMatrix();

    }

    /**
     * This is just for playing around with the blur effect and is not working correctly yet.
     */
    @ApiStatus.Experimental
    public static void processBlurEffect(@NotNull GuiGraphics graphics, int x, int y, int width, int height, float partial, float blurriness) {

        // shader seems to render dark out-of-screen edges of top and bottom of screen (because nothing is rendered out-of-screen)

        if (blurEffect == null) reloadBlurShader();

        RenderSystem.enableBlend();

        float _blurriness = blurriness * 10.0F;
        if (blurEffect != null && (_blurriness >= 1.0F)) {
            graphics.enableScissor(x, y, x + width, y + height);
            blurEffect.setUniform("Radius", _blurriness);
            blurEffect.process(partial);
            graphics.disableScissor();
        }

        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);

        RenderSystem.disableBlend();

    }

    /**
     * This is just for playing around with the blur effect and is not working correctly yet.
     */
    @ApiStatus.Experimental
    public static void reloadBlurShader() {
        if (blurEffect != null) {
            blurEffect.close();
        }
        try {
            GameRenderer.ResourceCache cache = new GameRenderer.ResourceCache(Minecraft.getInstance().getResourceManager(), new HashMap<>());
            blurEffect = new PostChain(Minecraft.getInstance().getTextureManager(), cache, Minecraft.getInstance().getMainRenderTarget(), BLUR_LOCATION);
            blurEffect.resize(Minecraft.getInstance().getWindow().getWidth(), Minecraft.getInstance().getWindow().getHeight());
        } catch (IOException ex) {
            LOGGER.warn("Failed to load shader: {}", BLUR_LOCATION, ex);
        } catch (JsonSyntaxException var4) {
            LOGGER.warn("Failed to parse shader: {}", BLUR_LOCATION, var4);
        }
    }

    public static void renderMissing(@NotNull GuiGraphics graphics, int x, int y, int width, int height) {
        int partW = width / 2;
        int partH = height / 2;
        //Top-left
        graphics.fill(x, y, x + partW, y + partH, MISSING_TEXTURE_COLOR_MAGENTA.getColorInt());
        //Top-right
        graphics.fill(x + partW, y, x + width, y + partH, MISSING_TEXTURE_COLOR_BLACK.getColorInt());
        //Bottom-left
        graphics.fill(x, y + partH, x + partW, y + height, MISSING_TEXTURE_COLOR_BLACK.getColorInt());
        //Bottom-right
        graphics.fill(x + partW, y + partH, x + width, y + height, MISSING_TEXTURE_COLOR_MAGENTA.getColorInt());
    }

    /**
     * Repeatedly renders a tileable (seamless) texture inside an area. Fills the area with the texture.
     *
     * @param graphics The {@link GuiGraphics} instance.
     * @param location The {@link ResourceLocation} of the texture.
     * @param x The X position the area should get rendered at.
     * @param y The Y position the area should get rendered at.
     * @param areaRenderWidth The width of the area.
     * @param areaRenderHeight The height of the area.
     * @param texWidth The full width (in pixels) of the texture.
     * @param texHeight The full height (in pixels) of the texture.
     */
    public static void blitRepeat(@NotNull GuiGraphics graphics, @NotNull ResourceLocation location, int x, int y, int areaRenderWidth, int areaRenderHeight, int texWidth, int texHeight) {
        graphics.blit(location, x, y, 0.0F, 0.0F, areaRenderWidth, areaRenderHeight, texWidth, texHeight);
        //blitRepeat(graphics, location, x, y, areaRenderWidth, areaRenderHeight, texWidth, texHeight, 0, 0, texWidth, texHeight, texWidth, texHeight);
    }

    /**
     * Repeatedly renders a tileable (seamless) texture inside an area. Fills the area with the texture.
     *
     * @param graphics The {@link GuiGraphics} instance.
     * @param renderType The render type.
     * @param location The {@link ResourceLocation} of the texture.
     * @param x The X position the area should get rendered at.
     * @param y The Y position the area should get rendered at.
     * @param areaRenderWidth The width of the area.
     * @param areaRenderHeight The height of the area.
     * @param texWidth The full width (in pixels) of the texture.
     * @param texHeight The full height (in pixels) of the texture.
     */
    public static void blitRepeat(@NotNull GuiGraphics graphics, @NotNull ResourceLocation location, int x, int y, int areaRenderWidth, int areaRenderHeight, int texRenderWidth, int texRenderHeight, int texOffsetX, int texOffsetY, int texPartWidth, int texPartHeight, int texWidth, int texHeight) {

        Objects.requireNonNull(graphics);
        Objects.requireNonNull(location);
        if ((areaRenderWidth <= 0) || (areaRenderHeight <= 0) || (texRenderWidth <= 0) || (texRenderHeight <= 0) || (texPartWidth <= 0) || (texPartHeight <= 0)) return;

        int repeatsHorizontal = Math.max(1, (areaRenderWidth / texPartWidth));
        if ((texPartWidth * repeatsHorizontal) < areaRenderWidth) repeatsHorizontal++;
        int repeatsVertical = Math.max(1, (areaRenderHeight / texPartHeight));
        if ((texPartHeight * repeatsVertical) < areaRenderHeight) repeatsVertical++;

        graphics.enableScissor(x, y, x + areaRenderWidth, y + areaRenderHeight);

        for (int horizontal = 0; horizontal < repeatsHorizontal; horizontal++) {
            for (int vertical = 0; vertical < repeatsVertical; vertical++) {
                int renderX = x + (texPartWidth * horizontal);
                int renderY = y + (texPartHeight * vertical);
                graphics.blit(location, renderX, renderY, texRenderWidth, texRenderHeight, (float)texOffsetX, (float)texOffsetY, texPartWidth, texPartHeight, texWidth, texHeight);
            }
        }

        blitNineSlicedTexture(graphics, RenderType::guiTextured, texture, x, y, width, height, textureWidth, textureHeight, borderTop, borderRight, borderBottom, borderLeft, color);

    }

    /**
     * Renders a texture using nine-slice scaling with tiled edges and center.
     *
     * @param graphics The GuiGraphics instance to use for rendering
     * @param renderType The render type.
     * @param texture The texture ResourceLocation to render
     * @param x The x position to render at
     * @param y The y position to render at
     * @param width The desired width to render
     * @param height The desired height to render
     * @param textureWidth The actual width of the texture
     * @param textureHeight The actual height of the texture
     * @param borderTop The size of the top border
     * @param borderRight The size of the right border
     * @param borderBottom The size of the bottom border
     * @param borderLeft The size of the left border
     * @param color The color to tint the texture with
     */
    public static void blitNineSlicedTexture(GuiGraphics graphics, @NotNull Function<ResourceLocation, RenderType> renderType, ResourceLocation texture, int x, int y, int width, int height,
                               int textureWidth, int textureHeight,
                               int borderTop, int borderRight, int borderBottom, int borderLeft, int color) {

        // Corner pieces
        // Top left
        graphics.blit(renderType, texture, x, y, 0, 0, borderLeft, borderTop, textureWidth, textureHeight, color);
        // Top right
        graphics.blit(renderType, texture, x + width - borderRight, y, textureWidth - borderRight, 0, borderRight, borderTop, textureWidth, textureHeight, color);
        // Bottom left
        graphics.blit(renderType, texture, x, y + height - borderBottom, 0, textureHeight - borderBottom, borderLeft, borderBottom, textureWidth, textureHeight, color);
        // Bottom right
        graphics.blit(renderType, texture, x + width - borderRight, y + height - borderBottom, textureWidth - borderRight, textureHeight - borderBottom, borderRight, borderBottom, textureWidth, textureHeight, color);

        // Edges - Tiled
        int centerWidth = textureWidth - borderLeft - borderRight;
        int centerHeight = textureHeight - borderTop - borderBottom;

        // Top edge
        for (int i = borderLeft; i < width - borderRight; i += centerWidth) {
            int pieceWidth = Math.min(centerWidth, width - borderRight - i);
            graphics.blit(renderType, texture, x + i, y, borderLeft, 0, pieceWidth, borderTop, textureWidth, textureHeight, color);
        }

        // Bottom edge
        for (int i = borderLeft; i < width - borderRight; i += centerWidth) {
            int pieceWidth = Math.min(centerWidth, width - borderRight - i);
            graphics.blit(renderType, texture, x + i, y + height - borderBottom, borderLeft, textureHeight - borderBottom, pieceWidth, borderBottom, textureWidth, textureHeight, color);
        }

        // Left edge
        for (int j = borderTop; j < height - borderBottom; j += centerHeight) {
            int pieceHeight = Math.min(centerHeight, height - borderBottom - j);
            graphics.blit(renderType, texture, x, y + j, 0, borderTop, borderLeft, pieceHeight, textureWidth, textureHeight, color);
        }

        // Right edge
        for (int j = borderTop; j < height - borderBottom; j += centerHeight) {
            int pieceHeight = Math.min(centerHeight, height - borderBottom - j);
            graphics.blit(renderType, texture, x + width - borderRight, y + j, textureWidth - borderRight, borderTop, borderRight, pieceHeight, textureWidth, textureHeight, color);
        }

        // Center - Tiled
        for (int i = borderLeft; i < width - borderRight; i += centerWidth) {
            int pieceWidth = Math.min(centerWidth, width - borderRight - i);
            for (int j = borderTop; j < height - borderBottom; j += centerHeight) {
                int pieceHeight = Math.min(centerHeight, height - borderBottom - j);
                graphics.blit(renderType, texture, x + i, y + j, borderLeft, borderTop, pieceWidth, pieceHeight, textureWidth, textureHeight, color);
            }
        }

    }


    public static float getPartialTick() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
    }

    public static boolean isXYInArea(int targetX, int targetY, int x, int y, int width, int height) {
        return isXYInArea((double)targetX, targetY, x, y, width, height);
    }

    public static boolean isXYInArea(double targetX, double targetY, double x, double y, double width, double height) {
        return (targetX >= x) && (targetX < (x + width)) && (targetY >= y) && (targetY < (y + height));
    }

    public static void resetGuiScale() {
        Window m = Minecraft.getInstance().getWindow();
        m.setGuiScale(m.calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().options.forceUnicodeFont().get()));
    }

<<<<<<< HEAD
    public static void resetShaderColor(GuiGraphics graphics) {
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void setShaderColor(GuiGraphics graphics, DrawableColor color) {
        Color c = color.getColor();
        float a = Math.min(1F, Math.max(0F, (float)c.getAlpha() / 255.0F));
        setShaderColor(graphics, color, a);
    }

    public static void setShaderColor(GuiGraphics graphics, DrawableColor color, float alpha) {
        Color c = color.getColor();
        float r = Math.min(1F, Math.max(0F, (float)c.getRed() / 255.0F));
        float g = Math.min(1F, Math.max(0F, (float)c.getGreen() / 255.0F));
        float b = Math.min(1F, Math.max(0F, (float)c.getBlue() / 255.0F));
        graphics.setColor(r, g, b, alpha);
    }

=======
>>>>>>> 55affb8... v3.4.0 - 1.21.4
    /**
     * @param color The color.
     * @param newAlpha Value between 0 and 255.
     * @return The given color with new alpha.
     */
    public static int replaceAlphaInColor(int color, int newAlpha) {
        newAlpha = Math.min(newAlpha, 255);
        return color & 16777215 | newAlpha << 24;
    }

    /**
     * @param color The color.
     * @param newAlpha Value between 0.0F and 1.0F.
     * @return The given color with new alpha.
     */
    public static int replaceAlphaInColor(int color, float newAlpha) {
        return replaceAlphaInColor(color, (int)(newAlpha * 255.0F));
    }

    public static void fillF(@NotNull GuiGraphics graphics, float minX, float minY, float maxX, float maxY, int color) {
        fillF(graphics, minX, minY, maxX, maxY, 0F, color);
    }

    public static void fillF(@NotNull GuiGraphics graphics, float minX, float minY, float maxX, float maxY, float z, int color) {
        Matrix4f matrix4f = graphics.pose().last().pose();
        if (minX < maxX) {
            float $$8 = minX;
            minX = maxX;
            maxX = $$8;
        }
        if (minY < maxY) {
            float $$9 = minY;
            minY = maxY;
            maxY = $$9;
        }
        float red = (float)FastColor.ARGB32.red(color) / 255.0F;
        float green = (float)FastColor.ARGB32.green(color) / 255.0F;
        float blue = (float)FastColor.ARGB32.blue(color) / 255.0F;
        float alpha = (float) FastColor.ARGB32.alpha(color) / 255.0F;
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferBuilder.addVertex(matrix4f, minX, minY, z).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(matrix4f, minX, maxY, z).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(matrix4f, maxX, maxY, z).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(matrix4f, maxX, minY, z).setColor(red, green, blue, alpha);
        BufferUploader.drawWithShader(Objects.requireNonNull(bufferBuilder.build()));
        RenderSystem.disableBlend();
    }

    public static void blitF(@NotNull GuiGraphics graphics, ResourceLocation location, float x, float y, float f3, float f4, float width, float height, float width2, float height2) {
        blit(graphics, location, x, y, width, height, f3, f4, width, height, width2, height2);
    }

    private static void blit(GuiGraphics $$0, ResourceLocation location, float $$1, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9, float $$10) {
        blit($$0, location, $$1, $$1 + $$3, $$2, $$2 + $$4, 0, $$7, $$8, $$5, $$6, $$9, $$10);
    }

    private static void blit(GuiGraphics graphics, ResourceLocation location, float $$1, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9, float $$10, float $$11) {
        innerBlit(
                graphics,
                location,
                $$1,
                $$2,
                $$3,
                $$4,
                $$5,
                ($$8 + 0.0F) / (float)$$10,
                ($$8 + (float)$$6) / (float)$$10,
                ($$9 + 0.0F) / (float)$$11,
                ($$9 + (float)$$7) / (float)$$11
        );
    }

    private static void innerBlit(GuiGraphics graphics, ResourceLocation location, float $$1, float $$2, float $$3, float $$4, float $$5, float $$6, float $$7, float $$8, float $$9) {
        RenderSystem.setShaderTexture(0, location);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f $$10 = graphics.pose().last().pose();
        BufferBuilder $$11 = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        $$11.addVertex($$10, $$1, $$3, $$5).setUv($$6, $$8);
        $$11.addVertex($$10, $$1, $$4, $$5).setUv($$6, $$9);
        $$11.addVertex($$10, $$2, $$4, $$5).setUv($$7, $$9);
        $$11.addVertex($$10, $$2, $$3, $$5).setUv($$7, $$8);
        BufferUploader.drawWithShader(Objects.requireNonNull($$11.build()));
    }

}
