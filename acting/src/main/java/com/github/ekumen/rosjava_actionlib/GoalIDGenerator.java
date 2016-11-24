/*
 * Copyright (C) 2015 Ernesto Corbellini, ecorbellini@ekumenlabs.com
 * Copyright (C) 2011 Alexander Perzylo, Technische Universität München
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.github.ekumen.rosjava_actionlib;

import org.ros.message.Time;
import actionlib_msgs.GoalID;
import org.ros.node.ConnectedNode;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The GoalIDGenerator may be used to create unique GoalIDs.
 *
 * <p>
 * The node's nodeName will be used and the time on the node.
 *
 * @author Alexander C. Perzylo, perzylo@cs.tum.edu
 */
public class GoalIDGenerator {
  /**
   * A global ID which provide a count for each goal id.
   */
  private static AtomicLong goalCount = new AtomicLong(0);

  /**
   * Unique nodeName to prepend to the goal id. This will generally be a fully
   * qualified node nodeName.
   */
  private ConnectedNode node;

  /**
   * Constructor to create a GoalIDGenerator using a unique nodeName to prepend to
   * the goal id. This will generally be a fully qualified node nodeName.
   *
   * @param node
   *          The node used to generate IDs. The node's full nodeName should be
   *          unique in the system.
   */
  public GoalIDGenerator(ConnectedNode node) {
    this.node = node;
  }

  /**
   * Creates a GoalID object with an unique id and a timestamp of the current
   * time.
   *
   * @return GoalID object
   */
  public String generateID(GoalID goalId) {
    String id;
    Time t = node.getCurrentTime();
    //NodeConfiguration nc = NodeConfiguration.newPrivate();
    //MessageFactory mf = nc.getTopicMessageFactory();
    //GoalID id = mf.newFromType(GoalID._TYPE);

    //StringBuilder sb = new StringBuilder(node.getName().toString());
    //sb.append("-").append(goalCount.incrementAndGet()).append("-").append(t.secs).append(".").append(t.nsecs);
    id = node.getName().toString() + "-" + goalCount.incrementAndGet()
      + "-" + t.secs + "." + t.nsecs;

    goalId.setId(id);
    goalId.setStamp(t);

    return id;
  }
}
