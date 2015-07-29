package fape.core.planning.planninggraph;

import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GStateVariable;
import fape.core.planning.heuristics.relaxed.DomainTransitionGraph;
import fape.core.planning.states.State;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.concrete.InstanceRef;
import planstack.anml.model.concrete.TPRef;

import java.util.*;

public class GroundDTGs {

    private final AnmlProblem pb;

    public class DTG extends DomainTransitionGraph {
        final public GStateVariable sv;
        final public ArrayList<ArrayList<DTEdge>> revEdges = new ArrayList<>();
        final public Map<DTNode, Integer> ids = new HashMap<>();

        final public ArrayList<ArrayList<DTEdge>> unconditionalTransitions = new ArrayList<>();

        int nextID = 0;

        public DTG(GStateVariable sv) {
            this.sv = sv;

            // initialize nodes
            for(String val : pb.instances().instancesOfType(sv.f.valueType())) {
                InstanceRef instance = pb.instance(val);
                addFluent(new DTNode(new Fluent(sv, instance, false), this.id()));
//                addFluent(new DTNode(new Fluent(sv, instance, true), this.id()));
            }
            addFluent(new DTNode(null, id()));
        }

        private void addFluent(DTNode n) {
            assert revEdges.size() == nextID;
            assert !ids.containsKey(n);
            if(!ids.containsKey(n)) {
                revEdges.add(new ArrayList<DTEdge>());
                unconditionalTransitions.add(new ArrayList<DTEdge>());
                ids.put(n, nextID++);
            }
        }

        private boolean hasEdge(DTNode from, DTNode to, GAction act) {
            for(DTEdge e : revEdges.get(ids.get(to))) {
                assert e.ga != null && e.act == null;
                if(e.from.equals(from) && e.ga.equals(act)) {
                    assert e.to.equals(to);
                    return true;
                }
            }
            return false;
        }

        private List<DTEdge> edges = null;

        public Collection<DTEdge> edges() {
            if(edges == null) {
                edges = new LinkedList<>();
                for(List<DTEdge> es : revEdges) {
                    for(DTEdge e : es) {
                        edges.add(e);
                    }
                }
                for(List<DTEdge> es : unconditionalTransitions) {
                    for(DTEdge e : es) {
                        edges.add(e);
                    }
                }
            }
            return edges;
        }

        private void addEdge(DTNode from, DTNode to, GAction act) {
            assert to != null && from != null;
            assert ids.containsKey(to) && ids.containsKey(from) : "A node is missing in the DTG (probably missed during initialization).";
            assert from.value == null || to.value == null || from.value.sv.equals(to.value.sv);
            if(!hasEdge(from, to, act))
                revEdges.get(ids.get(to)).add(new DTEdge(from, to, null, act));
        }

        public void addEdge(Fluent from, Fluent to, GAction act) {
            addEdge(new DTNode(from, id()), new DTNode(to, id()), act);
        }

        @Override public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(sv);
            for(Map.Entry<DTNode,Integer> entry : ids.entrySet()) {
                sb.append("\n  ");
                sb.append(entry.getKey());
                for(DTEdge e : revEdges.get(entry.getValue())) {
                    sb.append("\n    ");
                    sb.append(e);
                }
            }
            return sb.toString();
        }

        @Override
        public Iterator<DTEdge> inEdges(DTNode n) {
            assert ids.containsKey(n);
            return revEdges.get(ids.get(n)).iterator();
        }

        @Override
        public Iterator<DTEdge> outEdges(DTNode n) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public DTNode startNodeForFluent(Fluent f) {
            for(DTNode n : ids.keySet()) {
                if(n.hasFluent(f))
                    return n;
            }
            return null;
        }

        @Override
        public boolean isAccepting(DTNode n) {
            return n.value == null;
        }

        @Override
        public DTNode possibleEntryPointFrom(DTNode n) {
            return null; // static DTG do not accept any transition
        }

        @Override
        public Collection<DTNode> getAllNodes() {
            return ids.keySet();
        }

        @Override
        public Collection<DTNode> unifiableNodes(Fluent f, TPRef start, TPRef end, State st) {
            List<DTNode> mergeableNodes = new LinkedList<>();
            for(DTNode cur : ids.keySet()) {
                if(cur.canSupportValue(f, start, end, st))
                    mergeableNodes.add(cur);
            }
            return mergeableNodes;
        }

        @Override
        public boolean isFree(DTEdge e) {
            return false;
        }
    }

    Map<GStateVariable, DTG> dtgs = new HashMap<>();

    public GroundDTGs(Collection<GAction> actions, AnmlProblem pb) {
        this.pb = pb;
        for(GAction ga : actions) {

            for(Fluent effect : ga.add) {
                boolean added = false;
                if(!dtgs.containsKey(effect.sv))
                    dtgs.put(effect.sv, new DTG(effect.sv));
                for(Fluent precondition : ga.pre) {
                    if(effect.sv.equals(precondition.sv)) {
//                        assert !added : "A transition has two preconditions";

                        added = true;
                        dtgs.get(effect.sv).addEdge(precondition, effect, ga);
                    }
                }
                if(!added) // this is an unconditional transition
                    dtgs.get(effect.sv).addEdge(null, effect, ga);
            }
        }
    }

    public boolean hasDTGFor(GStateVariable sv) {
        return dtgs.containsKey(sv);
    }

    public DTG getDTGOf(GStateVariable sv) {
        if(!dtgs.containsKey(sv)) {
            // the dtg was not initialized because no actions have an effect on it
            DTG newDTG = new DTG(sv);
            dtgs.put(sv, newDTG);
        }
        return dtgs.get(sv);
    }

    public void print() {
        for(DTG dtg : dtgs.values()) {
            System.out.println(dtg+"\n");
        }
    }
}
