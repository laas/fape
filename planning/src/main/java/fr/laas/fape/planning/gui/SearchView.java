package fr.laas.fape.planning.gui;

import fr.laas.fape.gui.ChartWindow;
import fr.laas.fape.gui.TimedCanvas;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.states.SearchNode;
import prefuse.Visualization;
import prefuse.controls.ControlAdapter;
import prefuse.data.Node;
import prefuse.data.Table;
import prefuse.data.Tree;
import prefuse.data.tuple.TupleSet;
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
    public final static String COMMENT = "comment";
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

    final Planner planner;

    boolean skipDrawing = false;
    boolean followCurrentNode = true;

    ChartWindow actionsChartWindow = null;

    public SearchView(Planner planner) {
        this.planner = planner;
        Table nodeData = new Table();
        Table edgeData = new Table(0,1);
        nodeData.addColumn("flag", boolean.class);
        nodeData.addColumn(LABEL, String.class);
        nodeData.addColumn(NODE_STATUS, String.class);
        nodeData.addColumn(LAST_APPLIED_RESOLVER, String.class);
        nodeData.addColumn(SELECTED_FLAW, String.class);
        nodeData.addColumn(HEURISTIC_VALUES, String.class);
        nodeData.addColumn(COMMENT, String.class);
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
                    sb.append("Status: "); sb.append(item.getString(NODE_STATUS));
                    sb.append("\n");
                    sb.append(item.getString(HEURISTIC_VALUES)); sb.append("\n");
                    sb.append("Last resolver: "); sb.append(item.getString(LAST_APPLIED_RESOLVER)); sb.append("\n");
                    sb.append("Selected flaw: "); sb.append(item.getString(SELECTED_FLAW)); sb.append("\n");
                    if(!item.getString(COMMENT).isEmpty()) {
                        sb.append("Comment: "); sb.append(item.getString(COMMENT));
                    }
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

    public void setDeadEnd(SearchNode st) {
        assert nodes.containsKey(st.getID());
        nodes.get(st.getID()).setString(NODE_STATUS, "deadend");
    }

    public void setSolution(SearchNode st) {
        assert nodes.containsKey(st.getID());
        nodes.get(st.getID()).setString(NODE_STATUS, "solution");
        tview.updated();
    }

    public void setProperty(SearchNode st, String propertyID, String value) {
        assert nodes.containsKey(st.getID());
        Node item = nodes.get(st.getID());
        assert item.canGetString(propertyID);
        item.setString(propertyID, value);
    }

    public void setCurrentFocus(SearchNode st) {
        assert nodes.containsKey(st.getID());
        nodes.get(st.getID()).setString(NODE_STATUS, "expanded");
        if(followCurrentNode) {
            VisualItem v = tview.getVisualItem(nodes.get(st.getID()));
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

    public void addNode(SearchNode st) {
        String label = Integer.valueOf(st.getID()).toString();
        Node n;
        if(st.getParent() != null)
            n = addNode(st.getID(), st.getParent().getID());
        else
            n = addNode(st.getID(), -1);

        StringBuilder h = new StringBuilder();
        h.append("num-actions: "); h.append(st.getState().getNumActions());
        h.append(" num-open-goals: "); h.append(st.getState().tdb.getConsumers().size());
        h.append(" num-threats: "); h.append(st.getState().getAllThreats().size());
        h.append(" num-opentasks: "); h.append(st.getState().getOpenTasks().size());
        h.append(" num-unmotivated: "); h.append(st.getState().getUnmotivatedActions().size());
        h.append(" num-unbound: "); h.append(st.getState().getUnboundVariables().size());
        h.append("\n");
        try {
            h.append(planner.heuristicComputer().reportOnState(st.getState()));
            h.append(String.format(" g: %s, h: %s", planner.heuristicComputer().g(st), planner.heuristicComputer().h(st)));
        } catch (Throwable e) {} // just to make sure the planner does not crash because of the view

        n.setString(LABEL, label);
        n.setString(NODE_STATUS, "inqueue");
        n.setString(HEURISTIC_VALUES, h.toString());
        n.setString(SELECTED_FLAW, "???");
        n.setString(LAST_APPLIED_RESOLVER, "???");
        n.setString(COMMENT, "");
        n.set(ACTIONS_CANVAS, st.getState().getCanvasOfActions());
    }
}
