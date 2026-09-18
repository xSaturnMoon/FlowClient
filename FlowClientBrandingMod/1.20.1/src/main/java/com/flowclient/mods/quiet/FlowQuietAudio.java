package com.flowclient.mods.quiet;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.sounds.SoundSource;

final class FlowQuietAudio {
    private static final Map<SoundSource, Double> savedVolumes = new EnumMap<>(SoundSource.class);
    private static boolean muted;

    private FlowQuietAudio() {
    }

    static void mute(Minecraft client) {
        if (!FlowQuietSettings.get().isMuteAudio() || muted) {
            return;
        }

        muted = true;
        Options options = client.options;
        savedVolumes.clear();

        for (SoundSource source : SoundSource.values()) {
            OptionInstance<Double> volumeOption = options.getSoundSourceOptionInstance(source);
            savedVolumes.put(source, volumeOption.get());
            volumeOption.set(0.0);
        }

        client.getSoundManager().stop();
        client.getMusicManager().stopPlaying();
    }

    static void unmute(Minecraft client) {
        if (!muted) {
            return;
        }

        muted = false;
        Options options = client.options;
        for (Map.Entry<SoundSource, Double> entry : savedVolumes.entrySet()) {
            options.getSoundSourceOptionInstance(entry.getKey()).set(entry.getValue());
        }
        savedVolumes.clear();
    }

    static void forceUnmute(Minecraft client) {
        if (client == null) {
            muted = false;
            savedVolumes.clear();
            return;
        }
        unmute(client);
    }
}
