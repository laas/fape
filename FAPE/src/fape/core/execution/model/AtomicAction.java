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

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class AtomicAction {
    /*
    private static int idCounter = 0;
    public int mID = idCounter++;*/
    
    public enum EResult{
        SUCCESS, FAILURE
    }
    
    public int mStartTime;
    public int mID;
    public int duration;
    public String name;
    public List<String> params = new LinkedList<>();
    public String GetDescription(){
        String ret = "";
        
        ret += "("+name;
        for(String st:params){
            ret += " "+st;
        }
        ret += ")";        
        return ret;
    }   
}
