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

package fape.core.execution.model.types;

import fape.core.execution.model.Instance;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class Type {
    public String name;
    public String parent;
    public List<Instance> instances = new LinkedList<>();

    @Override
    public String toString() {
        return name;
    }
    
    
}
