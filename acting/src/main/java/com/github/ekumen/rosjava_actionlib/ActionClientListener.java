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

import org.ros.internal.message.Message;
import actionlib_msgs.GoalStatusArray;


/**
 * Listener interface to receive the incoming messages from the ActionLib server.
 * A client should implement this interface if it wants to receive the callbacks
 * with information from the server.
 * @author Ernesto Corbellini ecorbellini@ekumenlabs.com
 */
public interface ActionClientListener<T_ACTION_FEEDBACK extends Message,
  T_ACTION_RESULT extends Message> {
  /**
   * Called when a result message is received from the server.
   * @param result Result message from the server. The type of this message
   * depends on the application.
   */
  void resultReceived(T_ACTION_RESULT result);

  /**
   * Called when a feedback message is received from the server.
   * @param feedback The feedback message received from the server. The type of
   * this message depends on the application.
   */
  void feedbackReceived(T_ACTION_FEEDBACK feedback);

  /**
   * Called when a status message is received from the server.
   * @param status The status message received from the server.
   * @see actionlib_msgs.GoalStatusArray
   */
  void statusReceived(GoalStatusArray status);
}
