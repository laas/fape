/**
 * Copyright 2015 Ekumen www.ekumenlabs.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.ekumen.rosjava_actionlib;

import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.SubscriberListener;
import org.ros.internal.node.topic.PublisherIdentifier;
import org.ros.node.topic.DefaultSubscriberListener;
import org.ros.message.MessageListener;
import org.ros.message.Duration;
import org.ros.message.Time;
import org.ros.internal.message.Message;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.lang.reflect.Method;
import actionlib_msgs.GoalStatusArray;
import actionlib_msgs.GoalStatus;
import actionlib_msgs.GoalID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Client implementation for actionlib.
 * This class encapsulates the communication with an actionlib server.
 * @author Ernesto Corbellini ecorbellini@ekumenlabs.com
 */
public class ActionClient<T_ACTION_GOAL extends Message,
  T_ACTION_FEEDBACK extends Message,
  T_ACTION_RESULT extends Message> extends DefaultSubscriberListener {

  T_ACTION_GOAL actionGoal;
  ClientGoalManager<T_ACTION_GOAL> goalManager;
  private String actionGoalType;
  private String actionResultType;
  private String actionFeedbackType;
  private Publisher<T_ACTION_GOAL> goalPublisher = null;
  public Publisher<GoalID> cancelPublisher = null;
  private Subscriber<T_ACTION_RESULT> serverResult = null;
  private Subscriber<T_ACTION_FEEDBACK> serverFeedback = null;
  private Subscriber<GoalStatusArray> serverStatus = null;
  private ConnectedNode node = null;
  private String actionName;
  private ActionClientListener callbackTarget = null;
  GoalIDGenerator goalIdGenerator = null;
  private volatile boolean statusReceivedFlag = false;
  private volatile boolean feedbackPublisherFlag = false;
  private volatile boolean resultPublisherFlag = false;
  private Log log = LogFactory.getLog(ActionClient.class);

  /**
   * Constructor for an ActionClient object.
   * @param node The node object that is connected to the ROS master.
   * @param actionName A string representing the name of this action. This name
   * is used to publish the actinlib topics and should be agreed between server
   * and the client.
   * @param actionGoalType A string with the type information for the action
   * goal message.
   * @param actionFeedbackType A string with the type information for the
   * feedback message.
   * @param actionResultType A string with the type information for the result
   * message.
   */
  public ActionClient (ConnectedNode node, String actionName, String actionGoalType,
    String actionFeedbackType, String actionResultType) {
    this.node = node;
    this.actionName = actionName;
    this.actionGoalType = actionGoalType;
    this.actionFeedbackType = actionFeedbackType;
    this.actionResultType = actionResultType;
    goalIdGenerator = new GoalIDGenerator(node);
    connect(node);
    goalManager = new ClientGoalManager(new ActionGoal<T_ACTION_GOAL>());
  }

  public void attachListener(ActionClientListener target) {
    callbackTarget = target;
  }

  /**
    * Publish an action goal to the server. The type of the action goal message
    * is dependent on the application.
    * @param goal The action goal message.
    * @param id A string containing the ID for the goal. The ID should represent
    * this goal in a unique fashion in the server and the client.
    */
  public void sendGoal(T_ACTION_GOAL agMessage, String id) {
    GoalID gid = getGoalId(agMessage);
    if (id == "") {
      goalIdGenerator.generateID(gid);
    } else {
      gid.setId(id);
    }
    goalManager.setGoal(agMessage);
    goalPublisher.publish(agMessage);
  }

  /**
    * Publish an action goal to the server. The type of the action goal message
    * is dependent on the application. A goal ID will be automatically generated.
    * @param goal The action goal message.
    */
  public void sendGoal(T_ACTION_GOAL agMessage) {
    sendGoal(agMessage, "");
  }

  /**
   * Convenience method for retrieving the goal ID of a given action goal message.
   * @param goal The action goal message from where to obtain the goal ID.
   * @return Goal ID object containing the ID of the action message.
   * @see actionlib_msgs.GoalID
   */
  public GoalID getGoalId(T_ACTION_GOAL goal) {
    GoalID gid = null;
    try {
      Method m = goal.getClass().getMethod("getGoalId");
      m.setAccessible(true); // workaround for known bug http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6924232
      gid = (GoalID)m.invoke(goal);
    }
    catch (Exception e) {
      e.printStackTrace(System.out);
    }
    return gid;
  }

  /**
   * Convenience method for setting the goal ID of an action goal message.
   * @param goal The action goal message to set the goal ID for.
   * @param gid The goal ID object.
   * @see actionlib_msgs.GoalID
   */
  public void setGoalId(T_ACTION_GOAL goal, GoalID gid) {
    try {
      Method m = goal.getClass().getMethod("setGoalId", GoalID.class);
      m.setAccessible(true); // workaround for known bug http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6924232
      m.invoke(goal, gid);
    }
    catch (Exception e) {
      e.printStackTrace(System.out);
    }
  }

  /**
   * Publish a cancel message. This instructs the action server to cancel the
   * specified goal.
   * @param id The GoalID message identifying the goal to cancel.
   * @see actionlib_msgs.GoalID
   */
  public void sendCancel(GoalID id) {
    goalManager.cancelGoal();
    cancelPublisher.publish(id);
  }

  /**
   * Start publishing on the client topics: /goal and /cancel.
   * @param node The node object that is connected to the ROS master.
   */
  private void publishClient(ConnectedNode node) {
    goalPublisher = node.newPublisher(actionName + "/goal", actionGoalType);
    goalPublisher.setLatchMode(false);
    cancelPublisher = node.newPublisher(actionName + "/cancel", GoalID._TYPE);
  }

  /**
   * Stop publishing our client topics.
   */
  private void unpublishClient() {
    if (goalPublisher != null) {
      goalPublisher.shutdown(5, TimeUnit.SECONDS);
      goalPublisher = null;
    }
    if (cancelPublisher != null) {
      cancelPublisher.shutdown(5, TimeUnit.SECONDS);
      cancelPublisher = null;
    }
  }

  public T_ACTION_GOAL newGoalMessage() {
    return goalPublisher.newMessage();
  }

  /**
   * Subscribe to the server topics.
   * @param node The node object that is connected to the ROS master.
   */
  private void subscribeToServer(ConnectedNode node) {
    serverResult = node.newSubscriber(actionName + "/result", actionResultType);
    serverFeedback = node.newSubscriber(actionName + "/feedback", actionFeedbackType);
    serverStatus = node.newSubscriber(actionName + "/status", GoalStatusArray._TYPE);

    serverFeedback.addSubscriberListener(this);
    serverResult.addSubscriberListener(this);

    serverFeedback.addMessageListener(new MessageListener<T_ACTION_FEEDBACK>() {
      @Override
      public void onNewMessage(T_ACTION_FEEDBACK message) {
        gotFeedback(message);
      }
    });

    serverResult.addMessageListener(new MessageListener<T_ACTION_RESULT>() {
      @Override
      public void onNewMessage(T_ACTION_RESULT message) {
        gotResult(message);
      }
    });

    serverStatus.addMessageListener(new MessageListener<GoalStatusArray>() {
      @Override
      public void onNewMessage(GoalStatusArray message) {
        gotStatus(message);
      }
    });
  }

  /**
   * Unsubscribe from the server topics.
   */
  private void unsubscribeToServer() {
    if (serverFeedback != null) {
      serverFeedback.shutdown(5, TimeUnit.SECONDS);
      serverFeedback = null;
    }
    if (serverResult != null) {
      serverResult.shutdown(5, TimeUnit.SECONDS);
      serverResult = null;
    }
    if (serverStatus != null) {
      serverStatus.shutdown(5, TimeUnit.SECONDS);
      serverStatus = null;
    }
  }

  /**
   * Called whenever we get a message in the result topic.
   * @param message The result message received. The type of this message
   * depends on the application.
   */
  public void gotResult(T_ACTION_RESULT message) {
    ActionResult<T_ACTION_RESULT> ar = new ActionResult(message);
    if (ar.getGoalStatusMessage().getGoalId().getId().equals(goalManager.actionGoal.getGoalId())) {
      goalManager.updateStatus(ar.getGoalStatusMessage().getStatus());
    }
    goalManager.resultReceived();
    // Propagate the callback
    if (callbackTarget != null) {
      callbackTarget.resultReceived(message);
    }
  }

  /**
   * Called whenever we get a message in the feedback topic.
   * @param message The feedback message received. The type of this message
   * depends on the application.
   */
  public void gotFeedback(T_ACTION_FEEDBACK message) {
    ActionFeedback<T_ACTION_FEEDBACK> af = new ActionFeedback(message);
    if (af.getGoalStatusMessage().getGoalId().getId().equals(goalManager.actionGoal.getGoalId())) {
      goalManager.updateStatus(af.getGoalStatusMessage().getStatus());
    }
    // Propagate the callback
    if (callbackTarget != null) {
      callbackTarget.feedbackReceived(message);
    }
  }

  /**
   * Called whenever we get a message in the status topic.
   * @param message The GoalStatusArray message received.
   * @see actionlib_msgs.GoalStatusArray
   */
  public void gotStatus(GoalStatusArray message) {
    statusReceivedFlag = true;
    // Find the status for our current goal
    GoalStatus gstat = findStatus(message);
    if (gstat != null) {
      // update the goal status tracking
      goalManager.updateStatus(gstat.getStatus());
    } else {
//      log.info("Status update is not for our goal!");
    }
    // Propagate the callback
    if (callbackTarget != null) {
      callbackTarget.statusReceived(message);
    }
  }

  /**
   * Walk through the status array and find the status for the action goal that
   * we are interested in.
   * @param statusMessage The message with the goal status array
   * (actionlib_msgs.GoalStatusArray)
   * @return The goal status message for the goal we want or null if we didn't
   * find it.
   */
  public GoalStatus findStatus(GoalStatusArray statusMessage) {
    GoalStatus gstat = null;
    List<GoalStatus> statusList = statusMessage.getStatusList();
    for (GoalStatus s : statusList) {
//      log.info("Found >> " + s.getGoalId().getId() + " when searching for >> " + goalManager.actionGoal.getGoalId());
      if (s.getGoalId().getId().equals(goalManager.actionGoal.getGoalId())) {
        // this is the goal we are interested in
        gstat = s;
      }
    }
    return gstat;
  }

  /**
  * Publishes the client's topics and suscribes to the server's topics.
  * @param node The node object that is connected to the ROS master.
  */
  private void connect(ConnectedNode node) {
    publishClient(node);
    subscribeToServer(node);
  }

  /**
   * Wait for an actionlib server to connect.
   * @param timeout The maximum amount of time to wait for an action server. If
   * this value is less than or equal to zero, it will wait forever until a
   * server is detected.
   * @return True if the action server was detected before the timeout and
   * false otherwise.
   */
  public boolean waitForActionServerToStart(Duration timeout) {
    boolean res = false;
    boolean gotTime = true;
    Time finalTime = node.getCurrentTime().add(timeout);

    while (!res && gotTime) {
      res = goalPublisher.hasSubscribers() &&
        cancelPublisher.hasSubscribers();
//        && feedbackPublisherFlag && // assume everything goes right as this is very sensitive to timing
//        resultPublisherFlag &&
//        statusReceivedFlag;
      if (timeout.isPositive()) {
        gotTime = (node.getCurrentTime().compareTo(finalTime) < 0);
      }
    }
    return res;
  }

  /**
   * Wait indefinately until an actionlib server is connected.
   */
  public void waitForActionServerToStart() {
    waitForActionServerToStart(new Duration(0));
  }

  @Override
  public void onNewPublisher(Subscriber subscriber, PublisherIdentifier publisherIdentifier) {
  //public void onNewFeedbackPublisher(Subscriber<T_ACTION_FEEDBACK> subscriber, PublisherIdentifier publisherIdentifier) {
    if (subscriber.equals(serverFeedback)) {
      feedbackPublisherFlag = true;
//      log.info("Found server publishing on the " + actionName + "/feedback topic.");
    } else {
      if (subscriber.equals(serverResult)) {
        resultPublisherFlag = true;
//        log.info("Found server publishing on the " + actionName + "/result topic.");
      }
    }
  }

  /**
   * Get the current state of the action goal as being tracked by the client.
   * @return The state of the goal.
   * @see ClientStateMachine.ClientStates
   */
  public int getGoalState() {
    return goalManager.getGoalState();
  }

  /**
   * Finish the action client. Unregister publishers and listeners.
   */
  public void finish() {
    callbackTarget = null;
    unpublishClient();
    unsubscribeToServer();
  }

  protected void finalize() {
    finish();
  }
}
