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
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.HashMap;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class PitchDetector {

   final static String TAG = "PitchDetector";

    // Currently, only this combination of rate, encoding and channel mode
    // actually works on Android.
    private final static int RATE = 8000;
    private final static int CHANNEL_MODE = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private final static int ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private final static double[] stringFreqs = {82.41, 110.0, 146.83, 196.00, 246.94, 329.63};

    private static HashMap<Double, String> stringNames = new HashMap<Double,String>();

    //Each Buffer read in from the Microphone is indicative of 3 seconds passing
    private static int BUFFER_RATE_IN_MS = 3000;

    //Hanning Window used by FFT
    private final static int WINDOW_SIZE_IN_SAMPLES = 4096;
    private final static int WINDOW_SIZE_IN_MS = 1000 * WINDOW_SIZE_IN_SAMPLES / RATE;
    private final static int WINDOW_SIZE_IN_SHORTS = RATE * WINDOW_SIZE_IN_MS / 1000;

    // Each buffer has this much data inside of it.
    private final static int BUFFER_SIZE_IN_BYTES = RATE * BUFFER_RATE_IN_MS / 2048;

    // Range of guitar frequencies.
    private final static int MIN_FREQUENCY = 50; // HZ
    private final static int MAX_FREQUENCY = 500; // HZ - it's for guitar,

    // (TODO) Not sure about this one yet.
    private static int DRAW_FREQUENCY_STEP = 5;

    // This is a microphone, essentially.
    private AudioRecord recorder;

    // Message handler used for logging.
    private Handler handler;

    // Scrubbers for tweaking algorithm constants.
    private SeekBar A, B, C;

    // Displays the current tv_pitch to the screen.
    private TextView tv_pitch;

    private View background;

    // Flow control switches.
    private static volatile boolean shouldRun, shouldSmooth=false;

    // Constructor
    public PitchDetector(Handler handler, SeekBar A, SeekBar B, SeekBar C,
                         TextView pitch, View background) {
        shouldRun = true;
        this.handler = handler;
        this.A = A;
        this.B = B;
        this.C = C;
        this.tv_pitch = pitch;
        this.background = background;

        int i =0;
        stringNames.put(stringFreqs[i++], "E");
        stringNames.put(stringFreqs[i++], "A");
        stringNames.put(stringFreqs[i++], "D");
        stringNames.put(stringFreqs[i++], "G");
        stringNames.put(stringFreqs[i++], "B");
        stringNames.put(stringFreqs[i++], "e");
    }

    // Stop recording and close microphone.
    public void stop(){
        shouldRun = false;
    }

    // Main loop of the class.
    public void run() {

        DRAW_FREQUENCY_STEP = B.getProgress();
        BUFFER_RATE_IN_MS = C.getProgress();

        shouldRun = true;

        // Handles the audio processing on a background thread.
        AsyncTask<Void,Void,Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                // Tell the Android OS how much time this thread deserves.
                android.os.Process
                        .setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

                // Define the recorder here so it's off of the main thread.
                recorder = new AudioRecord(AudioSource.MIC, RATE, CHANNEL_MODE,
                        ENCODING, BUFFER_SIZE_IN_BYTES);

                // Check if Microphone state is good.
                if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "Couldn't initialize microphone!");
                    return null;
                }

                // Stores Raw PCM audio data
                short[] audio_data = new short[BUFFER_SIZE_IN_BYTES / 2];

                // Store Phase audio data
                double[] fft_data = new double[WINDOW_SIZE_IN_SAMPLES * 2];

                // Lower bound on the frequencies the FFT should detect.
                final int min_frequency_fft = Math.round(MIN_FREQUENCY
                        * WINDOW_SIZE_IN_SAMPLES / RATE);

                // Uppers bound on the frequencies the FFT should detect.
                final int max_frequency_fft = Math.round(MAX_FREQUENCY
                        * WINDOW_SIZE_IN_SAMPLES / RATE);

                // Normalization to make sense of a non-zero indexed range.
                final double sqrt_min_max_freq =  Math.pow(MIN_FREQUENCY * MAX_FREQUENCY, 0.5);

                // FFT Class
                DoubleFFT_1D fft = new DoubleFFT_1D(WINDOW_SIZE_IN_SAMPLES);

                //Prepare the Microphone
                recorder.startRecording();

                while (shouldRun) {

                    //Start Reading
                    recorder.read(audio_data, 0, WINDOW_SIZE_IN_SHORTS);

                    //Smooth
                    //if (shouldSmooth) {
                    //smooth(audio_data, (short) 35);
                    //}

                    //Prepare FFT array
                    for (int i = 0; i < WINDOW_SIZE_IN_SAMPLES; i++) {
                        fft_data[i * 2] = audio_data[i];
                        fft_data[i * 2 + 1] = 0;
                    }

                    // Perform FFT
                    fft.realForward(fft_data);

                    // Maps Freqs -> Amplitudes
                    HashMap<Double, Double> freqMap = new HashMap<Double, Double>();

                    // Pitch variables
                    double pitch = min_frequency_fft;
                    double best_amplitude = 0;

                    // (TODO) Not really sure what this does yet.
                    final double draw_frequency_step = ((double) RATE) / WINDOW_SIZE_IN_SAMPLES;

                    // Iterate through the possible frequency range.
                    for (int i = min_frequency_fft; i <= max_frequency_fft; i++) {

                        final double current_frequency = i * draw_frequency_step;

                        final double rel_freq = current_frequency - MIN_FREQUENCY;

                        final double draw_freq =
                                Math.round(rel_freq / DRAW_FREQUENCY_STEP) + MIN_FREQUENCY;

                        // Calculate Amplitude
                        final double real_val = Math.pow(fft_data[i * 2], 2);
                        final double imag_val = Math.pow(fft_data[i * 2 + 1], 2);
                        final double cur_amp = real_val + imag_val;

                        // Normalized Amplitude
                        final double norm_amp = cur_amp * sqrt_min_max_freq / current_frequency;

                        Double freq_amp = freqMap.get(draw_freq);

                        // Lazy Initialize this frequency's amplitude.
                        if (freq_amp == null) {
                            freq_amp = 0.0;
                        }

                        //Parabolic decay of last amplitude
                        final double decayed_amp = Math.pow(cur_amp, 0.5) / draw_frequency_step;

                        // Place newly calculated amplitude
                        freqMap.put(draw_freq, decayed_amp + freq_amp);

                        // Freq with greatest amplitude is our candidate pitch.
                        if (norm_amp > best_amplitude) {
                            pitch = current_frequency;
                            best_amplitude = norm_amp;
                        }
                    }

                    // Let the UI know what has transpired.
                    PostToUI(pitch, best_amplitude, System.currentTimeMillis());
                }

                // Stop and free the microphone.
                recorder.stop();
                recorder.release();

                return null;
            }
        }.execute();
    }

    private void smooth(short[] data, short smoothFactor) {
        short value = data[0]; // start with the first input
        for (int i=1, len= data.length; i<len; ++i){
            short currentValue = data[i];
            value += (currentValue - value) / smoothFactor;
            data[i] = value;
        }
    }

    private static Long timeStart;
    private void PostToUI(
            final double pitch,
            final double amplitude, final long time) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                //A.setProgress((int) pitchVal);
                //DecimalFormat df = new DecimalFormat("#.##");
                //tv_pitch.setText(df.format(pitch));
//                long timeDiff;
//                if (timeStart == null) {
//                    timeStart = time;
//                    timeDiff = 0;
//                } else {
//                    timeDiff = time - timeStart;
//                }
                Log.d(TAG, "Pitch: " +  pitch + " Amplitude: " + amplitude);
                //Log.d(TAG, "Time: " + timeDiff);
//                for (Map.Entry<Double,Double> freq  : frequencies.entrySet()){
//                    String key = freq.getKey().toString();
//                    String value = freq.getValue().toString();
//                    //Log.d(TAG, key + " " + value);
//                }

                // Is the amplitude over 10 billion? (crazy I know).
                if (amplitude > 10000000000.0) {

                    double doublePitch = pitch * 2;
                    double halfPitch = pitch / 2;

                    for (double freq : stringFreqs) {
                        if (pitch > freq - 5 && pitch < freq + 5) {
                            Log.d(TAG, stringNames.get(freq));
                            display_color(pitch, freq);
                            return;
                        }
                    }
                    for (double freq : stringFreqs) {
                        if (doublePitch > freq - 5 && doublePitch < freq + 5) {
                            Log.d(TAG, stringNames.get(freq));
                            display_color(doublePitch, freq);
                            return;
                        }
                    }
                    for (double freq : stringFreqs) {
                        if (halfPitch > freq - 5 && halfPitch < freq + 5) {
                            Log.d(TAG, stringNames.get(freq));
                            display_color(halfPitch, freq);
                            return;
                        }
                    }
                }
            }
        });
    }

    public void toggleSmooth() {
        shouldSmooth = !shouldSmooth;
    }

    private void display_color(double pitch, double targetPitch) {
        tv_pitch.setText(stringNames.get(targetPitch));

        double diff = pitch - targetPitch;

        A.setProgress(50 + ((int) diff * 10));

        Log.d(TAG, "Pitch difference: " + diff);
//        double R = diff * 51;
//        double G = 255 - (diff * 51);
//        int B = 0;
//        background.setBackgroundColor(Color.rgb((int)R,(int) G, B));
    }
}
