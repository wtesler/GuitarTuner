package tuner.will.tesler.guitartuner;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.jjoe64.graphview.LineGraphView;


public class Main extends Activity {

    private static final String TAG = "Main";

    // private static Yin yin;

    private static LineGraphView graph;

    private static RelativeLayout graphLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button yo = (Button) findViewById(R.id.yo_btn);
        yo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Yin.stop();
            }
        });


//        graph = new LineGraphView(this, "Bluetooth");
//
//        graph.setScrollable(true);
//        graph.setViewPort(1, 5000);
//        graph.setScalable(false);
//        graph.setHorizontalLabels(new String[] { "Time" });
//        graph.setVerticalLabels(new String[] { "Noise" });
//        GraphViewStyle style = graph.getGraphViewStyle();
//        style.setVerticalLabelsWidth(20);
//        style.setNumHorizontalLabels(10);
//        style.setTextSize(24);
//        graph.setGraphViewStyle(style);
//        graphLayout = (RelativeLayout) findViewById(R.id.rl_graph);
//        graphLayout.addView(graph);

        Yin.start();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        Yin.stop();
        super.onPause();
    }
}
