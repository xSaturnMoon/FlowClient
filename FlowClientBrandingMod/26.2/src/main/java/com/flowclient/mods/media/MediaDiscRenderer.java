package com.flowclient.mods.media;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ARGB;

final class MediaDiscRenderer {
    private static final int BORDER_COLOR = 0xFF1E1E1E;
    private static final int PLACEHOLDER_COLOR = 0xFF3A3A3A;
    private static final int GPU_PATH_THRESHOLD = 56;
    private static final int CPU_RENDER_CAP = 88;

    private MediaDiscRenderer() {
    }

    static void draw(GuiGraphicsExtractor graphics, int x, int y, int size, float rotation, MediaAlbumArtTexture album) {
        if (size >= GPU_PATH_THRESHOLD && album.hasGpuTexture()) {
            drawGpuDisc(graphics, x, y, size, rotation, album);
            return;
        }

        drawCpuDisc(graphics, x, y, size, rotation, album);
    }

    private static void drawGpuDisc(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int size,
            float rotationDegrees,
            MediaAlbumArtTexture album
    ) {
        DynamicTexture texture = album.gpuTexture();
        if (texture == null) {
            drawCpuDisc(graphics, x, y, size, rotationDegrees, album);
            return;
        }

        int centerX = x + size / 2;
        int centerY = y + size / 2;
        var pose = graphics.pose();

        pose.pushMatrix();
        pose.translate(centerX, centerY);
        pose.rotate((float) Math.toRadians(rotationDegrees));
        pose.translate(-size / 2.0F, -size / 2.0F);
        graphics.blit(
                texture.getTextureView(),
                texture.getSampler(),
                0,
                0,
                size,
                size,
                0.0F,
                0.0F,
                1.0F,
                1.0F
        );
        pose.popMatrix();

        drawCircleBorder(graphics, centerX, centerY, size / 2, BORDER_COLOR, borderStep(size));
    }

    private static void drawCpuDisc(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int size,
            float rotation,
            MediaAlbumArtTexture album
    ) {
        int renderSize = Math.min(size, CPU_RENDER_CAP);
        if (renderSize == size) {
            drawCpuDiscAt(graphics, x, y, size, rotation, album);
            return;
        }

        int centerX = x + size / 2;
        int centerY = y + size / 2;
        float scale = size / (float) renderSize;
        var pose = graphics.pose();

        pose.pushMatrix();
        pose.translate(centerX, centerY);
        pose.rotate((float) Math.toRadians(rotation));
        pose.scale(scale, scale);
        pose.translate(-renderSize / 2.0F, -renderSize / 2.0F);
        drawCpuDiscAt(graphics, 0, 0, renderSize, 0.0F, album);
        pose.popMatrix();
    }

    private static void drawCpuDiscAt(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int size,
            float rotation,
            MediaAlbumArtTexture album
    ) {
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        int outerRadius = size / 2;
        int artRadius = Math.max(1, outerRadius - 1);
        int step = renderStep(size);

        drawRotatedAlbumArt(graphics, centerX, centerY, artRadius, rotation, album, step);
        drawCircleBorder(graphics, centerX, centerY, outerRadius, BORDER_COLOR, step);
    }

    private static int renderStep(int size) {
        if (size <= 48) {
            return 1;
        }
        if (size <= 96) {
            return 2;
        }
        return 3;
    }

    private static int borderStep(int size) {
        if (size <= 140) {
            return 2;
        }
        return 4;
    }

    private static void drawRotatedAlbumArt(
            GuiGraphicsExtractor graphics,
            int centerX,
            int centerY,
            int radius,
            float rotationDegrees,
            MediaAlbumArtTexture album,
            int step
    ) {
        NativeImage image = album.image();
        if (image == null) {
            fillCircle(graphics, centerX, centerY, radius, PLACEHOLDER_COLOR);
            return;
        }

        float radians = (float) Math.toRadians(rotationDegrees);
        float cos = (float) Math.cos(-radians);
        float sin = (float) Math.sin(-radians);
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int radiusSq = radius * radius;

        for (int dy = -radius; dy < radius; dy += step) {
            for (int dx = -radius; dx < radius; dx += step) {
                if (dx * dx + dy * dy > radiusSq) {
                    continue;
                }

                float rotatedX = dx * cos - dy * sin;
                float rotatedY = dx * sin + dy * cos;
                int sx = (int) ((rotatedX + radius) * imageWidth / (radius * 2.0F));
                int sy = (int) ((rotatedY + radius) * imageHeight / (radius * 2.0F));
                sx = Math.min(imageWidth - 1, Math.max(0, sx));
                sy = Math.min(imageHeight - 1, Math.max(0, sy));

                int color = toRenderColor(image.getPixel(sx, sy));
                if (color == 0) {
                    continue;
                }

                graphics.fill(centerX + dx, centerY + dy, centerX + dx + step, centerY + dy + step, color);
            }
        }
    }

    private static void drawCircleBorder(
            GuiGraphicsExtractor graphics,
            int centerX,
            int centerY,
            int radius,
            int color,
            int step
    ) {
        for (int dy = -radius; dy <= radius; dy += step) {
            for (int dx = -radius; dx <= radius; dx += step) {
                double distance = Math.hypot(dx, dy);
                if (distance > radius || distance < radius - Math.max(1.0, step)) {
                    continue;
                }
                graphics.fill(centerX + dx, centerY + dy, centerX + dx + step, centerY + dy + step, color);
            }
        }
    }

    private static void fillCircle(GuiGraphicsExtractor graphics, int centerX, int centerY, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int halfWidth = (int) Math.sqrt((double) radius * radius - (double) dy * dy);
            if (halfWidth <= 0) {
                continue;
            }
            graphics.fill(centerX - halfWidth, centerY + dy, centerX + halfWidth + 1, centerY + dy + 1, color);
        }
    }

    private static int toRenderColor(int pixel) {
        int alpha = (pixel >>> 24) & 0xFF;
        int blue = (pixel >>> 16) & 0xFF;
        int green = (pixel >>> 8) & 0xFF;
        int red = pixel & 0xFF;

        if (alpha == 0 && red == 0 && green == 0 && blue == 0) {
            return 0;
        }

        if (alpha == 0) {
            alpha = 255;
        }

        return ARGB.color(alpha, red, green, blue);
    }
}
