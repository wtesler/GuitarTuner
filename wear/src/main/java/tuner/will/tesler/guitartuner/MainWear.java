package tuner.will.tesler.guitartuner;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashMap;

public class MainWear extends Activity {

    private static final String TAG = "MainWear";

    private TextView tv_note;
    private View background;
    Button start,stop;
    //private boolean sideways = false;
    private PitchDetector pd;
    private TextView tv_pitch;

    private Handler mHandler;

    private final static double[] stringFreqs = {82.41, 110.0, 146.83, 196.00, 246.94, 329.63};

    private static HashMap<Double, String> stringNames = new HashMap<Double,String>();

    PitchDetector.PitchDetectedListener listener = new PitchDetector.PitchDetectedListener() {
        @Override
        public void onPitchDetected(double pitch, double amplitude) {
            Log.d(TAG, "Pitch: " + pitch + " Amplitude: " + amplitude);
//                for (Map.Entry<Double,Double> freq  : frequencies.entrySet()){
//                    String key = freq.getKey().toString();
//                    String value = freq.getValue().toString();
//                    //Log.d(TAG, key + " " + value);
//                }

            // Is the amplitude over 1 million? (crazy I know).
            if (amplitude > 1000000.0) {

                double doublePitch = pitch * 2;
                double halfPitch = pitch / 2;

                for (double freq : stringFreqs) {
                    if (pitch > freq - 3 && pitch < freq + 3) {
                        Log.d(TAG, stringNames.get(freq) + " with pitch of " + pitch);
                        display_color(pitch, freq);
                        return;
                    }
                }
                for (double freq : stringFreqs) {
                    if (doublePitch > freq - 3 && doublePitch < freq + 3) {
                        Log.d(TAG, stringNames.get(freq) + " with pitch of " + pitch);
                        display_color(doublePitch, freq);
                        return;
                    }
                }
                for (double freq : stringFreqs) {
                    if (halfPitch > freq - 3 && halfPitch < freq + 3) {
                        Log.d(TAG, stringNames.get(freq) + " with pitch of " + pitch);
                        display_color(halfPitch, freq);
                        return;
                    }
                }
            }
        }
    };

    private void display_color(final double pitch, final double freq) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                tv_note.setText(stringNames.get(freq));
                tv_pitch.setText(Double.toString(pitch));
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_wear);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {

                mHandler = new Handler(getMainLooper());

                tv_note = (TextView) stub.findViewById(R.id.tv_note);

                int i =0;
                stringNames.put(stringFreqs[i++], "E");
                stringNames.put(stringFreqs[i++], "A");
                stringNames.put(stringFreqs[i++], "D");
                stringNames.put(stringFreqs[i++], "G");
                stringNames.put(stringFreqs[i++], "B");
                stringNames.put(stringFreqs[i], "e");

                background = findViewById(R.id.rl_frame);

                start = (Button) stub.findViewById(R.id.bt_start);
                start.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pd.run();
                    }
                });

                stop = (Button) stub.findViewById(R.id.bt_stop);
                stop.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pd.stop();
                    }
                });

                tv_pitch = (TextView) stub.findViewById(R.id.tv_pitch);
                tv_note = (TextView) stub.findViewById(R.id.tv_note);
            }
        });

        pd = new PitchDetector(listener);

    }

    @Override
    protected void onPause() {
        pd.stop();
        super.onPause();
    }
}
