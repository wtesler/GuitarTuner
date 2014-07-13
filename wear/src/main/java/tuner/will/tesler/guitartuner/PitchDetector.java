package tuner.will.tesler.guitartuner;

/** Copyright (C) 2009 by Aleksey Surkov.
 **
 ** Permission to use, copy, modify, and distribute this software and its
 ** documentation for any purpose and without fee is hereby granted, provided
 ** that the above copyright notice appear in all copies and that both that
 ** copyright notice and this permission notice appear in supporting
 ** documentation.  This software is provided "as is" without express or
 ** implied warranty.
 */


import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Process;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.HashMap;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class PitchDetector {
    // Currently, only this combination of rate, encoding and channel mode
    // actually works.
    private final static int RATE = 8000;
    private final static int CHANNEL_MODE = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private final static int ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private final static int BUFFER_SIZE_IN_MS = 3000;
    private final static int CHUNK_SIZE_IN_SAMPLES = 4096; // = 2 ^
    // CHUNK_SIZE_IN_SAMPLES_POW2
    private final static int CHUNK_SIZE_IN_MS = 1000 * CHUNK_SIZE_IN_SAMPLES
            / RATE;
    private final static int BUFFER_SIZE_IN_BYTES = RATE * BUFFER_SIZE_IN_MS
            / 1000 * 2;
    private final static int CHUNK_SIZE_IN_BYTES = RATE * CHUNK_SIZE_IN_MS
            / 1000 * 2;
    private final static int MIN_FREQUENCY = 50; // HZ
    private final static int MAX_FREQUENCY = 600; // HZ - it's for guitar,
    // should be enough
    private final static int DRAW_FREQUENCY_STEP = 5;

    private AudioRecord recorder;
    private Handler handler;
    private SeekBar seekBar;
    private TextView pitch;
    private static volatile boolean shouldRun;

    public native void DoFFT(double[] data, int size);  // an NDK library 'fft-jni'

    public PitchDetector(Handler handler, SeekBar seekBar, TextView pitch) {
        shouldRun = true;
        this.handler = handler;
        this.seekBar = seekBar;
        this.pitch = pitch;
    }

    public void stop(){
        shouldRun = false;
    }

    public void run() {

        AsyncTask<Void,Void,Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                android.os.Process
                        .setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
                recorder = new AudioRecord(AudioSource.MIC, RATE, CHANNEL_MODE,
                        ENCODING, 6144);
                if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                    return null;
                }
                short[] audio_data = new short[BUFFER_SIZE_IN_BYTES / 2];
                double[] data = new double[CHUNK_SIZE_IN_SAMPLES * 2];
                final int min_frequency_fft = Math.round(MIN_FREQUENCY
                        * CHUNK_SIZE_IN_SAMPLES / RATE);
                final int max_frequency_fft = Math.round(MAX_FREQUENCY
                        * CHUNK_SIZE_IN_SAMPLES / RATE);
                recorder.startRecording();
                while (shouldRun) {
                    recorder.read(audio_data, 0, CHUNK_SIZE_IN_BYTES / 2);
                    for (int i = 0; i < CHUNK_SIZE_IN_SAMPLES; i++) {
                        data[i * 2] = audio_data[i];
                        data[i * 2 + 1] = 0;
                    }
                    DoubleFFT_1D fft = new DoubleFFT_1D(CHUNK_SIZE_IN_SAMPLES);
                    fft.realForward(data);

                    double best_frequency = min_frequency_fft;
                    double best_amplitude = 0;
                    HashMap<Double, Double> frequencies = new HashMap<Double, Double>();
                    final double draw_frequency_step = 1.0 * RATE
                            / CHUNK_SIZE_IN_SAMPLES;
                    for (int i = min_frequency_fft; i <= max_frequency_fft; i++) {
                        final double current_frequency = i * 1.0 * RATE
                                / CHUNK_SIZE_IN_SAMPLES;
                        final double draw_frequency = Math
                                .round((current_frequency - MIN_FREQUENCY)
                                        / DRAW_FREQUENCY_STEP)
                                * DRAW_FREQUENCY_STEP + MIN_FREQUENCY;
                        final double current_amplitude = Math.pow(data[i * 2], 2)
                                + Math.pow(data[i * 2 + 1], 2);
                        final double normalized_amplitude = current_amplitude *
                                Math.pow(MIN_FREQUENCY * MAX_FREQUENCY, 0.5) / current_frequency;
                        Double current_sum_for_this_slot = frequencies
                                .get(draw_frequency);
                        if (current_sum_for_this_slot == null)
                            current_sum_for_this_slot = 0.0;
                        frequencies.put(draw_frequency, Math
                                .pow(current_amplitude, 0.5)
                                / draw_frequency_step + current_sum_for_this_slot);
                        if (normalized_amplitude > best_amplitude) {
                            best_frequency = current_frequency;
                            best_amplitude = normalized_amplitude;
                        }
                    }
                    PostToUI(frequencies, best_frequency);
                }

                recorder.stop();
                recorder.release();

                return null;
            }
        }.execute();
    }

    private void PostToUI(
            final HashMap<Double, Double> frequencies,
            final double pitchVal) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                seekBar.setProgress((int) pitchVal);
                pitch.setText(Integer.toString((int)pitchVal));
                Log.d("PitchDetector", "Pitch: " + pitchVal);
            }
        });
    }
}
