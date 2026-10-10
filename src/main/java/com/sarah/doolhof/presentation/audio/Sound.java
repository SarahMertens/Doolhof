package com.sarah.doolhof.presentation.audio;

import com.sarah.doolhof.common.GameEvent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public final class Sound {

    private static final float RATE = 22050f;
    private static final AudioFormat FORMAT = new AudioFormat(RATE, 16, 1, true, false);

    public enum Effect { KEY, DOOR, FLARE, ALERT, CAUGHT, WARNING, SHIFT, WON, LOST, HEARTBEAT }

    private final Map<Effect, Clip> clips = new EnumMap<>(Effect.class);
    private volatile boolean ready;
    private boolean muted;

    public Sound() {
        // Building the clips takes a moment; do it off the user interface thread.
        Thread loader = new Thread(this::load, "sound-loader");
        loader.setDaemon(true);
        loader.start();
    }

    public boolean isMuted() {
        return muted;
    }

    public void toggleMute() {
        muted = !muted;
    }

    public void play(List<GameEvent> events) {
        for (GameEvent event : events) {
            switch (event) {
                case KEY_COLLECTED -> play(Effect.KEY);
                case DOOR_OPENED -> play(Effect.DOOR);
                case FLARE_FIRED -> play(Effect.FLARE);
                case HUNTER_ALERTED -> play(Effect.ALERT);
                case PLAYER_CAUGHT -> play(Effect.CAUGHT);
                case SHIFT_WARNING -> play(Effect.WARNING);
                case WALLS_SHIFTED -> play(Effect.SHIFT);
                case LEVEL_WON -> play(Effect.WON);
                case LEVEL_LOST -> play(Effect.LOST);
            }
        }
    }

    public void play(Effect effect) {
        if (!ready || muted) {
            return;
        }
        try {
            Clip clip = clips.get(effect);
            clip.stop();
            clip.setFramePosition(0);
            clip.start();
        } catch (RuntimeException unavailable) {
            ready = false;
        }
    }

    private void load() {
        try {
            Random noise = new Random(7);

            clips.put(Effect.KEY, clip(join(tone(880, 0.07, 0.5), tone(1320, 0.16, 0.5))));
            clips.put(Effect.DOOR, clip(join(tone(523, 0.09, 0.45), tone(659, 0.09, 0.45), tone(784, 0.09, 0.45), tone(1047, 0.25, 0.45))));
            clips.put(Effect.FLARE, clip(mix(noise(noise, 0.5, 0.5), sweep(1800, 300, 0.5, 0.3))));
            clips.put(Effect.ALERT, clip(sweep(280, 620, 0.14, 0.45)));
            clips.put(Effect.CAUGHT, clip(mix(sweep(160, 45, 0.6, 0.7), noise(noise, 0.25, 0.3))));
            clips.put(Effect.WARNING, clip(tremolo(tone(58, 1.1, 0.7), 11)));
            clips.put(Effect.SHIFT, clip(mix(sweep(90, 35, 0.5, 0.8), noise(noise, 0.35, 0.35))));
            clips.put(Effect.WON, clip(join(tone(523, 0.1, 0.45), tone(659, 0.1, 0.45), tone(784, 0.1, 0.45), tone(1047, 0.1, 0.45), tone(1319, 0.4, 0.45))));
            clips.put(Effect.LOST, clip(join(tone(392, 0.18, 0.5), tone(330, 0.18, 0.5), tone(262, 0.18, 0.5), tone(196, 0.5, 0.5))));
            clips.put(Effect.HEARTBEAT, clip(join(tone(52, 0.09, 0.9), silence(0.07), tone(46, 0.12, 0.7))));

            ready = true;
        } catch (Exception | LinkageError noAudio) {
            // No sound device, or no sound support at all: play without sound.
            ready = false;
        }
    }

    // --- A tiny synthesizer ----------------------------------------------------

    private static double[] tone(double frequency, double seconds, double volume) {
        return sweep(frequency, frequency, seconds, volume);
    }

    private static double[] sweep(double from, double to, double seconds, double volume) {
        int length = (int) (seconds * RATE);
        double[] samples = new double[length];
        double phase = 0;

        for (int i = 0; i < length; i++) {
            double progress = (double) i / length;
            phase += 2 * Math.PI * (from + (to - from) * progress) / RATE;
            samples[i] = Math.sin(phase) * volume * envelope(progress);
        }
        return samples;
    }

    private static double[] noise(Random random, double seconds, double volume) {
        int length = (int) (seconds * RATE);
        double[] samples = new double[length];
        double smoothed = 0;

        for (int i = 0; i < length; i++) {
            // Averaging with the previous value takes the hiss off.
            smoothed = smoothed * 0.7 + (random.nextDouble() * 2 - 1) * 0.3;
            samples[i] = smoothed * volume * 2 * envelope((double) i / length);
        }
        return samples;
    }

    private static double[] silence(double seconds) {
        return new double[(int) (seconds * RATE)];
    }

    private static double[] tremolo(double[] samples, double timesPerSecond) {
        double[] result = new double[samples.length];
        for (int i = 0; i < samples.length; i++) {
            result[i] = samples[i] * (0.6 + 0.4 * Math.sin(2 * Math.PI * timesPerSecond * i / RATE));
        }
        return result;
    }

    private static double envelope(double progress) {
        return Math.min(1, progress * 40) * Math.pow(1 - progress, 1.5);
    }

    private static double[] join(double[]... parts) {
        int length = 0;
        for (double[] part : parts) {
            length += part.length;
        }

        double[] result = new double[length];
        int offset = 0;
        for (double[] part : parts) {
            System.arraycopy(part, 0, result, offset, part.length);
            offset += part.length;
        }
        return result;
    }

    private static double[] mix(double[] a, double[] b) {
        double[] result = new double[Math.max(a.length, b.length)];
        for (int i = 0; i < result.length; i++) {
            result[i] = (i < a.length ? a[i] : 0) + (i < b.length ? b[i] : 0);
        }
        return result;
    }

    private static Clip clip(double[] samples) throws Exception {
        byte[] bytes = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            short value = (short) (Math.clamp(samples[i], -1, 1) * Short.MAX_VALUE);
            bytes[i * 2] = (byte) value;
            bytes[i * 2 + 1] = (byte) (value >> 8);
        }

        Clip clip = AudioSystem.getClip();
        clip.open(FORMAT, bytes, 0, bytes.length);
        return clip;
    }
}
