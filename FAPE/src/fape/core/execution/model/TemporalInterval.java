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
package fape.core.execution.model;

import fape.core.planning.stn.TemporalVariable;
import fape.core.planning.temporaldatabases.events.TemporalEvent;
import fape.exceptions.FAPEException;

/**
 *
 * @author FD
 */
public class TemporalInterval {

    /**
     *
     */
    public String s,

    /**
     *
     */
    e;

    @Override
    public String toString() {
        return "[" + e + ", " + s + "]";
    }
    
    /**
     *
     * @param ev
     * @param start
     * @param end
     */
    public void AssignTemporalContext(TemporalEvent ev, TemporalVariable start, TemporalVariable end){
        switch (s) {
            case "TStart":
                ev.start = start;
                break;
            case "TEnd":
                ev.start = end;
                break;
            default:
                throw new FAPEException("Unsupported temporal annotation.");
        }
        
        switch (e) {
            case "TStart":
                ev.end = start;
                break;
            case "TEnd":
                ev.end = end;
                break;
            default:
                throw new FAPEException("Unsupported temporal annotation.");
        }
            
    }
}
