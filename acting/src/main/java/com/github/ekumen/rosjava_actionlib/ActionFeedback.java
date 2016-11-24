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

import java.lang.reflect.Method;
import org.ros.internal.message.Message;
import org.ros.message.Time;
import std_msgs.Header;
import actionlib_msgs.GoalID;
import actionlib_msgs.GoalStatus;

/**
 * Class to encapsulate the action feedback object.
 * @author Ernesto Corbellini ecorbellini@ekumenlabs.com
 */
public class ActionFeedback<T_ACTION_FEEDBACK extends Message> {
  private T_ACTION_FEEDBACK actionFeedbackMessage = null;

  public ActionFeedback(T_ACTION_FEEDBACK fmsg) {
    actionFeedbackMessage = fmsg;
  }

  public Header getHeaderMessage() {
    Header h = null;
    if (actionFeedbackMessage != null) {
      try {
        Method m = actionFeedbackMessage.getClass().getMethod("getHeader");
        m.setAccessible(true); // workaround for known bug http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6924232
        h = (Header)m.invoke(actionFeedbackMessage);
      }
      catch (Exception e) {
        e.printStackTrace(System.out);
      }
    }
    return h;
  }

  public GoalStatus getGoalStatusMessage() {
    GoalStatus gs = null;
    if (actionFeedbackMessage != null) {
      try {
        Method m = actionFeedbackMessage.getClass().getMethod("getStatus");
        m.setAccessible(true); // workaround for known bug http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6924232
        gs = (GoalStatus)m.invoke(actionFeedbackMessage);
      }
      catch (Exception e) {
        e.printStackTrace(System.out);
      }
    }
    return gs;
  }

  public Message getFeedbackMessage() {
    Message x = null;
    if (actionFeedbackMessage != null) {
      try {
        Method m = actionFeedbackMessage.getClass().getMethod("getFeedback");
        m.setAccessible(true); // workaround for known bug http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6924232
        x = (Message)m.invoke(actionFeedbackMessage);
      }
      catch (Exception e) {
        e.printStackTrace(System.out);
      }
    }
    return x;
  }
}
