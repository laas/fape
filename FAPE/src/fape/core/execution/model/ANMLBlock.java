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

import fape.core.execution.model.statements.Statement;
import fape.core.execution.model.types.Type;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */ 
public class ANMLBlock { 
    public List<Action> actions = new LinkedList<>(); //actio descriptions in the system 
    public List<Statement> statements = new LinkedList<>(); //statements that describe the system
    public List<Type> types = new LinkedList<>(); //types in the ssytem
    public List<Instance> instances = new LinkedList<>(); //variables in the system
    public List<ActionRef> actionsForTaskNetwork = new LinkedList<>(); //actions at the top of the task network
}
