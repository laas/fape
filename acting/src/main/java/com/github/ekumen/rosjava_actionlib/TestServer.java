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
import actionlib_tutorials.FibonacciActionGoal;
import actionlib_tutorials.FibonacciActionFeedback;
import actionlib_tutorials.FibonacciActionResult;
import actionlib_tutorials.FibonacciGoal;
import actionlib_tutorials.FibonacciFeedback;
import actionlib_tutorials.FibonacciResult;
import actionlib_msgs.GoalStatusArray;
import actionlib_msgs.GoalID;
import actionlib_msgs.GoalStatus;

/**
 * Class to test the actionlib server.
 * @author Ernesto Corbellini ecorbellini@ekumenlabs.com
 */
public class TestServer extends AbstractNodeMain implements ActionServerListener<FibonacciActionGoal> {
  private ActionServer<FibonacciActionGoal, FibonacciActionFeedback, FibonacciActionResult> as = null;
  private volatile FibonacciActionGoal currentGoal = null;

  @Override
  public GraphName getDefaultNodeName() {
    return GraphName.of("fibonacci_test_server");
  }

  @Override
  public void onStart(ConnectedNode node) {
    FibonacciActionResult result;
    String id;

    as = new ActionServer<FibonacciActionGoal, FibonacciActionFeedback,
      FibonacciActionResult>(node, "/fibonacci", FibonacciActionGoal._TYPE,
      FibonacciActionFeedback._TYPE, FibonacciActionResult._TYPE);

    as.attachListener(this);

    while(true) {
      if (currentGoal != null) {
        result = as.newResultMessage();
        result.getResult().setSequence(fibonacciSequence(currentGoal.getGoal().getOrder()));
        id = currentGoal.getGoalId().getId();
        as.setSucceed(id);
        as.setGoalStatus(result.getStatus(), id);
        System.out.println("Sending result...");
        as.sendResult(result);
        currentGoal = null;
      }
    }
  }

  @Override
  public void goalReceived(FibonacciActionGoal goal) {
    System.out.println("Goal received.");
  }

  @Override
  public void cancelReceived(GoalID id) {
    System.out.println("Cancel received.");
  }

  @Override
  public boolean acceptGoal(FibonacciActionGoal goal) {
    // If we don't have a goal, accept it. Otherwise, reject it.
    if (currentGoal == null) {
      currentGoal = goal;
      System.out.println("Goal accepted.");
      return true;
    } else {
      System.out.println("We already have a goal! New goal reject.");
      return false;
    }
  }

  private int[] fibonacciSequence(int order) {
    int i;
    int[] fib = new int[order + 2];

    fib[0] = 0;
    fib[1] = 1;

    for (i = 2; i < (order + 2); i++) {
      fib[i] = fib[i - 1] + fib[i - 2];
    }
    return fib;
  }

  /*
   * Sleep for an amount on miliseconds.
   * @param msec Number or miliseconds to sleep.
   */
  private void sleep(long msec) {
    try {
      Thread.sleep(msec);
    }
    catch (InterruptedException ex) {
    }
  }

}
