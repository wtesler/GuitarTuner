package tuner.will.tesler.guitartuner;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;

public class MainWear extends Activity implements SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "MainWear";

    private TextView note;
    private SeekBar A,B,C;
    private View background;
    Button start,stop, smooth;
    private boolean sideways = false;
    private PitchDetector pd;
    private Handler handler = new Handler();
    private TextView pitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_wear);
        //Yin.start(this);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {

                note = (TextView) stub.findViewById(R.id.tv_note);

                A = (SeekBar) stub.findViewById(R.id.seekBar0);
                B = (SeekBar) stub.findViewById(R.id.seekBar1);
                C = (SeekBar) stub.findViewById(R.id.seekBar2);

                A.setOnSeekBarChangeListener(MainWear.this);
                A.setOnSeekBarChangeListener(MainWear.this);
                A.setOnSeekBarChangeListener(MainWear.this);

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

                smooth = (Button) stub.findViewById(R.id.bt_smooth);
                smooth.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pd.toggleSmooth();
                        Button smooth = ((Button)v);
                        smooth.setTextColor(smooth.getCurrentTextColor() == Color.RED ?
                                Color.GREEN : Color.RED);
                    }
                });

                pitch = (TextView) stub.findViewById(R.id.tv_pitch);

                pd = new PitchDetector(handler, A, B, C, pitch, background);
            }
        });

    }

    @Override
    protected void onPause() {
        pd.stop();
        super.onPause();
    }

    private static final int SPEECH_REQUEST_CODE = 0;

    // Create an intent that can start the Speech Recognizer activity
    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    // This callback is invoked when the Speech Recognizer returns.
// This is where you process the intent and extract the speech text from the intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            Log.d(TAG, data.getExtras().toString());
            // Do something with spokenText
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch(seekBar.getId()) {
            case R.id.seekBar0:

                int diff = Math.abs(50 - progress);

                int R = (int) (255* (diff / 50.0));

                int G = (int) (255* (1 - (diff / 50.0)));
                int B = 0;
                background.setBackgroundColor(Color.rgb(R, G, B));
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
