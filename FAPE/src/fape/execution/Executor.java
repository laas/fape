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

package fape.execution;

import fape.acting.Actor;
import fape.model.AtomicAction;
import fape.planning.Planner;
import fape.util.Pair;
import fape.util.TimePoint;
import java.util.List;

/**
 *
 * @author FD
 */
public class Executor {
    
    Actor mActor;
    Listener mListener;
    
    public void bind(Actor a, Listener l){
        mActor = a;
        mListener = l;
    }
    
    /**
     * performs the translation between openPRS and ANML model
     * @param message 
     */
    public void eventReceived(String message) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    /**
     * performs the translation between openPRS and ANML model
     * @param acts 
     */
    public void executeAtomicActions(List<Pair<AtomicAction,TimePoint>> acts){
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
}
