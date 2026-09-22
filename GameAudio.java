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
            double phase = 2.0 * Math.PI * frequency * time;
            double squareWave = Math.signum(Math.sin(phase));
            double pulse = 0.72 + 0.28 * Math.sin(phase * 2.0);
            double wave = frequency > 0 ? squareWave * pulse : 0.0;
            double envelope = 1.0 - ((double) i / totalSamples);
            double stepped = Math.round(wave * volume * 127.0 * envelope / 8.0) * 8.0;
            data[i] = (byte) Math.max(-128, Math.min(127, stepped));
        }

        return data;
    }

    public byte[] createBackgroundLoop() {
        int[] melody = {220, 277, 330, 392, 330, 277, 262, 220, 247, 330, 392, 440, 392, 330, 277, 220};
        int[] bass = {55, 55, 73, 73, 82, 82, 73, 55, 55, 73, 73, 82, 82, 73, 55, 49};
        int[] harmony = {330, 392, 440, 392, 330, 294, 262, 330, 392, 440, 392, 330, 294, 262, 294, 330};
        int[] arpeggio = {659, 587, 523, 659, 587, 523, 466, 392, 523, 587, 659, 587, 523, 494, 440, 392};
        int noteDuration = 100;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (int i = 0; i < melody.length; i++) {
            byte[] bassData = createToneData(bass[i], noteDuration, 0.08);
            byte[] melodyData = createToneData(melody[i], noteDuration, 0.12);
            byte[] harmonyData = createToneData(harmony[i], noteDuration / 2, 0.05);
            byte[] arpeggioData = createToneData(arpeggio[i], noteDuration / 3, 0.04);

            out.write(bassData, 0, bassData.length);
            out.write(melodyData, 0, melodyData.length);
            out.write(arpeggioData, 0, arpeggioData.length);
            out.write(harmonyData, 0, harmonyData.length);

            byte[] rest = createToneData(0, 18, 0.0);
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
