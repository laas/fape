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

import fape.core.execution.model.*;

import fape.core.planning.Planner;
import fape.core.planning.states.State;
import fape.core.planning.stn.TemporalVariable;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.core.planning.temporaldatabases.events.TemporalEvent;
import fape.core.planning.temporaldatabases.events.propositional.PersistenceEvent;
import fape.exceptions.FAPEException;
import fape.util.Pair;

import java.util.*;

/**
 * this is an action in the task network, it may be decomposed
 *
 * @author FD
 */
public class Action {

    public enum Status { FAILED, EXECUTED, EXECUTING, PENDING; }

    public static int idCounter = 0;
    public int mID = idCounter++;

    public float minDuration = -1.0f;
    public float maxDuration = -1.0f;

    public TemporalVariable start, end;

    public String name;

    public List<Pair<List<ActionRef>, List<TemporalConstraint>>> refinementOptions; //those are the options how to decompose
    public List<Instance> params;
    public List<Reference> constantParams;
    public Status status = Status.PENDING;

    public TreeMap<String, VariableRef> localVariables = new TreeMap<>();
    private LinkedList<TemporalEvent> events = new LinkedList<>();
    private LinkedList<TemporalInterval> eventsIntervals = new LinkedList<>();

    public Action() {}

    public Action(Problem pb, ActionRef ref, Action parent) {
        AbstractAction abs = pb.actions.get(ref.name);
        if (abs == null) {
            throw new FAPEException("Seeding unknown action: " + ref.name);
        }

        params = abs.params;
        constantParams = ref.args;

        // contains bindings between state variables and variables
        List<Pair<Reference,String>> hardBindings = new LinkedList<>();

        if (parent != null) {
            // this look for parameters that where given a stateVariable value from the parent action
            // (typically r.right), add a pair of references with the stateVariable and the parameter name
            for(int refCt=0 ; refCt<constantParams.size() ; refCt++) {
                Reference r = constantParams.get(refCt);
                if(r.refs.size() > 1) {
                    String var = Planner.NewUnbindedVarName();
                    String param = params.get(refCt).name;
                    hardBindings.add(new Pair(r, param));
                    constantParams.remove(refCt);
                    constantParams.add(refCt, new Reference(var));
                }
            }
        }

        // for all parameters, register a new variable
        for(int i=0 ; i<params.size() ; i++) {
            Instance param = params.get(i);
            String varName = constantParams.get(i).variable();

            assert pb.types.containsType(param.type) : "Unknown type";

            if(pb.types.containsObject(varName)) {
                // variable is a domain constant
                localVariables.put(param.name, new VariableRef(varName, param.type));
            } else if(parent != null && parent.localVariables.containsKey(varName)) {
                // variable is defined in the parent action
                localVariables.put(param.name, parent.localVariables.get(varName));
            } else {
                // variable is undefined
                localVariables.put(param.name, new VariableRef(varName, param.type));
            }
        }

        // set the same name
        name = abs.name;

        //add the refinements
        refinementOptions = abs.strongDecompositions;

        // look for all parameters of decomposed actions and creates new variables from the unknown ones
        Map<String, String> unknownVars = new TreeMap<>();
        for(Pair<List<ActionRef>, List<TemporalConstraint>> dec : refinementOptions) {
            for(ActionRef act : dec.value1) {
                for(int argNum=0 ; argNum < act.args.size() ; argNum++) {
                    String argVar = act.args.get(argNum).variable();
                    if(!localVariables.containsKey(argVar) && !pb.types.containsObject(argVar)) {
                        String type = pb.actions.get(act.name).params.get(argNum).type;
                        if(unknownVars.containsKey(argVar) && !unknownVars.get(argVar).equals(type))
                            throw new FAPEException("Unsupported: variable has two different types (need to look for common descendant");
                        unknownVars.put(argVar, type);
                    }
                }
            }
        }

        for(String varName : unknownVars.keySet()) {
            localVariables.put(varName, new VariableRef(Planner.NewUnbindedVarName(), unknownVars.get(varName)));
        }

        maxDuration = abs.GetDuration();

        // This creates persistence event for every parameter of the form "r.right -> g"
        for(Pair<Reference,String> binding : hardBindings) {
            Reference svRef = binding.value1;

            // state variable is the reference passed (ex: r.right)
            VariableRef svParam = parent.localVariables.get(svRef.variable());
            String predType = pb.types.getContentType(svParam.type, svRef.predicate());//st.GetType(svRef);

            ParameterizedStateVariable sv = new ParameterizedStateVariable(svRef.predicate(), svParam, predType);

            // create persistence on the value (ex: g)
            PersistenceEvent ev = new PersistenceEvent(sv, localVariables.get(binding.value2));

            // Event is forced during the whole action
            TemporalInterval all = new TemporalInterval("TStart", "TEnd");
            addEvent(ev, all);
        }

        // bind all events and add them to the action
        for (AbstractTemporalEvent ev : abs.events) {
            // get the event binded into the current action
            TemporalEvent event = ev.event.bindedCopy(this);
            addEvent(event, ev.interval);
        }//event transformation end
    }

    public Collection<TemporalEvent> events() {
        return events;
    }

    public TemporalInterval intervalOf(TemporalEvent e) {
        return eventsIntervals.get(events.indexOf(e));
    }

    private void addEvent(TemporalEvent e, TemporalInterval interval) {
        events.add(e);
        eventsIntervals.add(interval);
    }

    @Deprecated
    public void clearEvents() {
        events = new LinkedList<>();
    }



    /**
     *
     * @return
     */
    public boolean IsRefinable() {
        return refinementOptions.size() > 0 && decomposition == null;
    }
    public List<Action> decomposition; //this is the truly realized decomposition

    /**
     *
     * @return
     */
    public Action DeepCopy(List<TemporalEvent> updatedEvents) {
        Action a = new Action();
        a.status = this.status;
        a.mID = mID;
        a.params = this.params;
        a.constantParams = this.constantParams;
        if (this.decomposition == null) {
            a.decomposition = null;
        } else {
            a.decomposition = new LinkedList<>();
            for (Action b : this.decomposition) {
                a.decomposition.add(b.DeepCopy(updatedEvents));
            }
        }

        a.minDuration = this.minDuration;
        a.maxDuration = this.maxDuration;
        a.end = this.end;
        a.name = this.name;
        a.refinementOptions = this.refinementOptions;
        a.start = this.start;
        a.localVariables = this.localVariables;

        // events are mutable and hence need to be mapped to the events
        // cloned by the temporal database manager
        for(TemporalEvent ev : this.events()) {
            TemporalEvent updatedEv = null;
            for(TemporalEvent newEv : updatedEvents) {
                if(newEv.mID == ev.mID)
                    updatedEv = newEv;
            }
            if(updatedEv == null) {
                throw new FAPEException("Unable to find the updated event for " + ev);
            }
            a.addEvent(updatedEv, this.intervalOf(ev));
        }

        return a;
    }

    public ActionRef BindedActionRef(ActionRef actionRef) {
        ActionRef binded = new ActionRef();
        binded.name = actionRef.name;
        for(Reference arg : actionRef.args) {
            binded.args.add(BindedReference(arg));
        }
        return binded;
    }

    /**
     * Returns a reference where usage of a parameter is replaced by the appropriate variable.
     * @param ref
     * @return
     */
    public Reference BindedReference(Reference ref) {
        Reference ret = new Reference(ref);
        String first = ret.GetConstantReference();
        if(localVariables.containsKey(first)) {
            ret.ReplaceFirstReference(localVariables.get(first).var);
        }
        return ret;

    }

    public VariableRef GetBindedVariableRef(VariableRef var) {
        return GetBindedVariableRef(var.GetReference(), var.type);
    }

    public VariableRef GetBindedVariableRef(Reference ref, String type) {
        return new VariableRef(BindedReference(ref), type);
    }

    public VariableRef GetBindedVariableRef(String varName, String type) {
        return GetBindedVariableRef(new Reference(varName), type);
    }

    public ParameterizedStateVariable getBindedStateVariable(ParameterizedStateVariable sv) {
        return new ParameterizedStateVariable(sv.predicateName, GetBindedVariableRef(sv.variable), sv.type);
    }

    public String GetType(State st, Reference ref) {
        assert ref.refs.size() == 1;
        return GetType(st, ref.GetConstantReference());
    }

    public String GetType(State st, String variable) {
        for(int i=0 ; i<params.size() ; i++) {
            if(params.get(i).name.equals(variable)) {
                return params.get(i).type;
            }
        }
        return st.parameterBindings.get(variable).type;
    }

    @Override
    public String toString() {
        return name;
    }

    public float GetCost() {
        return 1.0f;
    }

    public List<String> ProduceParameters(State st) {
        List<String> ret = new LinkedList<>();

        for(Instance i : this.params) {
            VariableRef var = this.localVariables.get(i.name);
            assert st.parameterBindings.containsKey(var);
            assert st.parameterBindings.get(var).domain.size() == 1;
            ret.add(st.parameterBindings.get(var).domain.getFirst());
        }

        return ret;
    }

}
