package com.flowclient.mods.media;

import com.flowclient.compat.FlowIds;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

final class MediaAlbumArtTexture {
    private static final int MAX_STORED_SIZE = 64;
    private static final int MAX_GPU_SIZE = 128;

    private String loadedTrackId = "";
    private NativeImage artImage;
    private DynamicTexture gpuTexture;
    private ResourceLocation gpuTextureLocation;
    private boolean gpuReady;

    private static final MediaAlbumArtTexture INSTANCE = new MediaAlbumArtTexture();

    private MediaAlbumArtTexture() {
    }

    static MediaAlbumArtTexture get() {
        return INSTANCE;
    }

    NativeImage image() {
        return this.artImage;
    }

    ResourceLocation gpuTextureLocation() {
        return this.gpuTextureLocation;
    }

    DynamicTexture gpuTexture() {
        return this.gpuTexture;
    }

    boolean isReady() {
        return this.artImage != null;
    }

    boolean hasGpuTexture() {
        return this.gpuReady && this.gpuTexture != null;
    }

    void update(Minecraft minecraft, MediaSessionState state) {
        String trackId = state.trackKey();
        if (trackId.isBlank()) {
            this.clear();
            return;
        }

        if (trackId.equals(this.loadedTrackId) && this.isReady()) {
            return;
        }

        Path artPath = resolveArtPath(state);
        if (artPath == null) {
            this.loadedTrackId = trackId;
            this.clearImage();
            return;
        }

        try (InputStream input = Files.newInputStream(artPath)) {
            NativeImage source = NativeImage.read(input);
            NativeImage square = toSquare(source, MAX_STORED_SIZE);
            source.close();
            this.replaceImage(square);
            this.loadedTrackId = trackId;
        } catch (Exception ignored) {
            this.loadedTrackId = trackId;
            this.clearImage();
        }
    }

    void clear() {
        this.loadedTrackId = "";
        this.clearImage();
    }

    private void replaceImage(NativeImage image) {
        this.clearImage();
        this.artImage = image;
        this.syncGpuTexture();
    }

    private void clearImage() {
        if (this.artImage != null) {
            this.artImage.close();
            this.artImage = null;
        }
        this.releaseGpuTexture();
    }

    private void syncGpuTexture() {
        if (this.artImage == null) {
            this.releaseGpuTexture();
            return;
        }

        NativeImage gpuImage = toGpuImage(this.artImage);
        if (this.gpuTexture == null) {
            this.gpuTextureLocation = FlowIds.id("media_album_art");
            this.gpuTexture = new DynamicTexture(gpuImage);
            Minecraft.getInstance().getTextureManager().register(this.gpuTextureLocation, this.gpuTexture);
        } else {
            this.gpuTexture.setPixels(gpuImage);
            this.gpuTexture.upload();
        }

        this.gpuReady = true;
    }

    private void releaseGpuTexture() {
        this.gpuReady = false;
        if (this.gpuTexture != null) {
            Minecraft.getInstance().getTextureManager().release(this.gpuTextureLocation);
            this.gpuTexture.close();
            this.gpuTexture = null;
            this.gpuTextureLocation = null;
        }
    }

    private static NativeImage toGpuImage(NativeImage source) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        int targetSize = Math.min(MAX_GPU_SIZE, Math.max(sourceWidth, sourceHeight));
        NativeImage image = new NativeImage(targetSize, targetSize, false);

        for (int y = 0; y < targetSize; y++) {
            for (int x = 0; x < targetSize; x++) {
                int sx = (x * sourceWidth) / targetSize;
                int sy = (y * sourceHeight) / targetSize;
                image.setPixelRGBA(
                        x,
                        y,
                        source.getPixelRGBA(
                                Math.min(sourceWidth - 1, Math.max(0, sx)),
                                Math.min(sourceHeight - 1, Math.max(0, sy))
                        )
                );
            }
        }

        applyCircularMask(image);
        return image;
    }

    private static void applyCircularMask(NativeImage image) {
        int size = image.getWidth();
        float center = (size - 1) / 2.0F;
        float radiusSq = center * center;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - center;
                float dy = y - center;
                if (dx * dx + dy * dy > radiusSq) {
                    image.setPixelRGBA(x, y, 0);
                }
            }
        }
    }

    private static Path resolveArtPath(MediaSessionState state) {
        if (state.artPath() != null && !state.artPath().isBlank()) {
            Path path = Path.of(state.artPath());
            if (Files.isRegularFile(path)) {
                return path;
            }
        }

        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            Path fallback = Path.of(appData, "FlowLauncher", "bin", "FlowMediaProbe", "current-art.dat");
            if (Files.isRegularFile(fallback)) {
                return fallback;
            }
        }

        return null;
    }

    private static NativeImage toSquare(NativeImage source, int maxSize) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        int squareSize = Math.max(sourceWidth, sourceHeight);
        int targetSize = Math.min(maxSize, squareSize);

        NativeImage square = new NativeImage(targetSize, targetSize, false);
        for (int y = 0; y < targetSize; y++) {
            for (int x = 0; x < targetSize; x++) {
                int sx = (x * sourceWidth) / targetSize;
                int sy = (y * sourceHeight) / targetSize;
                square.setPixelRGBA(x, y, source.getPixelRGBA(
                        Math.min(sourceWidth - 1, Math.max(0, sx)),
                        Math.min(sourceHeight - 1, Math.max(0, sy))
                ));
            }
        }

        return square;
    }
}
