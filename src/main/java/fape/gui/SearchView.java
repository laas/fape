package fape.gui;

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import prefuse.Visualization;
import prefuse.controls.ControlAdapter;
import prefuse.data.Node;
import prefuse.data.Table;
import prefuse.data.Tree;
import prefuse.data.tuple.TupleSet;
import prefuse.util.FontLib;
import prefuse.util.ui.JFastLabel;
import prefuse.visual.VisualItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.HashMap;

public class SearchView {
    final static String desc = "description";
    final static String LABEL = "label";
    final static Color BACKGROUND = Color.WHITE;
    final static Color FOREGROUND = Color.BLACK;

    final Tree t;
    final TreeView tview;
    final HashMap<Integer, Node> nodes = new HashMap<>();

    final Node root;
    final static String ACTIVITY_ON_FOCUS_CHANGE = "filter";

    public SearchView(APlanner planner) {

        Table nodeData = new Table();
        Table edgeData = new Table(0,1);
        nodeData.addColumn("flag", boolean.class);
        nodeData.addColumn(LABEL, String.class);
        nodeData.addColumn(desc, String.class);
        edgeData.addColumn(Tree.DEFAULT_SOURCE_KEY, int.class);
        edgeData.addColumn(Tree.DEFAULT_TARGET_KEY, int.class);
        edgeData.addColumn("LABEL", String.class);

        t = new Tree(nodeData, edgeData);
        root = t.addRoot();
        root.setString(LABEL, "Init");
        root.setString("description", "Empty partial plan");
//        Node n2 = t.addChild(n1);
//        n2.setString(LABEL, "[2]");
//        Node n3 = t.addChild(n1);
//        n3.setString(LABEL, "[3]");

        tview = new TreeView(t, LABEL, desc);


        final JFastLabel title = new JFastLabel("                 ");
        title.setPreferredSize(new Dimension(350, 20));
        title.setVerticalAlignment(SwingConstants.BOTTOM);
        title.setBorder(BorderFactory.createEmptyBorder(3,0,0,0));
        title.setFont(FontLib.getFont("Tahoma", Font.PLAIN, 16));
        title.setBackground(BACKGROUND);
        title.setForeground(FOREGROUND);

        tview.addControlListener(new ControlAdapter() {
            public void itemEntered(VisualItem item, MouseEvent e) {
                if ( item.canGetString(desc) )
                    title.setText(item.getString(desc));
            }
            public void itemExited(VisualItem item, MouseEvent e) {
                title.setText(null);
            }
        });

        Box box = new Box(BoxLayout.X_AXIS);
        box.add(Box.createHorizontalStrut(10));
        box.add(title);
        box.add(Box.createHorizontalGlue());
//        box.add(search);
        box.add(Box.createHorizontalStrut(3));
        box.setBackground(BACKGROUND);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND);
        panel.setForeground(FOREGROUND);
        panel.add(tview, BorderLayout.CENTER);
        panel.add(box, BorderLayout.SOUTH);

        JFrame frame = new JFrame("p r e f u s e  |  t r e e v i e w");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(panel);
        frame.pack();
        frame.setVisible(true);
    }

    public void setCurrentFocus(State st) {
        assert nodes.containsKey(st.mID);
        TupleSet ts = tview.vis().getFocusGroup(Visualization.FOCUS_ITEMS);
        tview.vis().getFocusGroup(Visualization.FOCUS_ITEMS).setTuple(nodes.get(st.mID));
        tview.run("filter");
        tview.updated();
        tview.vis()
    }

    public void addNode(int id, int parentID, String label, String description) {
        Node n;
        if(parentID < 0)
            n = t.addChild(root);
        else {
            assert nodes.containsKey(parentID);
            assert nodes.get(parentID) != null;
            n = t.addChild(nodes.get(parentID));
        }
        nodes.put(id, n);
        n.setString(LABEL, label);
        n.setString(desc, description);

//        tview.run("treeLayout");
//        tview.run("subLayout");
//        tview.run("paint");
//        tview.run("fullPaint");
        tview.updated();
    }

    public void addNode(State newNode, State parent) {
        String label = Integer.valueOf(newNode.mID).toString();
        String description = "bsdqsdqsd qsd "+newNode.mID;
        if(parent != null)
            addNode(newNode.mID, parent.mID, label, description);
        else
            addNode(newNode.mID, -1, label, description);
    }

    public static void main(String[] args) {
        SearchView sv = new SearchView(null);
        sv.addNode(0,-1, "[1]", "node 1");
        sv.addNode(1, 0, "[2]", "Second node");
        sv.addNode(2, 0, "[3]", "Second node");
        sv.addNode(3, 0, "[4]", "Second node");
    }

}
