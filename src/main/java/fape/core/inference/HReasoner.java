package fape.core.inference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class HReasoner {
    private static final int baseNumVars = 100;
    private static final int baseNumClause = 1000;

    private final HashMap<Object,Integer> varsIds;
    private Object[] vars;
    private final Reasoner res;

    private int numVar = 0;


    public HReasoner() {
        vars = new Object[baseNumVars];
        varsIds = new HashMap<>();
        res = new Reasoner(baseNumVars, baseNumClause);
    }

    public void addHornClause(Object left, Collection<Object> right) {
        addHornClause(left, right.toArray());
    }

    public void addHornClause(Object left, Object... right) {
        if(!varsIds.containsKey(left))
            addVar(left);
        int[] rightIds = new int[right.length];
        for(int i=0 ; i<right.length ; i++) {
            if (!varsIds.containsKey(right[i]))
                addVar(right[i]);
            rightIds[i] = id(right[i]);
        }

        res.addHornClause(id(left), rightIds);
    }

    public void set(Object o) {
        if(!varsIds.containsKey(o))
            addVar(o);
        res.set(id(o));
    }

    public Collection<Object> trueFacts() {
        List<Object> facts = new ArrayList<>();
        for(int var=0 ; var<numVar ; var++) {
            if(res.varsStatus[var]) {
                facts.add(vars[var]);
            }
        }
        return facts;
    }

    private int id(Object o) {
        return varsIds.get(o);
    }

    private void addVar(Object o) {
        assert !varsIds.containsKey(o) : "Var already registered";
        int id = numVar++;
        vars[id] = o;
        varsIds.put(o, id);
        res.addVar(id);
    }


    public static void main(String[] args) {
        HReasoner res = new HReasoner();
        res.addHornClause("A", "a", "b");
        res.addHornClause("c", "A");
        res.addHornClause("B", "c", "b");
        res.addHornClause("d", "B");

        System.out.println(res.trueFacts());
        res.set("c");
        System.out.println(res.trueFacts());
        res.set("b");
        System.out.println(res.trueFacts());
        res.set("e");
    }
}
