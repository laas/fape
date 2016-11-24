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

import java.util.List;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.internal.message.Message;
import org.ros.message.Duration;
import actionlib_tutorials.FibonacciActionGoal;
import actionlib_tutorials.FibonacciActionFeedback;
import actionlib_tutorials.FibonacciActionResult;
import actionlib_tutorials.FibonacciGoal;
import actionlib_tutorials.FibonacciFeedback;
import actionlib_tutorials.FibonacciResult;
import actionlib_msgs.GoalStatusArray;
import actionlib_msgs.GoalID;
import actionlib_msgs.GoalStatus;
import org.apache.commons.logging.Log;


/**
 * Class to test the actionlib client.
 * @author Ernesto Corbellini ecorbellini@ekumenlabs.com
 */
public class TestClient extends AbstractNodeMain implements ActionClientListener<FibonacciActionFeedback, FibonacciActionResult> {
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
    ac = new ActionClient<FibonacciActionGoal, FibonacciActionFeedback, FibonacciActionResult>(node, "/fibonacci", FibonacciActionGoal._TYPE, FibonacciActionFeedback._TYPE, FibonacciActionResult._TYPE);
    FibonacciActionGoal goalMessage;
    GoalID gid;
    Duration serverTimeout = new Duration(20);
    boolean serverStarted;

    log = node.getLog();
    // Attach listener for the callbacks
    ac.attachListener(this);
    System.out.println("\nWaiting for action server to start...");
    serverStarted = ac.waitForActionServerToStart(new Duration(20));
    if (serverStarted) {
      System.out.println("Action server started.\n");
    }
    else {
      System.out.println("No actionlib server found after waiting for " + serverTimeout.totalNsecs()/1e9 + " seconds!");
      System.exit(1);
    }

    // Create Fibonacci goal message
    goalMessage = (FibonacciActionGoal)ac.newGoalMessage();
    FibonacciGoal fibonacciGoal = goalMessage.getGoal();
    // set Fibonacci parameter
    fibonacciGoal.setOrder(3);
    System.out.println("Sending goal...");
    ac.sendGoal(goalMessage);
    gid = ac.getGoalId(goalMessage);
    System.out.println("Sent goal with ID: " + gid.getId());
    System.out.println("Waiting for goal to complete...");
    while (ac.getGoalState() != ClientStateMachine.ClientStates.DONE) {
      sleep(1);
    }
    System.out.println("Goal completed!\n");

    System.out.println("Sending a new goal...");
    ac.sendGoal(goalMessage);
    gid = ac.getGoalId(goalMessage);
    System.out.println("Sent goal with ID: " + gid.getId());
    System.out.println("Cancelling this goal...");
    ac.sendCancel(gid);
    while (ac.getGoalState() != ClientStateMachine.ClientStates.DONE) {
      sleep(1);
    }
    System.out.println("Goal cancelled succesfully.\n");
    System.out.println("Bye!");
    System.exit(0);
  }

  @Override
  public void resultReceived(FibonacciActionResult message) {
    FibonacciResult result = message.getResult();
    int[] sequence = result.getSequence();
    int i;

    resultReceived = true;
    System.out.print("Got Fibonacci result sequence: ");
    for (i=0; i<sequence.length; i++)
      System.out.print(Integer.toString(sequence[i]) + " ");
    System.out.println("");
  }

  @Override
  public void feedbackReceived(FibonacciActionFeedback message) {
    FibonacciFeedback result = message.getFeedback();
    int[] sequence = result.getSequence();
    int i;

    System.out.print("Feedback from Fibonacci server: ");
    for (i=0; i<sequence.length; i++)
      System.out.print(Integer.toString(sequence[i]) + " ");
    System.out.print("\n");
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
