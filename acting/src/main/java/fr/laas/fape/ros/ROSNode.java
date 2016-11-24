package fr.laas.fape.ros;

import org.ros.RosRun;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;

import java.util.Collections;
import java.util.Random;


public class ROSNode extends AbstractNodeMain {

    private static ConnectedNode connectedNode;

    public static ConnectedNode getNode() {
        if(connectedNode == null) {
            try {
                RosRun.main(Collections.singletonList("fr.laas.fape.ros.ROSNode").toArray(new String[1]));
            } catch (Exception e) {
                throw new Move3dException("Could not start the main ROS node", e);
            }
            while(connectedNode == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        }
        assert connectedNode != null;
        return connectedNode;
    }

    /** Returns an anonymous node name */
    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("fape_"+new Random().nextInt(99999999));
    }

    @Override
    public void onStart(ConnectedNode node) {
        // wait until we have a clock signal, this is a  workaround
        // for bug: https://github.com/gabpeixoto/rosjava/issues/148
        while (true) {
            try {
                node.getCurrentTime();
                break; // no exception, so let's stop waiting
            } catch (NullPointerException e) {
                // wait a bit more for the clock to be setup
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e1) {
                }
            }
        }
        connectedNode = node;
    }
}
