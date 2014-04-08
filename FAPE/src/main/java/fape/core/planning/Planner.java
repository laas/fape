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


import fape.core.execution.Executor;
import fape.core.execution.model.AtomicAction;
import fape.core.planning.planner.APlanner;
import fape.core.planning.preprocessing.ActionDecompositions;
import fape.core.planning.preprocessing.ActionSupporters;
import fape.core.planning.printers.Printer;
import fape.core.planning.search.*;
import fape.core.planning.search.abstractions.AbstractionHierarchy;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.ChainComponent;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.exceptions.FAPEException;
import fape.util.Pair;
import fape.util.TimeAmount;
import fape.util.TinyLogger;
import planstack.anml.model.LVarRef;
import planstack.anml.model.concrete.ActRef;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.AbstractDecomposition;
import planstack.anml.model.concrete.*;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.anml.model.concrete.statements.Statement;
import planstack.anml.parser.ParseResult;

import java.util.*;

/**
*
* @author FD
*/
public class Planner extends APlanner {


    @Override
    public String shortName() {
        return "baseline";
    }

    @Override
    public State search(TimeAmount forHowLong) {
        return aStar(forHowLong);
    }

    @Override
    public Comparator<Pair<Flaw, List<SupportOption>>> flawComparator(State st) {
        return optionsComparatorMinDomain;
    }

    @Override
    public Comparator<State> stateComparator() {
        return new StateComparator();
    }

    Comparator<Pair<Flaw, List<SupportOption>>> optionsComparatorMinDomain = new Comparator<Pair<Flaw, List<SupportOption>>>() {
        @Override
        public int compare(Pair<Flaw, List<SupportOption>> o1, Pair<Flaw, List<SupportOption>> o2) {
            return o1.value2.size() - o2.value2.size();
        }
    };

    public static void main(String[] args) throws InterruptedException {
        long start = System.currentTimeMillis();
        String anml = "problems/handover.anml";

        if(args.length > 0)
            anml = args[0];
        Planner p = new Planner();
        Planner.actionResolvers = true;
        p.Init();
        p.ForceFact(Executor.ProcessANMLfromFile(anml));
        boolean timeOut = false;
        try {
            timeOut = !p.Repair(new TimeAmount(1000 * 6000));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Planning finished for " + anml + " with failure.");
            //throw new FAPEException("Repair failure.");
        }
        long end = System.currentTimeMillis();
        float total = (end - start) / 1000f;
        if (timeOut) {
            System.out.println("Planning finished for " + anml + " timed out.");
        } else {
            System.out.println("Planning finished for " + anml + " in " + total + "s");
            State sol = p.GetCurrentState();

            System.out.println("=== Temporal databases === \n"+ Printer.temporalDatabaseManager(sol, sol.tdb));

            Plan plan = new Plan(sol);
            plan.exportToDot("plan.dot");
            System.out.println("Look at plan.dot for a complete plan.");
        }
    }
}
