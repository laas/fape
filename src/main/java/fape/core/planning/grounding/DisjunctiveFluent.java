package fape.core.planning.grounding;

import fape.core.planning.planninggraph.Landmark;
import fape.core.planning.planninggraph.PGUtils;
import fape.core.planning.states.State;
import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.concrete.VarRef;

import java.util.*;

public class DisjunctiveFluent implements Landmark {

    public final Set<Fluent> fluents;

    public DisjunctiveFluent(Collection<Fluent> fluents) {
        this.fluents = new HashSet<>(fluents);
    }

    public DisjunctiveFluent(ParameterizedStateVariable sv, VarRef value, State st) {
        this.fluents = new HashSet<>(fluentsOf(sv, value, st, false));
    }

    public static Collection<Fluent> fluentsOf(ParameterizedStateVariable sv, VarRef value, State st, boolean addChangeableFluents) {
        HashSet<Fluent> fluents = new HashSet<>();
        List<VarRef> variables = new LinkedList<>();
        for(VarRef var : sv.args()) {
            if(!variables.contains(var)) {
                variables.add(var);
            }
        }
        if(!variables.contains(value)) {
            variables.add(value);
        }

        List<List<VarRef>> valuesSets = new LinkedList<>();
        for(VarRef var : variables) {
            List<VarRef> values = new LinkedList<>();
            for(String val : st.domainOf(var)) {
                values.add(st.pb.instances().referenceOf(val));
            }
            valuesSets.add(values);
        }

        List<List<VarRef>> argList = PGUtils.allCombinations(valuesSets);

        for(List<VarRef> args : argList) {

            VarRef[] fluentArgs = new VarRef[sv.args().length];
            int i =0;
            for(VarRef arg : sv.args()) {
                int argIndex;
                for(argIndex=0 ; argIndex<variables.size() ; argIndex++) {
                    if(arg.equals(variables.get(argIndex)))
                        break;
                }
                assert argIndex < args.size() : "Couldn't find arggument for ";
                fluentArgs[i++] = args.get(argIndex);
            }
            int argIndex;
            for(argIndex=0 ; argIndex<variables.size() ; argIndex++) {
                if(value.equals(variables.get(argIndex)))
                    break;
            }
            VarRef varOfValue = args.get(argIndex);

            fluents.add(new Fluent(sv.func(), fluentArgs, varOfValue, false));
            if(addChangeableFluents)
                fluents.add(new Fluent(sv.func(), fluentArgs, varOfValue, true));
        }
        return fluents;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(Fluent f : fluents) {
            sb.append(f.toString());
            sb.append('\n');
        }
        sb.append('\n');
        return sb.toString();
    }


}
