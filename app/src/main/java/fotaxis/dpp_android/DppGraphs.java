package fotaxis.dpp_android;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;


/**
 * Created by awais on 30.09.15.
 */
public class DppGraphs implements OnChartValueSelectedListener{

    Context ctx;
    View view;
    LineChart xleroChart;
    Calendar calendar;
    public DppGraphs(Context ctx) {
        this.ctx=ctx;
        calendar = new GregorianCalendar(); //TODO add time zone later +1Berlin
    }

    public void setView(View view) {
        this.view = view;
    }

    public void setXleroLineChart(float lowerLimit,float upperLimit,int id){
        xleroChart=(LineChart)view.findViewById(id);
        // no description text
        xleroChart.setDescription("");
        xleroChart.setNoDataTextDescription("No chart data available.");
        xleroChart.setOnChartValueSelectedListener(this);
        // enable value highlighting
        //xleroChart.setHighlightEnabled(true);
        // enable touch gestures
        xleroChart.setTouchEnabled(true);

        // enable scaling and dragging
        xleroChart.setDragEnabled(true);
        xleroChart.setScaleEnabled(true);
        xleroChart.setDrawGridBackground(false);

        // if disabled, scaling can be done on x- and y-axis separately
        xleroChart.setPinchZoom(true);

        // set an alternative background color
        xleroChart.setBackgroundColor(Color.LTGRAY);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        MyMarkerView mv = new MyMarkerView(ctx,R.layout.custom_marker_view);
        // set the marker to the chart
        xleroChart.setMarkerView(mv);

        // add empty data
        xleroChart.setData(data);
        Typeface tf = Typeface.createFromAsset(ctx.getAssets(), "OpenSans-Regular.ttf");

        // get the legend (only possible after setting data)
        Legend l = xleroChart.getLegend();

        // modify the legend ...
        // l.setPosition(LegendPosition.LEFT_OF_CHART);
        l.setForm(Legend.LegendForm.LINE);
        l.setTypeface(tf);
        l.setTextColor(Color.WHITE);

        XAxis xl = xleroChart.getXAxis();
        xl.setTypeface(tf);
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setSpaceBetweenLabels(5);
        xl.setEnabled(true);

        YAxis leftAxis = xleroChart.getAxisLeft();
        leftAxis.setTypeface(tf);
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMaxValue(upperLimit);
        leftAxis.setAxisMinValue(lowerLimit);
        leftAxis.setStartAtZero(false);
        leftAxis.setDrawGridLines(true);
        YAxis rightAxis = xleroChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    public void addXleroEntry(final float x, final float y){
        //at the end of the event queue
        view.post(new Runnable() {
            @Override
            public void run() {
                addXleroMeterEntry(x, y);
            }
        });
    }

    public void addGasEntry(final float x, final String label, final int color){
        view.post(new Runnable() {
            @Override
            public void run() {
                addEntry(x, label, color);
            }
        });
    }

    public void add3Gases(final float co, final float co2, final float temp){
        view.post(new Runnable() {
            @Override
            public void run() {
                add3Entries(co,co2,temp);
            }
        });
    }

    private void addEntry(float x,String label,int color){
        LineData data = xleroChart.getData();
        if (data != null) {
            LineDataSet setX = data.getDataSetByIndex(0);
            if (setX == null ) {
                setX=createSet(color, label);
                data.addDataSet(setX);
            }
            // add a new x-value first
            Date trialTime = new Date();
            calendar.setTime(trialTime);
            data.addXValue(calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND));
            data.addEntry(new Entry(x, setX.getEntryCount()), 0);
            // let the chart know it's data has changed
            xleroChart.notifyDataSetChanged();
            // limit the number of visible entries
            xleroChart.setVisibleXRangeMaximum(15);
            xleroChart.moveViewToX(data.getXValCount() - 7);

            if(data.getXValCount()>5000){
                //Toast.makeText(ctx,"clearing boom!",Toast.LENGTH_LONG).show();
                xleroChart.clear();
                LineData newData = new LineData();
                data.setValueTextColor(Color.WHITE);
                xleroChart.setData(newData);
            }
        }
    }

    private void addXleroMeterEntry(float x,float y) {
        LineData data = xleroChart.getData();
        if (data != null) {

            LineDataSet setX = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            LineDataSet setY=data.getDataSetByIndex(1);
            //LineDataSet setZ=data.getDataSetByIndex(2);
            if (setX == null || setY ==null) {
                setX=createSet(Color.RED, "x");
                setY=createSet(Color.GREEN,"y");
              //  setZ=createSet(Color.BLUE, "z");
                data.addDataSet(setX);
                data.addDataSet(setY);
//                data.addDataSet(setZ);
            }

            // add a new x-value first
            Date trialTime = new Date();
            calendar.setTime(trialTime);
            data.addXValue(calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND));
            data.addEntry(new Entry(x, setX.getEntryCount()), 0);
            data.addEntry(new Entry(y, setY.getEntryCount()), 1);
//            data.addEntry(new Entry(z, setZ.getEntryCount()), 2);


            // let the chart know it's data has changed
            xleroChart.notifyDataSetChanged();

            // limit the number of visible entries
            xleroChart.setVisibleXRangeMaximum(15);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry

            xleroChart.moveViewToX(data.getXValCount() - 7);
            // this automatically refreshes the chart (calls invalidate())
            //xleroChart.moveViewTo(data.getXValCount()-7, 55f, YAxis.AxisDependency.LEFT);

            if(data.getXValCount()>5000){
                //Toast.makeText(ctx,"clearing boom!",Toast.LENGTH_LONG).show();
                xleroChart.clear();
                LineData newData = new LineData();
                data.setValueTextColor(Color.WHITE);
                xleroChart.setData(newData);
            }

        }
    }

    private void add3Entries(float co,float co2,float temp) {
        LineData data = xleroChart.getData();
        if (data != null) {
            LineDataSet setX = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            LineDataSet setY=data.getDataSetByIndex(1);
            LineDataSet setZ=data.getDataSetByIndex(2);
            if (setX == null || setY ==null) {
                setX=createSet(Color.RED, "C0");
                setY=createSet(Color.GREEN,"CO2");
                setZ=createSet(Color.BLUE, "Temperature");
                data.addDataSet(setX);
                data.addDataSet(setY);
                data.addDataSet(setZ);
            }

            // add a new x-value first
            Date trialTime = new Date();
            calendar.setTime(trialTime);
            data.addXValue(calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND));
            data.addEntry(new Entry(co, setX.getEntryCount()), 0);
            data.addEntry(new Entry(co2, setY.getEntryCount()), 1);
            data.addEntry(new Entry(temp, setZ.getEntryCount()), 2);

            // let the chart know it's data has changed
            xleroChart.notifyDataSetChanged();

            // limit the number of visible entries
            xleroChart.setVisibleXRangeMaximum(20);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry

            xleroChart.moveViewToX(data.getXValCount() - 10);
            // this automatically refreshes the chart (calls invalidate())
            //xleroChart.moveViewTo(data.getXValCount()-7, 55f, YAxis.AxisDependency.LEFT);

            if(data.getXValCount()>5000){
                //Toast.makeText(ctx,"clearing boom!",Toast.LENGTH_LONG).show();
                xleroChart.clear();
                LineData newData = new LineData();
                data.setValueTextColor(Color.WHITE);
                xleroChart.setData(newData);
            }

        }
    }

    private LineDataSet createSet(int color,String dataLabel) {

        LineDataSet set = new LineDataSet(null, dataLabel);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(color);
        set.setCircleColor(color);
        set.setLineWidth(1.5f);
        set.setCircleSize(3.5f);
        set.setFillAlpha(65);
        set.setFillColor(color);
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(5f);
        set.setDrawValues(true);
        return set;
    }

    public void feedMultiple( final Activity act) {

        new Thread(new Runnable() {

            @Override
            public void run() {
                for(int i = 0; i < 500; i++) {
                    act.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //addEntry();
                            Random rand = new Random();
                            addXleroMeterEntry(rand.nextInt(30) + 1,rand.nextInt(40)+1);
                        }
                    });
                    try {
                        Thread.sleep(35);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }


    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
        Toast.makeText(ctx,e.getVal()+"",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected() {

    }
}
