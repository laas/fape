package fr.laas.fape.planning.core.planning.search.strategies.plans.tsp;

import fr.laas.fape.planning.core.planning.grounding.GAction;
import fr.laas.fape.planning.core.planning.grounding.GStateVariable;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.util.Counter;

import java.util.*;
import java.util.stream.Collectors;

public class CausalGraph {

    Map<GStateVariable, Map<GStateVariable, Integer>> outEdges = new HashMap<>();
    Map<GStateVariable, Map<GStateVariable, Integer>> inEdges = new HashMap<>();

    public Iterable<GStateVariable> getStateVariables() {
        return inEdges.keySet();
    }

    public void addEdge(GStateVariable from, GStateVariable to) {
        outEdges.putIfAbsent(from, new HashMap<>());
        inEdges.putIfAbsent(from, new HashMap<>());
        outEdges.putIfAbsent(to, new HashMap<>());
        inEdges.putIfAbsent(to, new HashMap<>());

        outEdges.get(from).put(to, outEdges.get(from).getOrDefault(to, 0));
        inEdges.get(to).put(from, inEdges.get(to).getOrDefault(from, 0));
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
        for(GStateVariable w : outEdges.get(v).keySet()) {
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

    public void makeAcyclic() {
        for(Set<GStateVariable> scc : getStronglyConnectedComponents()) {
            while (scc.size() > 1) {
                int minInWeight = Integer.MAX_VALUE;
                GStateVariable toRemove = null;
                for (GStateVariable sv : scc) {
                    int weight = inEdges.get(sv).values().stream().mapToInt(Integer::intValue).sum(); // TODO: this might be limited to edges strictly in the SCC
                    if(weight < minInWeight) {
                        minInWeight = weight;
                        toRemove = sv;
                    }
                }
                scc.remove(toRemove);
                for(GStateVariable remaining : scc) {
                    inEdges.get(toRemove).remove(remaining);
                    outEdges.get(remaining).remove(toRemove);
                }
            }
        }
    }

    public Map<GStateVariable, Integer> getTopologicalLevels() {
        HashMap<GStateVariable,Counter> numInEdges = new HashMap<>();
        HashMap<GStateVariable,Integer> topoLevel = new HashMap<>();
        Stack<GStateVariable> nextLevel = new Stack<>();
        int levelCounter = 0;

        for(GStateVariable sv : getStateVariables()) {
            numInEdges.put(sv, new Counter(inEdges.get(sv).size()));
            if(numInEdges.get(sv).getValue() == 0)
                nextLevel.add(sv);
        }

        Stack<GStateVariable> currentLevel = nextLevel;
        nextLevel = new Stack<>();
        while(!currentLevel.isEmpty()) {
            GStateVariable sv = currentLevel.pop();
            topoLevel.put(sv, levelCounter);

            for(GStateVariable child : outEdges.get(sv).keySet()) {
                numInEdges.get(child).decrement();
                if(numInEdges.get(child).getValue() == 0)
                    nextLevel.push(child);
            }

            if(currentLevel.isEmpty()) {
                currentLevel = nextLevel;
                nextLevel = new Stack<>();
                levelCounter++;
            }
        }
        assert numInEdges.values().stream().allMatch(x -> x.getValue() == 0);
        return topoLevel;
    }

    public static CausalGraph getCausalGraph(Planner planner) {
        CausalGraph cg = new CausalGraph();
        for(GAction ga : planner.preprocessor.getAllActions()) {
            for(GAction.GLogStatement s1 : ga.gStatements.stream().map(p -> p.value2).collect(Collectors.toList())) {
                if(!s1.isChange())
                    continue;
                for(GAction.GLogStatement s2 : ga.gStatements.stream().map(p -> p.value2).collect(Collectors.toList())) {
                    if(s1.sv.equals(s2.sv))
                        continue;
                    cg.addEdge(s2.sv, s1.sv);
                }
            }
        }
        return cg;
    }


}
