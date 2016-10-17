package fr.laas.fape.planning.core.planning.grounding;

import fr.laas.fape.anml.model.ParameterizedStateVariable;
import fr.laas.fape.anml.model.concrete.InstanceRef;
import fr.laas.fape.anml.model.concrete.VarRef;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.util.Utils;

import java.util.*;

public class DisjunctiveFluent {

    public final Set<Fluent> fluents;

    public DisjunctiveFluent(Collection<Fluent> fluents) {
        this.fluents = new HashSet<>(fluents);
    }

    public DisjunctiveFluent(ParameterizedStateVariable sv, VarRef value, State st, Planner planner) {
        this.fluents = new HashSet<>(fluentsOf(sv, value, st, planner));
    }

    public static Set<Fluent> fluentsOf(ParameterizedStateVariable sv, VarRef value, State st, Planner planner) {
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

        List<List<InstanceRef>> valuesSets = new LinkedList<>();
        for(VarRef var : variables) {
            valuesSets.add(st.valuesOf(var));
        }

        List<List<InstanceRef>> argList = Utils.allCombinations(valuesSets);

        for(List<InstanceRef> args : argList) {

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
            InstanceRef varOfValue = args.get(argIndex);

            GStateVariable gsv = planner.preprocessor.getStateVariable(sv.func(), fluentArgs);
            fluents.add(planner.preprocessor.getFluent(gsv, varOfValue));
        }
        return fluents;
    }

    public static Set<GStateVariable> instantiationsOf(ParameterizedStateVariable sv, State st) {
        HashSet<GStateVariable> svs = new HashSet<>();
        List<VarRef> variables = new LinkedList<>();
        for(VarRef var : sv.args()) {
            if(!variables.contains(var)) {
                variables.add(var);
            }
        }

        List<List<InstanceRef>> valuesSets = new LinkedList<>();
        for(VarRef var : variables) {
            List<InstanceRef> values = new LinkedList<>();
            for(String val : st.domainOf(var)) {
                values.add(st.pb.instances().referenceOf(val));
            }
            valuesSets.add(values);
        }

        List<List<InstanceRef>> argList = Utils.allCombinations(valuesSets);

        for(List<InstanceRef> args : argList) {

            VarRef[] fluentArgs = new VarRef[sv.args().length];
            int i =0;
            for(VarRef arg : sv.args()) {
                int argIndex;
                for(argIndex=0 ; argIndex<variables.size() ; argIndex++) {
                    if(arg.equals(variables.get(argIndex)))
                        break;
                }
                assert argIndex < args.size() : "Couldn't find argument for ";
                fluentArgs[i++] = args.get(argIndex);
            }

            GStateVariable gsv = st.pl.preprocessor.getStateVariable(sv.func(), fluentArgs);
            svs.add(gsv);
        }
        return svs;
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
