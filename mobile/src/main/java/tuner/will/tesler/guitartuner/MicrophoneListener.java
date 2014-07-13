package tuner.will.tesler.guitartuner;

import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.SeekBar;

import com.jjoe64.graphview.GraphViewDataInterface;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.LineGraphView;

public class MicrophoneListener implements Listener {

    // This acts as the Audio Input Stream. Using read() to get info.
    AudioRecord ar;

    // true when read audio should halt
    boolean shouldStopListening = false;

    // this gets set by initCorrectAudioRecord
    private int bufferSize;

    // holds the packet at each read() interval
    short[] packet;

    // Reflects changes to the graph
    Handler graphHandler = new Handler();

    // The Graph itself, passed in from Record Fragment
    LineGraphView graph;

    // Container that holds the graph's data
    GraphViewSeries series;

    // Current X position on the graph
    Long curX;

    SeekBar volume;

    boolean isListening = false;
    boolean isRecording = false;

    public MicrophoneListener(SeekBar volume) {
        this.volume = volume;
    }

    @Override
    public void start(LineGraphView graph) {

        // Graph
        this.graph = graph;

        // Graph Content
        series = new GraphViewSeries(new GraphViewData[] {});

        // attach the two
        this.graph.addSeries(series);

        // Audio Input Stream
        ar = initCorrectAudioRecord();

        // this stores the received packet at each sample
        packet = new short[bufferSize];

        // Audio Input Stream Begin
        ar.startRecording();

        // Begin Sampling the Audio Input Stream
        ReadAudioAsync raa = new ReadAudioAsync();
        raa.execute();

    }

    // Create the Audio Input Stream and begin reading
    @Override
    public void record() {

        GraphViewSeriesStyle gvss = new GraphViewSeriesStyle(Color.RED, 3);
        series = new GraphViewSeries("Cosinus curve", gvss,
                new GraphViewData[] {});

        this.graph.addSeries(series);

        // This will trigger a cue in the ReadAudioAsync that will make it start
        // saving
        isRecording = true;

    }

    @Override
    public void stopRecording() {

        if (isRecording) {

            isRecording = false;

            series = new GraphViewSeries(new GraphViewData[] {});

            this.graph.addSeries(series);

        }
    }

    @Override
    public void pause() {
        shouldStopListening = true;
    }

    // This class has an infinite loop inside of the doInBackground. It uses a
    // blocking operation called read() which happens at a sample rate defined
    // by the AudioRecord
    private class ReadAudioAsync extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            isListening = true;
        }

        @Override
        protected String doInBackground(String... arg0) {

            // INFINITE LOOP
            while (true) {

                // Base Case: Stop reading.
                if (shouldStopListening) {
                    if (ar != null) {
                        ar.stop();
                        ar.release();
                        ar = null;
                    }
                    shouldStopListening = false;
                    break;
                }

                // Read a packet in from the audio capture
                readPacket(packet, 0, bufferSize);

                //adjustForVolume(packet);

                if (curX == null) {
                    curX = System.currentTimeMillis();
                }

                long diff = System.currentTimeMillis() - curX;

                curX += diff;

                // Log.i("Time Diff", Long.toString(diff));
                //
                // Log.w("Packet size", Integer.toString(packet.length));

                for (int i = 0; i < packet.length;) {

                    // Log.d("packet value", Byte.toString(bytes[j]));
                    double progress = 1;
                    if (volume.getProgress() != 0) {
                        progress = (double) volume.getProgress() / 10;
                    }

                    // Update the Graph with the data from the packet
                    double Db = 60 * Math
                            .log10((Math.abs(packet[i]) / (500.0 * progress)));

                    Log.d("Audio", Double.toString(Db));

                    // Threshold at which sound is revealed to the graph
                    if (Db > 1) {
                        updateGraph(new GraphViewData(curX, Db));
                    } else {
                        updateGraph(new GraphViewData(curX, 0));
                    }

                    break;

                }

            }

            return "SUCCESS";
        }

        @Override
        protected void onPostExecute(String state) {
            super.onPostExecute(state);

            isListening = false;

            if (state.contentEquals("SUCCESS")) {
            }
            if (state.contentEquals("FAILURE")) {
            }
        }
    }

    private void adjustForVolume(short[] packet) {
        double multiplier = (double) volume.getProgress() / 10;
        if (multiplier != 1) {
            for (int i = 0; i < packet.length; i++) {
                packet[i] *= multiplier;
            }
        }
    }

    //
    public void updateGraph(final GraphViewData data) {
        graphHandler.post(new Runnable() {
            @Override
            public void run() {
                series.appendData(data, true, 1000);
            }
        });
    }

    // Read in a packet into the buffer
    private void readPacket(short[] data, int off, int length) {
        try {
            int read;
            while (length > 0) {
                read = ar.read(data, off, length);
                length -= read;
                off += read;
            }
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }
    }

    // Graph Node Object
    static class GraphViewData implements GraphViewDataInterface {
        long x;
        double y;

        public GraphViewData(long x, double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public double getX() {
            return x;
        }

        @Override
        public double getY() {
            return y;
        }
    }

    // Utility Method: Configures the Microphone correctly, more aptly...
    // Configure The Audio Record to the correct settings for the device and
    // return an Audio Record Object
    public AudioRecord initCorrectAudioRecord() {
        int[] mSampleRates = new int[] { 8000, 11025, 22050, 44100 };
        for (int rate : mSampleRates) {
            for (short audioFormat : new short[] {
                    AudioFormat.ENCODING_PCM_8BIT,
                    AudioFormat.ENCODING_PCM_16BIT }) {
                for (short channelConfig : new short[] {
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.CHANNEL_IN_STEREO }) {
                    try {
                        Log.d("Determining rate...", "Attempting rate " + rate
                                + "Hz, bits: " + audioFormat + ", channel: "
                                + channelConfig);
                        bufferSize = AudioRecord.getMinBufferSize(rate,
                                channelConfig, audioFormat);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            AudioRecord recorder = new AudioRecord(
                                    AudioSource.DEFAULT, rate, channelConfig,
                                    audioFormat, bufferSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
                                return recorder;
                        }
                    } catch (Exception e) {
                        Log.e("Determining rate...", rate
                                + "Exception, keep trying.", e);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean isRecording() {
        return isRecording;
    }

    @Override
    public boolean isListening() {
        return isListening;
    }
}
