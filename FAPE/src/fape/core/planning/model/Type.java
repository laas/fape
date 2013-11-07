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

import java.util.HashMap;

/**
 *
 * @author FD
 */
public class Type {
    /**
     * the variables that are nested in this type, the first string is the name,
     * the second one is the name of its type
     */
    public HashMap<String, String> contents;
    /**
     * inherit from a parent type
     * @param parent 
     */
    public Type(Type parent){
        contents = new HashMap<>(parent.contents);
    }
}
