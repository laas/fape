package fape.util;

import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.category.LevelRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import planstack.anml.model.concrete.Action;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class ActionsChart {

    public static boolean initialized = false;
    public static DefaultCategoryDataset bardataset = null;
    public static ValueMarker timeMarker = null;
    public static JFreeChart barchart = null;

    public static void init() {
        if(initialized)
            return;
        bardataset = new DefaultCategoryDataset();
        timeMarker = new ValueMarker(0);
        timeMarker.setPaint(Color.black);
        timeMarker.setLabel("currentTime");

        barchart = ChartFactory.createStackedBarChart(
                "Planned actions",      //Title
                "Actions",             // X-axis Label
                "Time",               // Y-axis Label
                bardataset,             // Dataset
                PlotOrientation.HORIZONTAL,      //Plot orientation
                false,                // Show legend
                true,                // Use tooltips
                false                // Generate URLs
        );

        CategoryPlot plot = (CategoryPlot) barchart.getPlot();

        LevelRenderer renderer1 = new LevelRenderer();
        renderer1.setSeriesPaint(0, Color.black);
        plot.setRenderer(1, renderer1);

        plot.addRangeMarker(timeMarker);

        StackedBarRenderer rend = (StackedBarRenderer) plot.getRenderer();
        rend.setSeriesPaint(0, new Color(0, 0, 0, 0)); //Transparent for start
        rend.setSeriesPaint(1, Color.gray);
        rend.setSeriesPaint(2, Color.lightGray);
        rend.setSeriesPaint(3, Color.green);
        rend.setSeriesPaint(4, Color.red);

        barchart.getTitle().setPaint(Color.BLUE);    // Set the colour of the title
//        barchart.setBackgroundPaint(Color.BLACK);    // Set the background colour of the chart
        CategoryPlot cp = barchart.getCategoryPlot();  // Get the Plot object for a bar graph
//        cp.setBackgroundPaint(Color.BLACK);       // Set the plot background colour
        cp.setRangeGridlinePaint(Color.RED);      // Set the colour of the plot gridlines

        showchart(barchart, "FAPE: Actions");

        initialized = true;
    }

    public static void addPendingAction(String name, int start, int minDur, int maxDur) {
        addAction(name, start, minDur, maxDur, 0, 0);
    }

    public static void addExecutedAction(String name, int start, int end) {
        addAction(name, start, 0, 0, end-start, 0);
    }

    public static void addFailedAction(String name, int start, int end) {
        addAction(name, start, 0, 0, 0, end-start);
    }

    private static void addAction(String name, int start, int minDur, int maxDur, int realDur, int failDur) {
        init();
        bardataset.setValue(start, "start", name);
        bardataset.setValue(minDur, "minDuration", name);
        bardataset.setValue(maxDur-minDur, "durUncertainty", name);
        bardataset.setValue(realDur, "realDur", name);
        bardataset.setValue(failDur, "failDur", name);
    }

    public static void setCurrentTime(int currentTime) {
        init();
        timeMarker.setValue(currentTime);
    }

    public static void main(String[] args) {
        addPendingAction("Go(from,to,PR2", 10, 5, 7);
        addPendingAction("Pick(PR2, Cup, Kitchen)", 15, 7, 9);
        addPendingAction("1Go(from,to,PR2", 10, 5, 7);
        addPendingAction("1Pick(PR2, Cup, Kitchen)", 15, 7, 9);
        addPendingAction("2Go(from,to,PR2", 10, 5, 7);
        addPendingAction("2Pick(PR2, Cup, Kitchen)", 15, 7, 9);
        addPendingAction("3Go(from,to,PR2", 10, 5, 7);
        addPendingAction("3Pick(PR2, Cup, Kitchen)", 15, 7, 9);



        try {
            Thread.sleep(2000);
        } catch (Exception e) {}
        bardataset.setValue(20,"start" ,"Go(Ra, L1, L2)" );
        addPendingAction("Go(from,to,PR2", 12, 5, 7);
        addExecutedAction("2Go(from,to,PR2", 11, 25);
        addFailedAction("3Go(from,to,PR2", 13, 19);

        setCurrentTime(20);
    }

    public static void showchart(JFreeChart chart,String title){
        JFrame plotframe=new JFrame();
        ChartPanel cp=new ChartPanel(chart);
        cp.setPreferredSize(new Dimension(300,300));
        Box perfPanel=Box.createVerticalBox();
        perfPanel.add(cp);
        plotframe.setContentPane(new ChartPanel(chart));
        plotframe.setTitle(title);
        plotframe.setSize(640,430);
        plotframe.setVisible(true);
        plotframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    static public void displayState(final State st) {
        init();
        bardataset.clear();
        java.util.List<Action> acts = new LinkedList<>(st.getAllActions());
        Collections.sort(acts, new Comparator<Action>() {
            @Override
            public int compare(Action a1, Action a2) {
                return (int) (st.getEarliestStartTime(a1.start()) - st.getEarliestStartTime(a2.start()));
            }
        });
        setCurrentTime(st.getEarliestStartTime(st.pb.earliestExecution()));

        for(Action a : acts) {
            int start = st.getEarliestStartTime(a.start());
            int earliestEnd = st.getEarliestStartTime(a.end());
            String name = Printer.action(st, a);
            switch (a.status()) {
                case EXECUTED:
                    ActionsChart.addExecutedAction(name, start, earliestEnd);
                    break;
                case EXECUTING:
                case PENDING:
                    if(st.getDurationBounds(a).nonEmpty()) {
                        int min = st.getDurationBounds(a).get()._1();
                        int max = st.getDurationBounds(a).get()._2();
                        ActionsChart.addPendingAction(name, start, min, max);
                    } else {
                        ActionsChart.addPendingAction(name, start, earliestEnd - start, earliestEnd-start);
                    }
                    break;
                case FAILED:
                    ActionsChart.addFailedAction(name, start, earliestEnd);
                    break;
            }
        }
    }
}
