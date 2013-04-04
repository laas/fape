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

package fape.model.compact.types;

import fape.model.compact.Variable;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class Type {
    public String name;
    public Type parent;
    List<Variable> vars = new LinkedList<>();
}
