package fape.core.planning.planninggraph;

import fape.util.Pair;
import planstack.anml.model.abs.AbstractAction;

import java.util.*;

public class DisjunctiveAction {

    public final Set<GroundAction> actions;

    public DisjunctiveAction(Collection<GroundAction> actions) {
        this.actions = new HashSet<>(actions);
    }

    public DisjunctiveAction() {
        this.actions = new HashSet<>();
    }

    public List<Pair<AbstractAction, List<Set<String>>>> actionsAndParams(GroundProblem pb) {
        List<Pair<AbstractAction, List<Set<String>>>> ret = new LinkedList<>();

        for(GroundAction act : actions) {
            List<Set<String>> argValues = null;
            for(Pair<AbstractAction, List<Set<String>>> actionRet : ret) {
                if(actionRet.value1 == act.act.abs()) {
                    argValues = actionRet.value2;
                }
            }

            if(argValues == null) {
                argValues = new LinkedList<>();
                for(int i=0 ; i<act.params.size() ; i++) {
                    argValues.add(new HashSet<String>());
                }
                ret.add(new Pair(act.act.abs(), argValues));
            }

            for(int i=0 ; i<act.params.size() ; i++) {
                argValues.get(i).add(pb.valueOf(act.params.get(i)));
            }
        }

        return ret;
    }
}
