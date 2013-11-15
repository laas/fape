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

package fape.core.planning.model;

/**
 *
 * @author FD
 */
public class StateVariable {
    public enum EStateVariableType{
        BOOLEAN, FLOAT, INTEGER, ENUM
    }
    public EStateVariableType mType;
    /**
     * fully qualifying name, list of nesting separated by dots
     */
    public String name;    
}
