package tuner.will.tesler.guitartuner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.jjoe64.graphview.GraphViewStyle;
import com.jjoe64.graphview.LineGraphView;

public class RecordFragment extends android.support.v4.app.Fragment {

    // BluetoothInterface btInterface = new BluetoothInterface();

    // MicrophoneListener micListener = new MicrophoneListener();
    // BTListener btListener;

    Listener listener;

    LineGraphView graph;

    RelativeLayout graphLayout;

    public RecordFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the Fragment
        View rv = inflater.inflate(R.layout.fragment_main, container, false);

        graph = new LineGraphView(rv.getContext(), "Microphone");

        graph.setScrollable(true);
        graph.setViewPort(1, 5000);
        graph.setScalable(false);
        graph.setHorizontalLabels(new String[]{"Time"});
        graph.setVerticalLabels(new String[]{"Noise"});
        GraphViewStyle style = graph.getGraphViewStyle();
        style.setVerticalLabelsWidth(20);
        style.setNumHorizontalLabels(10);
        style.setTextSize(24);
        graph.setGraphViewStyle(style);
        graphLayout = (RelativeLayout) rv.findViewById(R.id.rl_graph);
        graphLayout.addView(graph);

        assignCorrectListener();

        listener.start(graph);


        load();

        return rv;
    }

    @Override
    public void onPause() {
        super.onPause();
        listener.pause();
        listener = null;

        System.gc();
    }

    @Override
    public void onResume() {
        super.onResume();


    }

    public void assignCorrectListener() {

        SeekBar seekBar = new SeekBar(getActivity());
        listener = new MicrophoneListener(seekBar);

    }

    public void load() {

    }

    public void save() {

    }
}

