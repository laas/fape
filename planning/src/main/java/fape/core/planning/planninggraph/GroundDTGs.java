package fape.core.planning.planninggraph;

import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GStateVariable;
import fape.core.planning.heuristics.relaxed.DTGImpl;
import fape.core.planning.planner.APlanner;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.concrete.InstanceRef;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GroundDTGs {

    private final AnmlProblem pb;

    Map<GStateVariable, DTGImpl> dtgs = new HashMap<>();
    final APlanner planner;

    public GroundDTGs(Collection<GAction> actions, AnmlProblem pb, APlanner planner) {
        this.pb = pb;
        this.planner = planner;
        for(GAction ga : actions) {

            for(Fluent effect : ga.add) {
                boolean added = false;
                if(!dtgs.containsKey(effect.sv)) {
                    dtgs.put(effect.sv, initDTGForStateVariable(effect.sv));
                    // initialize nodes
                    for(String val : pb.instances().instancesOfType(effect.sv.f.valueType())) {
                        InstanceRef instance = pb.instance(val);
                        Fluent f = planner.preprocessor.getFluent(effect.sv, instance);
                        dtgs.get(effect.sv).addNodeIfAbsent(f, 0, null, null);
                    }

                }
                for(Fluent precondition : ga.pre) {
                    if(effect.sv.equals(precondition.sv)) {
//                        assert !added : "A transition has two preconditions";

                        added = true;
                        dtgs.get(effect.sv).addEdge(precondition, 0, null, ga, effect, 0);
                    }
                }
                if(!added) // this is an unconditional transition
                    dtgs.get(effect.sv).addEdge(null, 0, null, ga, effect, 0);
            }
        }
    }

    public boolean hasDTGFor(GStateVariable sv) {
        return dtgs.containsKey(sv);
    }

    public DTGImpl getDTGOf(GStateVariable sv) {
        if(!dtgs.containsKey(sv)) {
            // the dtg was not initialized because no actions have an effect on it
            dtgs.put(sv, initDTGForStateVariable(sv));
        }
        return dtgs.get(sv);
    }

    public DTGImpl initDTGForStateVariable(GStateVariable sv) {
        DTGImpl dtg = new DTGImpl(1, false);
        // initialize nodes
        for(String val : pb.instances().instancesOfType(sv.f.valueType())) {
            InstanceRef instance = pb.instance(val);
            final Fluent f = planner.preprocessor.getFluent(sv, instance);
            dtg.addNode(f, 0, null, null);
            dtg.setEntryPoint(f, 0);
        }
        dtg.addNode(null, 0, null, null);
        dtg.setAccepting(null, 0);
        return dtg;
    }
}
