package fr.laas.fape.ros.action;

import actionlib_msgs.GoalID;
import actionlib_msgs.GoalStatusArray;
import com.github.ekumen.rosjava_actionlib.ActionClient;
import com.github.ekumen.rosjava_actionlib.ActionClientListener;
import com.github.ekumen.rosjava_actionlib.ActionResult;
import fr.laas.fape.ros.ROSNode;
import fr.laas.fape.ros.ROSUtils;
import org.ros.message.Duration;
import org.ros.node.ConnectedNode;

import org.ros.internal.message.Message;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by abitmonn on 11/18/16.
 */
public class SimpleActionServer<T_ActionGoal extends Message, T_ActionFeedback extends Message, T_ActionResult extends Message>
        implements ActionClientListener<T_ActionFeedback, T_ActionResult> {
    private ConnectedNode node;
    private ActionClient<T_ActionGoal, T_ActionFeedback, T_ActionResult> client = null;

    /** Maps a goal ID to a callback */
    private Map<String,Consumer<T_ActionResult>> callbacks = new HashMap<>();

    public SimpleActionServer(String actionServer, String goalType, String feedbackType, String resultType) {

        node = ROSNode.getNode();
        client = new ActionClient<>(node, actionServer,goalType, feedbackType, resultType);
        client.attachListener(this);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}
        if(!client.waitForActionServerToStart(new Duration(5)))
            throw new RuntimeException("No actionlib server found after waiting for " + 5 + " seconds!");
    }

    private void setGoal(T_ActionGoal g, Object req) {
        Method m = null;
        try {
            for(Method x :g.getClass().getMethods()) {
                if(x.getName().equals("setGoal"))
                    m = x;
            }
            // m = g.getClass().getMethod("setGoal"); // strangely this does not work
            m.setAccessible(true);
            m.invoke(g, req);
        } catch (Exception e) {
            System.out.println("Goal message: "+ ROSUtils.format(g));
            System.out.println("Goal class: "+g.getClass().getName());
            throw new RuntimeException(e);
        }
    }

    public void cancelPreviousGoals() {
        client.cancelPublisher.publish(ROSUtils.emptyMessageFromType("actionlib_msgs/GoalID"));
    }

    public T_ActionGoal newGoalMessage() { return client.newGoalMessage(); }

    public T_ActionResult sendRequest(T_ActionGoal goalMessage) {
        // count down that will be decremented when an answer is received
        final CountDownLatch countDown = new CountDownLatch(1);
        // to store a pointer to the received result
        final Object[] result = new Object[1];

        client.sendGoal(goalMessage);

        // register a callback to store the answer an notify of reception
        GoalID gid = client.getGoalId(goalMessage);
        callbacks.put(gid.getId(), msg -> { result[0] = msg; countDown.countDown(); });

        // wait for at most 5 seconds for the results, otherwise exit
        try {
            if(countDown.await(300L, TimeUnit.SECONDS)) {
//                System.out.println("Received answer:\n"+ROSUtils.format((Message) result[0]));
                return (T_ActionResult) result[0];
            } else {
                throw new RuntimeException("Did not receive any answer after waiting for 5 minutes");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public T_ActionResult sendGoal(Object req) {
        // create and send goal message
        T_ActionGoal goalMessage = client.newGoalMessage();
        setGoal(goalMessage, req);
        return sendRequest(goalMessage);
    }

    @Override
    public void resultReceived(T_ActionResult actionResult) {
        ActionResult<T_ActionResult> res = new ActionResult<>(actionResult);
        GoalID goalID = res.getGoalStatusMessage().getGoalId();
//        System.out.println("Result received "+goalID.getId()+":\n"+ROSUtils.format(actionResult));
        if(callbacks.containsKey(goalID.getId()))
            callbacks.get(goalID.getId()).accept(actionResult);
        else
            System.out.println("Received answer to unrecorded goal.");
    }

    @Override
    public void feedbackReceived(T_ActionFeedback actionFeedback) {
//        System.out.println("Received feedback: \n"+ROSUtils.format(actionFeedback));
    }

    @Override
    public void statusReceived(GoalStatusArray status) {
//        System.out.println("Received status: \n"+ROSUtils.format(status));
    }
}

