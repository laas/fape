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

package fape.planning.stn;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class STNManager {
    STN stn = new STN();
    List<TemporalVariable> variables = new LinkedList<>();
    public TemporalVariable getNewTemporalVariable(){
        return new TemporalVariable();
    }
}
