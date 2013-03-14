package fape;

import fape.acting.Actor;
import fape.execution.Executor;
import fape.execution.Listener;
import fape.planning.Planner;

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

/**
 * starts up all the components and binds them together
 * @author FD
 */
public class FAPE {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        Actor a = new Actor();
        Planner p = new Planner();
        Executor e = new Executor();
        Listener l = new Listener(null, null, null, null);
        
        a.bind(e, p);
        e.bind(a, l);
        l.bind(e);
        
        a.run();
    }
}
