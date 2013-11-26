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
import fape.core.execution.model.ActionRef;
import fape.core.execution.model.AtomicAction;
import fape.core.execution.model.Instance;
import fape.core.execution.model.Reference;
import fape.core.execution.model.statements.Statement;
import fape.core.execution.model.types.Type;
import fape.core.planning.bindings.ObjectVariable;
import fape.core.planning.model.AbstractAction;
import fape.core.planning.model.AbstractTemporalEvent;
import fape.core.planning.model.Action;
import fape.core.planning.model.StateVariable;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.core.planning.temporaldatabases.events.TemporalEvent;
import fape.core.planning.temporaldatabases.events.resources.ConsumeEvent;
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
    public HashMap<String, AbstractAction> actions = new HashMap<>();

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

        State st = GetCurrentState();

        // this a generic predecesor of all types
        types.put("object", new fape.core.planning.model.Type());

        //convert types
        for (Type t : pl.types) {
            types.put(t.name, TransitionIO2Planning.transformType(t, types));
        }

        //convert instances and create state variables from them
        for (Instance i : pl.instances) {
            List<StateVariable> l = TransitionIO2Planning.decomposeInstance("", i.name, i.type, types, i.type);
            for (StateVariable v : l) {
                vars.put(v.name, v);
            }
        }

        //process statements
        for (Statement s : pl.statements) {
            if (!vars.containsKey(s.GetVariableName())) {
                throw new FAPEException("Unknown state variable: " + s.GetVariableName());
            }
            TransitionIO2Planning.InsertStatementIntoState(s, vars.get(s.GetVariableName()), st);
        }

        //process actions
        for (fape.core.execution.model.Action a : pl.actions) {
            if (actions.containsKey(a.name)) {
                throw new FAPEException("Overriding action abstraction: " + a.name);
            }
            AbstractAction act = TransitionIO2Planning.TransformAction(a);
            actions.put(act.name, act);
        }

        //process seeds
        for (ActionRef ref : pl.actionsForTaskNetwork) {
            AbstractAction abs = actions.get(ref.name);
            if (abs == null) {
                throw new FAPEException("Seeding unknown action: " + ref.name);
            }

            Action act = new Action();
            // set the same name
            act.name = abs.name;
            //prepare the time points
            
            act.start = st.tempoNet.getNewTemporalVariable();
            act.end = st.tempoNet.getNewTemporalVariable();
            act.duration = abs.GetDuration(st);
            boolean success = st.tempoNet.EnforceConstraint(act.start, act.end, (int) act.duration, (int) act.duration);
            if (!success) {
                throw new FAPEException("The initial temporal network is inconsistent.");
            }
            // get the action parameters
            /*for (int i = 0; i < abs.params.size(); i++) {
             Instance in = abs.params.get(i);
             Reference re = ref.args.get(i);
             ObjectVariable obj = st.bindings.getNewObjectVariable();
             if (re.refs.getFirst().endsWith("_")) {
             // this is an unbinded reference, we need to add all possible state variables     
             String searchStr = "";
             if (re.toString().contains(".")) {
             searchStr = re.toString().split("_")[1];
             }
             //in.
             for (String str : vars.keySet()) {
             String type = vars.get(str).type;
             if ((str.endsWith(searchStr)||searchStr.equals("")) && in.type.equals(type)) {
             obj.domain.add(vars.get(str));
             }
             }
             } else {
             // this is binded to a constant - one specific state variable
             obj.domain.add(vars.get(re.toString()));
             }
             act.parameters.add(obj);
             }*/
            for (AbstractTemporalEvent ev : abs.events) {
                
                TemporalEvent event = ev.event.cc();                               
                
                // sets the temporal interval into the context of the temporal context of the parent action
                ev.interval.AssignTemporalContext(event, act.start, act.end);
                
                String stateVariableReferenceSuffix = ev.stateVariableReference.toString();
                stateVariableReferenceSuffix = stateVariableReferenceSuffix.substring(stateVariableReferenceSuffix.indexOf("."), stateVariableReferenceSuffix.length());
                //we need to asociate corresponding object variables with the events

                //now i find the variable that instantiates my state variable reference
                ObjectVariable obj = st.bindings.getNewObjectVariable();
                for (int i = 0; i < abs.params.size(); i++) {
                    Instance instanceOfTheParameter = abs.params.get(i);
                    if (instanceOfTheParameter.name.equals(ev.stateVariableReference.refs.getFirst())) {
                        //this is the right parameter
                        Reference re = ref.args.get(i);
                        
                        if (re.refs.getFirst().endsWith("_")) {
                            // this is an unbinded reference, we need to add all possible state variables     
                            /*String searchStr = stateVariableReferenceSuffix;
                            if (re.toString().contains(".")) {
                                searchStr = re.toString().split("_")[1] + stateVariableReferenceSuffix;
                            }*/
                            String typeDerivation = instanceOfTheParameter.type + stateVariableReferenceSuffix;
                            for (String str : vars.keySet()) {
                                //String type = vars.get(str).type;
                                StateVariable var = vars.get(str);
                                if (var.typeDerivationName.equals(typeDerivation)) {
                                    obj.domain.add(vars.get(str));
                                }
                            }
                        } else {
                            // this is binded to a constant - one specific state variable
                            StateVariable var = vars.get(re.toString()+stateVariableReferenceSuffix);
                            if(var == null){
                                throw new FAPEException("Unknown state variable.");
                            }
                            obj.domain.add(var);
                        }
                    }
                }
                //now we have the object variable for the event, we insert it into the action and create a temporal database for it
                TemporalDatabase db = st.tdb.GetNewDatabase();
                db.var = obj;
                event.objectVar = obj;
                //we add the event into the database and the action
                act.events.add(event);
                db.events.add(event);
            }
            
            //now we need to propagate the binding constraints
            List<Pair<Integer,Integer>> binds = abs.GetLocalBindings();
            for(Pair<Integer,Integer> p:binds){
                st.bindings.AddBinding(act.events.get(p.value1).objectVar, act.events.get(p.value2).objectVar);
            }
            
            
            int xx = 0;
        }

        //for()
        int xx = 0;
    }
}

