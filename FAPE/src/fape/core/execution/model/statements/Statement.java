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

package fape.core.execution.model.statements;

import fape.core.execution.model.Reference;
import fape.core.execution.model.TemporalInterval;

/**
 * temporarily qualified expression
 * @author FD
 */
public class Statement {

    /**
     *
     */
    public String operator;

    /**
     *
     */
    public Reference from,

    /**
     *
     */
    to;

    /**
     *
     */
    public TemporalInterval interval;

    /**
     *
     */
    public Reference leftRef;

    /**
     *
     */
    //public Reference rightRef;

    /**
     *
     */
    public float value;

    /**
     *
     * @return
     */
    public float GetResourceValue(){
        return Float.parseFloat(from.refs.getFirst().toString());
    }

    /**
     *
     * @return
     */
    public boolean IsResourceRelated(){
        if(from == null){
            return false;
        }
        try{
            Float.parseFloat(from.refs.getFirst());
            return true;
        }catch(NumberFormatException e){
            return false;
        }        
    }

    /**
     *
     * @return
     */
    public String GetVariableName(){
        String st = "";
        for(String s:leftRef.refs){
            st += s + ".";
        }
        return st.substring(0, st.length()-1);
    }
}
