package fape.core.planning.planninggraph;

import fape.util.Pair;
import planstack.anml.model.abs.AbstractAction;

import java.util.*;

/**
 * A disjunctive action is a set of ground actions.
 */
public class DisjunctiveAction implements Landmark {

    public final Set<GroundAction> actions;

    public DisjunctiveAction(Collection<GroundAction> actions) {
        this.actions = new HashSet<>(actions);
    }

    public DisjunctiveAction() {
        this.actions = new HashSet<>();
    }

    /**
     * Extracts all abstracts actions together with a set of values for each parameter.
     *
     * Invoked on a disjunctive action <code>{Move(a,b), Move(a,c) Pick(r, i, a)}</code>, it would return
     * <code>[ (Move, [{a}, {b,c}]), (Pick, [{r}, {i}, {a}]) ]</code>
     *
     * @param pb Problem in which the actions are defined.
     * @return All abstract actions with a list of set of values for each action.
     */
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

    /**
     * Returns true if this action contains all GroundActions present in the other one.
     * @param o
     * @return
     */
    public boolean contains(DisjunctiveAction o) {
        for(GroundAction ga : o.actions) {
            if(!actions.contains(ga))
                return false;
        }
        return true;

    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for(GroundAction a : actions) {
            builder.append(a);
            builder.append('\n');
        }
        return builder.toString();
    }
}
