package fape.core.planning.planninggraph;

import fape.core.planning.states.State;
import fape.exceptions.FAPEException;
import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.concrete.VarRef;

import java.util.*;

public class DisjunctiveFluent implements Landmark {

    public final Set<Fluent> fluents;

    public DisjunctiveFluent(Collection<Fluent> fluents) {
        this.fluents = new HashSet<>(fluents);
    }

    public DisjunctiveFluent(ParameterizedStateVariable sv, VarRef value, State st, GroundProblem pb) {
        List<VarRef> variables = new LinkedList<>();
        for(VarRef var : sv.jArgs()) {
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
                values.add(pb.liftedPb.instances().referenceOf(val));
            }
            valuesSets.add(values);
        }

        List<List<VarRef>> argList = PGUtils.allCombinations(valuesSets);

        this.fluents = new HashSet<>();
        for(List<VarRef> args : argList) {

            List<VarRef> fluentArgs = new LinkedList<>();
            for(VarRef arg : sv.jArgs()) {
                int argIndex;
                for(argIndex=0 ; argIndex<variables.size() ; argIndex++) {
                    if(arg.equals(variables.get(argIndex)))
                        break;
                }
                if(argIndex >= args.size())
                    throw new FAPEException("Couldn't find arggument for ");
                fluentArgs.add(args.get(argIndex));
            }
            int argIndex;
            for(argIndex=0 ; argIndex<variables.size() ; argIndex++) {
                if(value.equals(variables.get(argIndex)))
                    break;
            }
            VarRef varOfValue = args.get(argIndex);

            fluents.add(new Fluent(sv.func(), fluentArgs, varOfValue));
        }
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
