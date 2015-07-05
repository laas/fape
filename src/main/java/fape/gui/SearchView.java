package fape.gui;

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import fape.drawing.TimedCanvas;
import fape.drawing.gui.ChartWindow;
import prefuse.Visualization;
import prefuse.controls.ControlAdapter;
import prefuse.controls.ToolTipControl;
import prefuse.data.Node;
import prefuse.data.Table;
import prefuse.data.Tree;
import prefuse.data.tuple.TupleSet;
import prefuse.util.FontLib;
import prefuse.util.ui.JFastLabel;
import prefuse.visual.VisualItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;

public class SearchView {
    final static String LABEL = "label";
    final static String NODE_STATUS = "status";
    public final static String LAST_APPLIED_RESOLVER = "last-applied-resolver";
    public final static String SELECTED_FLAW = "selected-flaw";
    public final static String HEURISTIC_VALUES ="heuristic-values";
    public final static String ACTIONS_CANVAS = "actions-canvas";
    final static Color BACKGROUND = Color.WHITE;
    final static Color FOREGROUND = Color.BLACK;

    final static String helpMessage =
            "    Help\n"+
                    " - go over a node to see a description of the partial plan.\n" +
                    "   This Description contains the flaw that was selected to for solvind,\n" +
                    "   the resolver that was used to get from the parent partial plan to\n" +
                    "   this one. And some basic information on heuristics value.\n" +
                    " - click on a node to see the correspondig partial plan.\n" +
                    " - nodes en green are the one that have been expanded, those in red are\n" +
                    "   dead-ends, other are still in the queue.";



    final Tree t;
    final TreeView tview;
    final HashMap<Integer, Node> nodes = new HashMap<>();

    final Node root;
    final static String ACTIVITY_ON_FOCUS_CHANGE = "filter";

    final APlanner planner;

    boolean skipDrawing = false;
    boolean followCurrentNode = true;

    ChartWindow actionsChartWindow = null;

    public SearchView(APlanner planner) {
        this.planner = planner;
        Table nodeData = new Table();
        Table edgeData = new Table(0,1);
        nodeData.addColumn("flag", boolean.class);
        nodeData.addColumn(LABEL, String.class);
        nodeData.addColumn(NODE_STATUS, String.class);
        nodeData.addColumn(LAST_APPLIED_RESOLVER, String.class);
        nodeData.addColumn(SELECTED_FLAW, String.class);
        nodeData.addColumn(HEURISTIC_VALUES, String.class);
        nodeData.addColumn(ACTIONS_CANVAS, TimedCanvas.class);
        edgeData.addColumn(Tree.DEFAULT_SOURCE_KEY, int.class);
        edgeData.addColumn(Tree.DEFAULT_TARGET_KEY, int.class);
        edgeData.addColumn("LABEL", String.class);

        t = new Tree(nodeData, edgeData);
        root = t.addRoot();
        root.setString(LABEL, "Init");
        root.setString(NODE_STATUS, "init");
        root.setString(LAST_APPLIED_RESOLVER, "");
        root.setString(SELECTED_FLAW, "");
        root.setString(HEURISTIC_VALUES, "");
        root.set(ACTIONS_CANVAS, null);

        tview = new TreeView(t, LABEL);


        final JTextArea title = new JTextArea(helpMessage, 10, 20);

        tview.addControlListener(new ControlAdapter() {
            public void itemEntered(VisualItem item, MouseEvent e) {
                StringBuilder sb = new StringBuilder();
                if(item.canGetString(NODE_STATUS)) {
                    sb.append("Status: "); sb.append(item.getString(NODE_STATUS));; sb.append("\n");
                    sb.append(item.getString(HEURISTIC_VALUES)); sb.append("\n");
                    sb.append("Last resolver: "); sb.append(item.getString(LAST_APPLIED_RESOLVER)); sb.append("\n");
                    sb.append("Selected flaw: "); sb.append(item.getString(SELECTED_FLAW));
                }
                title.setText(sb.toString());
            }

            @Override
            public void itemClicked(VisualItem item, MouseEvent e) {
                if(item.canGet(ACTIONS_CANVAS, TimedCanvas.class) && item.get(ACTIONS_CANVAS) != null) {
                    if (actionsChartWindow == null)
                        actionsChartWindow = new ChartWindow("Action chart");
                    actionsChartWindow.draw((TimedCanvas) item.get(ACTIONS_CANVAS));
                }
            }
//            public void itemExited(VisualItem item, MouseEvent e) {
//                title.setText(null);
//            }
        });

        Box box = new Box(BoxLayout.X_AXIS);
        box.add(Box.createHorizontalStrut(10));
        box.add(title);
        box.add(Box.createHorizontalGlue());
//        box.add(search);
        box.add(Box.createHorizontalStrut(3));
        box.setBackground(BACKGROUND);

        JPanel optionsPanel = new JPanel();
        JCheckBox fastCB = new JCheckBox("Fast", false);
        fastCB.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if(itemEvent.getStateChange() == ItemEvent.DESELECTED)
                    skipDrawing = false;
                else if(itemEvent.getStateChange() == ItemEvent.SELECTED)
                    skipDrawing = true;
            }
        });
        fastCB.setToolTipText("Only draw once every fifty state expansion.");

        final JCheckBox followCB = new JCheckBox("Follow", true);
        followCB.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if(itemEvent.getStateChange() == ItemEvent.DESELECTED)
                    followCurrentNode = false;
                else if(itemEvent.getStateChange() == ItemEvent.SELECTED)
                    followCurrentNode = true;
            }
        });
        followCB.setToolTipText("Puts the currently expanded node on at the center of the screen.");
        optionsPanel.setLayout(new GridLayout(1,0));
        optionsPanel.add(fastCB);
        optionsPanel.add(followCB);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND);
        panel.setForeground(FOREGROUND);
        panel.add(optionsPanel, BorderLayout.NORTH);
        panel.add(tview, BorderLayout.CENTER);
        panel.add(box, BorderLayout.SOUTH);

        JFrame frame = new JFrame("FAPE Search view");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(panel);
        frame.pack();
        frame.setVisible(true);
    }

    public void setDeadEnd(State st) {
        assert nodes.containsKey(st.mID);
        nodes.get(st.mID).setString(NODE_STATUS, "deadend");
    }

    public void setSolution(State st) {
        assert nodes.containsKey(st.mID);
        nodes.get(st.mID).setString(NODE_STATUS, "solution");
        tview.updated();
    }

    public void setProperty(State st, String propertyID, String value) {
        assert nodes.containsKey(st.mID);
        Node item = nodes.get(st.mID);
        assert item.canGetString(propertyID);
        item.setString(propertyID, value);
    }

    public void setCurrentFocus(State st) {
        assert nodes.containsKey(st.mID);
        nodes.get(st.mID).setString(NODE_STATUS, "expanded");
        if(followCurrentNode) {
            VisualItem v = tview.getVisualItem(nodes.get(st.mID));
            TupleSet ts = tview.vis().getFocusGroup(Visualization.FOCUS_ITEMS);
            ts.setTuple(v);
        } else {
            if(tview.vis().getFocusGroup(Visualization.FOCUS_ITEMS).getTupleCount() != 0)
                tview.vis().getFocusGroup(Visualization.FOCUS_ITEMS).clear();
        }
        if(!skipDrawing) {
            tview.updated();
        } else {
            if (cnt == 0)
                tview.updated();
            cnt = (cnt + 1) % 50;
        }
    }

    int cnt = 0;
    private Node addNode(int id, int parentID) {
        Node n;
        if(parentID < 0)
            n = t.addChild(root);
        else {
            assert nodes.containsKey(parentID);
            assert nodes.get(parentID) != null;
            n = t.addChild(nodes.get(parentID));
        }
        nodes.put(id, n);

        if(!skipDrawing) {
            tview.updated();
        } else {
            if (cnt == 0)
                tview.updated();
            cnt = (cnt + 1) % 50;
        }
        return n;
    }

    public void addNode(State st, State parent) {
        String label = Integer.valueOf(st.mID).toString();
        Node n;
        if(parent != null)
            n = addNode(st.mID, parent.mID);
        else
            n = addNode(st.mID, -1);

        StringBuilder h = new StringBuilder();
        h.append("num-actions: "); h.append(st.getNumActions());
        h.append(" num-open-goals: "); h.append(st.tdb.getConsumers().size());
        h.append(" num-threats: "); h.append(st.getAllThreats().size());
        h.append(" num-undecomposed: "); h.append(st.getOpenLeaves().size());
        h.append(" num-opentasks: "); h.append(st.getOpenTaskConditions().size());
        h.append(" num-unmotivated: "); h.append(st.getUnmotivatedActions().size());
        h.append(" num-unbound: "); h.append(st.getUnboundVariables().size()); h.append("\n");
        h.append(planner.stateComparator().reportOnState(st));
        if(planner.definesHeuristicsValues()) {
            h.append(String.format(" g: %s, h: %s, f: %s", planner.g(st), planner.h(st), planner.f(st)));
        }

        n.setString(LABEL, label);
        n.setString(NODE_STATUS, "inqueue");
        n.setString(HEURISTIC_VALUES, h.toString());
        n.setString(SELECTED_FLAW, "???");
        n.setString(LAST_APPLIED_RESOLVER, "???");
        n.set(ACTIONS_CANVAS, st.getCanvasOfActions());
    }
}
