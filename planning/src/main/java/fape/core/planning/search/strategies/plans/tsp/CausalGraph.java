package fape.core.planning.search.strategies.plans.tsp;

import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GStateVariable;
import fape.core.planning.planner.APlanner;

import static fape.core.planning.grounding.GAction.*;

import java.util.*;
import java.util.stream.Collectors;

public class CausalGraph {

    Map<GStateVariable, Set<GStateVariable>> outEdges = new HashMap<>();
    Map<GStateVariable, Set<GStateVariable>> inEdges = new HashMap<>();

    public void addEdge(GStateVariable from, GStateVariable to) {
        outEdges.putIfAbsent(from, new HashSet<>());
        inEdges.putIfAbsent(from, new HashSet<>());
        outEdges.putIfAbsent(to, new HashSet<>());
        inEdges.putIfAbsent(to, new HashSet<>());
        outEdges.get(from).add(to);
        inEdges.get(to).add(from);
    }

    private static class SCCStruct {
        public int c = 0;
        public Stack<GStateVariable> s = new Stack<>();
        public Stack<GStateVariable> p = new Stack<>();
        Map<GStateVariable, Integer> preorders = new HashMap<>();
        Map<GStateVariable, Integer> compponentIDs = new HashMap<>();
        int nextComponent = 0;
        List<Set<GStateVariable>> componentsList = new LinkedList<>();
    }

    private void recSCC(GStateVariable v, SCCStruct data) {
        data.preorders.put(v, data.c++);
        data.s.push(v);
        data.p.push(v);
        for(GStateVariable w : outEdges.get(v)) {
            if(!data.preorders.containsKey(w)) {
                recSCC(w, data);
            } else if(!data.compponentIDs.containsKey(w)) {
                while(data.preorders.get(data.p.peek()) > data.preorders.get(w))
                    data.p.pop();
            }
        }
        if(data.p.peek() == v) {
            Set<GStateVariable> comp = new HashSet<>();
            GStateVariable cur;
            do {
                cur = data.s.pop();
                data.compponentIDs.put(cur, data.nextComponent);
                comp.add(cur);
            } while(cur != v);
            data.nextComponent++;
            data.componentsList.add(comp);
            data.p.pop();
        }
    }

    public List<Set<GStateVariable>> getStronglyConnectedComponents() {
        SCCStruct data = new SCCStruct();

        for(GStateVariable v : outEdges.keySet()) {
            if(!data.preorders.containsKey(v))
                recSCC(v, data);
        }

        return data.componentsList;
    }

    public static CausalGraph getCausalGraph(APlanner planner) {
        CausalGraph cg = new CausalGraph();
        for(GAction ga : planner.preprocessor.getAllActions()) {
            for(GLogStatement s1 : ga.gStatements.stream().map(p -> p.value2).collect(Collectors.toList())) {
                if(s1 instanceof GPersistence)
                    continue;
                for(GLogStatement s2 : ga.gStatements.stream().map(p -> p.value2).collect(Collectors.toList())) {
                    if(s1.sv.equals(s2.sv))
                        continue;
                    cg.addEdge(s2.sv, s1.sv);
                }
            }
        }
        return cg;
    }
}
