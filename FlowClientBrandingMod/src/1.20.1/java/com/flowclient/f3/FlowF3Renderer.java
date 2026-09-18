package com.flowclient.f3;

import com.flowclient.mixin.ParticleEngineAccessor;
import com.mojang.blaze3d.platform.GLX;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FlowF3Renderer {
    private static final int MARGIN = 2;
    private static final int LINE_HEIGHT = 9;

    private FlowF3Renderer() {
    }

    public static void render(Minecraft client, GuiGraphics graphics) {
        render(client, graphics, client.font);
    }

    public static void render(Minecraft client, GuiGraphics graphics, Font font) {
        if (!FlowF3Animation.shouldRender()) {
            return;
        }

        int screenWidth = graphics.guiWidth();
        float eased = FlowF3Animation.easedProgress();
        int slide = (int) (FlowF3Animation.SLIDE_DISTANCE * (1f - eased));

        renderColumn(font, graphics, buildLeftLines(client), screenWidth, false, -slide);
        renderColumn(font, graphics, buildRightLines(client), screenWidth, true, slide);
    }

    private static void renderColumn(
            Font font,
            GuiGraphics graphics,
            List<F3Line> lines,
            int screenWidth,
            boolean rightAlign,
            int offsetX
    ) {
        int y = MARGIN;
        for (F3Line line : lines) {
            if (!line.isBlank()) {
                drawLineBackground(font, graphics, y, line, rightAlign, screenWidth, offsetX);
                drawLine(font, graphics, y, line, rightAlign, screenWidth, offsetX);
            }
            y += LINE_HEIGHT;
        }
    }

    private static void drawLineBackground(
            Font font,
            GuiGraphics graphics,
            int y,
            F3Line line,
            boolean rightAlign,
            int screenWidth,
            int offsetX
    ) {
        int textWidth = line.width(font);
        int left;
        int right;
        if (rightAlign) {
            left = screenWidth - MARGIN - textWidth - 1 + offsetX;
            right = screenWidth - MARGIN + 1 + offsetX;
        } else {
            left = MARGIN - 1 + offsetX;
            right = MARGIN + textWidth + 1 + offsetX;
        }

        graphics.fill(left, y - 1, right, y + LINE_HEIGHT - 1, FlowF3Colors.LINE_BACKGROUND);
    }

    private static void drawLine(
            Font font,
            GuiGraphics graphics,
            int y,
            F3Line line,
            boolean rightAlign,
            int screenWidth,
            int offsetX
    ) {
        int totalWidth = line.width(font);
        int drawX = rightAlign ? screenWidth - MARGIN - totalWidth + offsetX : MARGIN + offsetX;

        for (F3Segment segment : line.segments()) {
            graphics.drawString(font, segment.text(), drawX, y, segment.color(), true);
            drawX += font.width(segment.text());
        }
    }

    private static List<F3Line> buildLeftLines(Minecraft client) {
        List<F3Line> lines = new ArrayList<>();
        String version = SharedConstants.getCurrentVersion().name();

        lines.add(F3Line.labeled("Minecraft ", version, FlowF3Colors.HEADER_LABEL, FlowF3Colors.HEADER_VALUE));
        lines.add(F3Line.labeled("FPS: ", String.valueOf(client.getFps()), FlowF3Colors.HEADER_LABEL, FlowF3Colors.HEADER_VALUE));
        lines.add(F3Line.labeled("Server: ", resolveServerName(client), FlowF3Colors.HEADER_LABEL, FlowF3Colors.HEADER_VALUE));
        lines.add(F3Line.spacer());

        ClientLevel level = client.level;
        if (level != null && client.getCameraEntity() != null) {
            var entity = client.getCameraEntity();
            BlockPos blockPos = entity.blockPosition();
            Direction direction = entity.getDirection();
            ChunkPos chunkPos = ChunkPos.containing(blockPos);

            lines.add(F3Line.location(
                    "XYZ: ",
                    String.format(
                            Locale.ROOT,
                            "%.3f %.5f %.3f",
                            entity.getX(),
                            entity.getY(),
                            entity.getZ()
                    )
            ));
            lines.add(F3Line.location(
                    "Block: ",
                    String.format(
                            Locale.ROOT,
                            "%d %d %d",
                            blockPos.getX(),
                            blockPos.getY(),
                            blockPos.getZ()
                    )
            ));
            lines.add(F3Line.location(
                    "Facing: ",
                    String.format(
                            Locale.ROOT,
                            "%s (%.1f / %.1f)",
                            facingLabel(direction),
                            Mth.wrapDegrees(entity.getYRot()),
                            Mth.wrapDegrees(entity.getXRot())
                    )
            ));
            lines.add(F3Line.location("World: ", level.dimension().location().toString()));
            lines.add(F3Line.location(
                    "Chunk: ",
                    String.format(
                            Locale.ROOT,
                            "%d %d %d",
                            chunkPos.x(),
                            SectionPos.blockToSectionCoord(blockPos.getY()),
                            chunkPos.z()
                    )
            ));
            lines.add(F3Line.location(
                    "Section Relative: ",
                    String.format(
                            Locale.ROOT,
                            "%d %d %d",
                            SectionPos.sectionRelative(blockPos.getX()),
                            SectionPos.sectionRelative(blockPos.getY()),
                            SectionPos.sectionRelative(blockPos.getZ())
                    )
            ));
            lines.add(F3Line.location("Biome: ", biomeName(level, blockPos)));
            lines.add(F3Line.location("Client Light: ", clientLightValues(level, blockPos)));

            lines.add(F3Line.stat("Entities: ", entityValues(client)));
            lines.add(F3Line.stat("Particles: ", String.valueOf(particleCount(client))));
            lines.add(F3Line.stat("Day: ", String.valueOf(dayCount(level))));
            lines.add(F3Line.stat("Sounds: ", soundsValues(client)));
            lines.add(F3Line.stat("Render Distance: ", String.valueOf(client.options.renderDistance().get())));
            lines.add(F3Line.stat("Simulation Distance: ", String.valueOf(simulationDistance(client, level))));
        }

        return lines;
    }

    private static List<F3Line> buildRightLines(Minecraft client) {
        List<F3Line> lines = new ArrayList<>();
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long maxMemory = Runtime.getRuntime().maxMemory();
        int memoryPercent = maxMemory == 0L ? 0 : (int) (usedMemory * 100L / maxMemory);
        long usedMb = usedMemory / (1024L * 1024L);
        long maxMb = maxMemory / (1024L * 1024L);

        var window = client.getWindow();

        lines.add(F3Line.hardware("GPU: ", GLX.getOpenGLVersionString()));
        lines.add(F3Line.hardware("RAM Usage: ", memoryPercent + "%"));
        lines.add(F3Line.solid(usedMb + "/" + maxMb + " MB", FlowF3Colors.HW_VALUE));
        lines.add(F3Line.hardware("Java ", System.getProperty("java.version")));
        lines.add(F3Line.hardware("CPU: ", GLX._getCpuInfo()));
        lines.add(F3Line.hardware(
                "Display: ",
                String.format(
                        Locale.ROOT,
                        "%dx%d",
                        window.getWidth(),
                        window.getHeight()
                )
        ));
        lines.add(F3Line.hardware("OpenGL: ", GLX.getOpenGLVersionString()));
        lines.add(F3Line.spacer());

        appendTargetBlockLines(client, lines);
        return lines;
    }

    private static void appendTargetBlockLines(Minecraft client, List<F3Line> lines) {
        ClientLevel level = client.level;
        HitResult hitResult = client.hitResult;
        if (level == null || !(hitResult instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos pos = blockHit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        lines.add(F3Line.target(
                "Target Block: ",
                String.format(Locale.ROOT, "%d %d %d", pos.getX(), pos.getY(), pos.getZ())
        ));
        lines.add(F3Line.solid(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), FlowF3Colors.TARGET_VALUE));

        for (Property<?> property : state.getProperties()) {
            lines.add(F3Line.target(
                    property.getName() + ": ",
                    String.valueOf(state.getValue(property))
            ));
        }
    }

    private static String clientLightValues(ClientLevel level, BlockPos blockPos) {
        LevelLightEngine lightEngine = level.getChunkSource().getLightEngine();
        int blockLight = level.getBrightness(LightLayer.BLOCK, blockPos);
        int skyLight = level.getBrightness(LightLayer.SKY, blockPos);
        int combined = lightEngine.getRawBrightness(blockPos, 0);
        return String.format(Locale.ROOT, "%d (%d sky, %d block)", combined, skyLight, blockLight);
    }

    private static String resolveServerName(Minecraft client) {
        if (client.isLocalServer()) {
            return "Integrated Server";
        }

        if (client.getConnection() != null) {
            String brand = client.getConnection().getServerBrand();
            if (brand != null && !brand.isBlank()) {
                return brand;
            }
        }

        if (client.getCurrentServer() != null && client.getCurrentServer().name != null) {
            return client.getCurrentServer().name;
        }

        return "Unknown";
    }

    private static String facingLabel(Direction direction) {
        return switch (direction) {
            case NORTH -> "North -Z";
            case SOUTH -> "South +Z";
            case EAST -> "East +X";
            case WEST -> "West -X";
            case UP -> "Up +Y";
            case DOWN -> "Down -Y";
        };
    }

    private static String biomeName(ClientLevel level, BlockPos blockPos) {
        return level.getBiome(blockPos)
                .unwrapKey()
                .map(key -> key.location().toString())
                .orElse("unknown");
    }

    private static String entityValues(Minecraft client) {
        if (client.level == null) {
            return "0/0";
        }

        int rendered = 0;
        for (var ignored : client.level.entitiesForRendering()) {
            rendered++;
        }

        return rendered + "/?";
    }

    private static int particleCount(Minecraft client) {
        var groups = ((ParticleEngineAccessor) client.particleEngine).flowclient$getParticles();
        int total = 0;
        for (var queue : groups.values()) {
            total += queue.size();
        }
        return total;
    }

    private static int dayCount(Level level) {
        return (int) (level.getDayTime() / 24000L);
    }

    private static String soundsValues(Minecraft client) {
        String debug = client.getSoundManager().getChannelDebugString();
        if (debug.startsWith("Sounds: ")) {
            String payload = debug.substring("Sounds: ".length());
            String[] parts = payload.split(" \\+ ");
            if (parts.length == 2) {
                String[] left = parts[0].split("/");
                String[] right = parts[1].split("/");
                if (left.length == 2 && right.length == 2) {
                    int used = Integer.parseInt(left[0]) + Integer.parseInt(right[0]);
                    int max = Integer.parseInt(left[1]) + Integer.parseInt(right[1]);
                    return used + "/" + max;
                }
            }

            return payload;
        }

        return debug;
    }

    private static int simulationDistance(Minecraft client, ClientLevel level) {
        return client.options.getEffectiveRenderDistance();
    }

    private record F3Segment(String text, int color) {
    }

    private record F3Line(List<F3Segment> segments) {
        static F3Line solid(String text, int color) {
            return new F3Line(List.of(new F3Segment(text, color)));
        }

        static F3Line labeled(String label, String value, int labelColor, int valueColor) {
            return new F3Line(List.of(
                    new F3Segment(label, labelColor),
                    new F3Segment(value, valueColor)
            ));
        }

        static F3Line location(String label, String value) {
            return labeled(label, value, FlowF3Colors.LOC_LABEL, FlowF3Colors.LOC_VALUE);
        }

        static F3Line stat(String label, String value) {
            return labeled(label, value, FlowF3Colors.STAT_LABEL, FlowF3Colors.STAT_VALUE);
        }

        static F3Line hardware(String label, String value) {
            return labeled(label, value, FlowF3Colors.HW_LABEL, FlowF3Colors.HW_VALUE);
        }

        static F3Line target(String label, String value) {
            return labeled(label, value, FlowF3Colors.TARGET_LABEL, FlowF3Colors.TARGET_VALUE);
        }

        static F3Line spacer() {
            return new F3Line(List.of());
        }

        boolean isBlank() {
            return this.segments.isEmpty();
        }

        int width(Font font) {
            int total = 0;
            for (F3Segment segment : this.segments) {
                total += font.width(segment.text());
            }
            return total;
        }
    }
}
