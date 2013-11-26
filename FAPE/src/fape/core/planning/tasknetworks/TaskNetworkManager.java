/*
 * Author:  Filip Dvořák <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2013 Filip Dvořák <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using this program is prohibited
 * without previous written permission of the author. Publishing or providing
 * further the contents of this file is prohibited without previous written
 * permission of the author.
 */

package fape.core.planning.tasknetworks;

import fape.core.planning.model.Action;

/**
 *
 * @author FD
 */
public class TaskNetworkManager {
    TaskNetwork net = new TaskNetwork();

    public void AddSeed(Action act) {
        net.roots.add(act);
    }
}
