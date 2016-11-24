package fr.laas.fape.ros;

import actionlib_msgs.GoalID;
import actionlib_msgs.GoalStatus;
import actionlib_msgs.GoalStatusArray;
import com.github.ekumen.rosjava_actionlib.ActionClient;
import com.github.ekumen.rosjava_actionlib.ActionClientListener;
import com.github.ekumen.rosjava_actionlib.ClientStateMachine;
import gtp_ros_msg.*;
import org.apache.commons.logging.Log;
import org.ros.message.Duration;
import org.ros.message.MessageFactory;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;

import java.util.List;

public class RosTest extends AbstractNodeMain implements ActionClientListener<requestActionFeedback, requestActionResult> {
    static {
        // comment this line if you want logs activated
        System.setProperty("org.apache.commons.logging.Log",
                "org.apache.commons.logging.impl.NoOpLog");
    }
    private ActionClient ac = null;
    private volatile boolean resultReceived = false;
    private Log log;

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("fibonacci_test_client");
    }

    @Override
    public void onStart(ConnectedNode node) {
        // wait until we have a clock signal
        while (true) {
            try {
                node.getCurrentTime();
                break; // no exception, so let's stop waiting
            } catch (NullPointerException e) {
                System.out.println("Waiting for clock first message");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                }
            }
        }
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPrivate();
        MessageFactory messageFactory = nodeConfiguration.getTopicMessageFactory();
        requestGoal req = messageFactory.newFromType(requestGoal._TYPE);
        System.out.println("name: "+req.getReq().getActionName());
        ac = new ActionClient<requestActionGoal, requestActionFeedback, requestActionResult>(node, "/gtp_ros_server", requestActionGoal._TYPE, requestActionFeedback._TYPE, requestActionResult._TYPE);
        requestActionGoal goalMessage;
        GoalID gid;
        Duration serverTimeout = new Duration(20);
        boolean serverStarted;

        log = node.getLog();
        // Attach listener for the callbacks
        ac.attachListener(this);
        System.out.println("\nWaiting for action server to start...");
        serverStarted = ac.waitForActionServerToStart(new Duration(5));
        if (serverStarted) {
            System.out.println("Action server started.\n");
        }
        else {
            System.out.println("No actionlib server found after waiting for " + serverTimeout.totalNsecs()/1e9 + " seconds!");
            System.exit(1);
        }

        // Create Fibonacci goal message
        goalMessage = (requestActionGoal) ac.newGoalMessage();
        goalMessage.getGoal().getReq().setRequestType("update");
        System.out.println("name: "+goalMessage.getGoal().getReq().getRequestType());
        System.out.println(ClientStateMachine.ClientStates.translateState(ac.getGoalState()));
        ac.sendGoal(goalMessage);
        System.out.println(ClientStateMachine.ClientStates.translateState(ac.getGoalState()));
        while (ac.getGoalState() != ClientStateMachine.ClientStates.DONE) {
            System.out.println(ClientStateMachine.ClientStates.translateState(ac.getGoalState()));
            sleep(1);
        }
        System.out.println(ClientStateMachine.ClientStates.translateState(ac.getGoalState()));

        sleep(1000);
        System.exit(0);
    }

    @Override
    public void resultReceived(requestActionResult message) {
        System.out.println("Result: "+message);
        System.out.println(ROSUtils.format(message));
    }

    @Override
    public void feedbackReceived(requestActionFeedback message) {
        System.out.println("feedback: "+message);
    }

    @Override
    public void statusReceived(GoalStatusArray status) {
        List<GoalStatus> statusList = status.getStatusList();
        for(GoalStatus gs:statusList) {
            log.info("GoalID: " + gs.getGoalId().getId() + " -- GoalStatus: " + gs.getStatus() + " -- " + gs.getText());
        }
        log.info("Current state of our goal: " + ClientStateMachine.ClientStates.translateState(ac.getGoalState()));
    }

    void sleep(long msec) {
        try {
            Thread.sleep(msec);
        }
        catch (InterruptedException ex) {
        }
    }


}

