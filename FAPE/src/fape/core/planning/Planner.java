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

import fape.core.execution.model.ANMLBlock;
import fape.core.execution.model.AtomicAction;
import fape.core.execution.model.Instance;
import fape.core.execution.model.statements.Statement;
import fape.core.execution.model.types.Type;
import fape.core.planning.model.StateVariable;
import fape.core.planning.states.State;
import fape.core.transitions.TransitionIO2Planning;
import fape.exceptions.FAPEException;
import fape.util.Pair;
import fape.util.TimeAmount;
import fape.util.TimePoint;
import java.util.HashMap;

import java.util.List;
import javax.swing.SwingWorker;

/**
 *
 * @author FD
 */
public class Planner {

    public State init;
    // a list of types keyed by its name
    public HashMap<String, fape.core.planning.model.Type> types = new HashMap<>();
    public HashMap<String, StateVariable> vars = new HashMap<>();

    public enum EPlanState {

        CONSISTENT, INCONSISTENT, INFESSIBLE, UNINITIALIZED
    }
    /**
     * what is the current state of the plan
     */
    public EPlanState planState = EPlanState.UNINITIALIZED;

    /**
     * initializes the data structures of the planning problem
     *
     * @param pl
     */
    public void Init() {
        init = new State();
    }

    public State GetCurrentState() {
        return init;
    }

    /**
     * starts plan repair, records the best plan, produces the best plan after
     * <b>forHowLong</b> miliseconds or null, if no plan was found
     *
     * @param forHowLong
     */
    public void Repair(TimeAmount forHowLong) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * progresses in the plan up for howFarToProgress, returns either
     * AtomicActions that were instantiated with corresponding start times, or
     * null, if not solution was found in the given time
     *
     * @param howFarToProgress
     * @param forHowLong
     * @return
     */
    public List<Pair<AtomicAction, TimePoint>> Progress(TimeAmount howFarToProgress, TimeAmount forHowLong) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * restarts the planning problem into its initial state
     */
    public void Restart() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * enforces given facts into the plan (possibly breaking it) this is an
     * incremental step, if there was something already defined, the name
     * collisions are considered to be intentional
     *
     * @param pl
     */
    public void ForceFact(ANMLBlock pl) {
        //read everything that is contained in the ANML block

        // this a generic predecesor of all types
        types.put("object", new fape.core.planning.model.Type());

        //convert types
        for (Type t : pl.types) {
            types.put(t.name, TransitionIO2Planning.transformType(t, types));
        }

        //convert instances and create state variables from them
        for (Instance i : pl.instances) {
            List<StateVariable> l = TransitionIO2Planning.decomposeInstance("", i.name, i.type, types);
            for (StateVariable v : l) {
                vars.put(v.name, v);
            }
        }

        //process statements
        for (Statement s : pl.statements) {
            if (!vars.containsKey(s.GetVariableName())) {
                throw new FAPEException("Unknown state variable: " + s.GetVariableName());
            }
            TransitionIO2Planning.InsertStatementIntoState(s, vars.get(s.GetVariableName()), GetCurrentState());
        }
        
        //process 

        //for()
        int xx = 0;
    }
}
