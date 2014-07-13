package tuner.will.tesler.guitartuner;

import com.jjoe64.graphview.LineGraphView;

public interface Listener {

    public void start(LineGraphView graph);

    public void record();

    public void pause();

    public void stopRecording();

    public boolean isRecording();

    public boolean isListening();

}

