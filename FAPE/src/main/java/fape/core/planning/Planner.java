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
package fape.core.planning;


import fape.core.planning.planner.APlanner;
import fape.core.planning.planner.ActionExecution;
import fape.core.planning.preprocessing.ActionSupporterFinder;
import fape.core.planning.preprocessing.ActionSupporters;
import fape.core.planning.search.Flaw;
import fape.core.planning.search.resolvers.Resolver;
import fape.core.planning.states.State;
import fape.util.Pair;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.concrete.ActRef;
import planstack.constraints.stnu.Controllability;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The base line planner that stick to a lifted representation and supports the whole range of anml problems.
 *
 * TODO: use lifted abstraction hierarchies & more efficient DTG
 */
public class Planner extends APlanner {



    Comparator<Pair<Flaw, List<Resolver>>> optionsComparatorMinDomain = new Comparator<Pair<Flaw, List<Resolver>>>() {
        @Override
        public int compare(Pair<Flaw, List<Resolver>> o1, Pair<Flaw, List<Resolver>> o2) {
            return o1.value2.size() - o2.value2.size();
        }
    };

    public Planner(State initialState, String[] planSelStrategies, String[] flawSelStrategies, Map<ActRef, ActionExecution> actionsExecutions) {
        super(initialState, planSelStrategies, flawSelStrategies, actionsExecutions);
    }

    public Planner(Controllability controllability, String[] planSelStrategies, String[] flawSelStrategies) {
        super(controllability, planSelStrategies, flawSelStrategies);
    }

    @Override
    public String shortName() {
        return "base";
    }

    @Override
    public State search(long deadline) {
        return aStar(deadline);
    }

    @Override
    public ActionSupporterFinder getActionSupporterFinder() {
        return new ActionSupporters(pb);
    }
}
