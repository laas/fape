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

import java.lang.Exception;
import actionlib_msgs.GoalStatus;

/*
 * Class to manage the server state machine transitions.
 * @author Ernesto Corbellini ecorbellini@ekumenlabs.com
 */
public class ServerStateMachine {
  public static class Events {
    public final static int CANCEL_REQUEST = 1;
    public final static int CANCEL = 2;
    public final static int REJECT = 3;
    public final static int ACCEPT = 4;
    public final static int SUCCEED = 5;
    public final static int ABORT = 6;
  }

  private int state;

  ServerStateMachine() {
    // Initial state
    state = GoalStatus.PENDING;
  }

  public synchronized int getState() {
    return state;
  }

  public synchronized void setState(int s) {
    state = s;
  }

  public synchronized int transition(int event) throws Exception {
    int nextState = state;
    switch (state) {
      case GoalStatus.PENDING:
        switch (event) {
          case Events.REJECT:
            nextState = GoalStatus.REJECTED;
            break;
          case Events.CANCEL_REQUEST:
            nextState = GoalStatus.RECALLING;
            break;
          case Events.ACCEPT:
            nextState = GoalStatus.ACTIVE;
            break;
          default:
            throw new Exception("Actionlib server exception: Invalid transition event!");
        }
        break;
      case GoalStatus.RECALLING:
        switch (event) {
          case Events.REJECT:
            nextState = GoalStatus.REJECTED;
            break;
          case Events.CANCEL:
            nextState = GoalStatus.RECALLED;
            break;
          case Events.ACCEPT:
            nextState = GoalStatus.PREEMPTING;
            break;
          default:
            throw new Exception("Actionlib server exception: Invalid transition event!");
        }
        break;
      case GoalStatus.ACTIVE:
        switch (event) {
          case Events.SUCCEED:
            nextState = GoalStatus.SUCCEEDED;
            break;
          case Events.CANCEL_REQUEST:
            nextState = GoalStatus.PREEMPTING;
            break;
          case Events.ABORT:
            nextState = GoalStatus.ABORTED;
            break;
          default:
            throw new Exception("Actionlib server exception: Invalid transition event!");
        }
        break;
      case GoalStatus.PREEMPTING:
        switch (event) {
          case Events.SUCCEED:
            nextState = GoalStatus.SUCCEEDED;
            break;
          case Events.CANCEL:
            nextState = GoalStatus.PREEMPTED;
            break;
          case Events.ABORT:
            nextState = GoalStatus.ABORTED;
            break;
          default:
            throw new Exception("Actionlib server exception: Invalid transition event!");
        }
        break;
      case GoalStatus.REJECTED:
        break;
      case GoalStatus.RECALLED:
        break;
      case GoalStatus.PREEMPTED:
        break;
      case GoalStatus.SUCCEEDED:
        break;
      case GoalStatus.ABORTED:
        break;
      default:
        throw new Exception("Actionlib server exception: Invalid state!");
    }
    // transition to the next state
    state = nextState;
    return nextState;
  }
}
