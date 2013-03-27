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

package fape.model.compact;

import fape.model.compact.tqes.Tqe;
import fape.util.Pair;
import java.util.List;

/**
 *
 * @author FD
 */
public class Action {
    String name;
    List<Parameter> params; //typed parameters
    List<Tqe> tques; //what happens
    float duration; //end - start of the action
    float softRefinement; //how critical is it to make some strongDecomposition for this action
    Tqe hardRefinement; //if this holds, we need to make strongDecomposition
    Pair<List<Action>, List<TemporalConstraint>> weakDecomposition; //we can always try to apply this decomposition, it contains some actions and constraints on their ordering
    List<Pair<List<Action>, List<TemporalConstraint>>> strongDecompositions; //same as weak though we choose only one from those
}
