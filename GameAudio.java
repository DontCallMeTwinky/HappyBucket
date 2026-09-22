import java.io.ByteArrayOutputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;

public class GameAudio {
    private static final float AUDIO_SAMPLE_RATE = 22050f;
    private Clip backgroundMusicClip;

    public byte[] createToneData(int frequency, int durationMs, double volume) {
        int totalSamples = (int) (AUDIO_SAMPLE_RATE * durationMs / 1000.0);
        byte[] data = new byte[totalSamples];

        for (int i = 0; i < totalSamples; i++) {
            double time = i / AUDIO_SAMPLE_RATE;
            double wave = frequency > 0 ? Math.sin(2.0 * Math.PI * frequency * time) : 0.0;
            double envelope = 1.0 - ((double) i / totalSamples);
            data[i] = (byte) (wave * volume * 127.0 * envelope);
        }

        return data;
    }

    public byte[] createBackgroundLoop() {
        int[] melody = {220, 220, 277, 330, 392, 330, 277, 262, 220, 196, 174, 196};
        int[] bass = {55, 55, 73, 73, 82, 82, 73, 73, 55, 55, 49, 49};
        int[] harmony = {330, 392, 440, 392, 330, 294, 262, 294, 330, 392, 440, 392};
        int noteDuration = 120;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (int i = 0; i < melody.length; i++) {
            byte[] bassData = createToneData(bass[i], noteDuration, 0.07);
            byte[] melodyData = createToneData(melody[i], noteDuration, 0.11);
            byte[] harmonyData = createToneData(harmony[i], noteDuration / 2, 0.05);

            out.write(bassData, 0, bassData.length);
            out.write(melodyData, 0, melodyData.length);
            out.write(harmonyData, 0, harmonyData.length);

            byte[] rest = createToneData(0, 25, 0.0);
            out.write(rest, 0, rest.length);
        }

        return out.toByteArray();
    }

    public void playToneEffect(int frequency, int durationMs, double volume) {
        try {
            AudioFormat format = new AudioFormat(AUDIO_SAMPLE_RATE, 8, 1, true, false);
            byte[] toneData = createToneData(frequency, durationMs, volume);
            Clip clip = AudioSystem.getClip();
            clip.open(format, toneData, 0, toneData.length);
            clip.start();

            Thread cleanup = new Thread(() -> {
                try {
                    Thread.sleep(durationMs);
                } catch (InterruptedException ignored) {
                } finally {
                    clip.stop();
                    clip.close();
                }
            });
            cleanup.setDaemon(true);
            cleanup.start();
        } catch (LineUnavailableException ignored) {
        }
    }

    public void startBackgroundMusic() {
        try {
            if (backgroundMusicClip != null && backgroundMusicClip.isRunning()) {
                return;
            }

            AudioFormat format = new AudioFormat(AUDIO_SAMPLE_RATE, 8, 1, true, false);
            byte[] musicData = createBackgroundLoop();
            backgroundMusicClip = AudioSystem.getClip();
            backgroundMusicClip.open(format, musicData, 0, musicData.length);
            backgroundMusicClip.loop(-1);
        } catch (Exception ignored) {
        }
    }

    public void stopBackgroundMusic() {
        if (backgroundMusicClip != null && backgroundMusicClip.isOpen()) {
            backgroundMusicClip.stop();
            backgroundMusicClip.close();
        }
    }
}
