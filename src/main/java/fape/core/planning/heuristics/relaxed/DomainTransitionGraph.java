package fape.core.planning.heuristics.relaxed;

import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import planstack.anml.model.concrete.Action;

import java.util.Iterator;

public abstract class DomainTransitionGraph {

    private static int nextDTGID = 1;
    private static int getNextDTGID() {
        nextDTGID += 2;
        return nextDTGID -2;
    }

    public DomainTransitionGraph() {
        id = getNextDTGID();
    }

    final int id;

    public class DTNode {

        public final int lvl;
        public final int containerID;
        public final Fluent value;
        public DTNode(Fluent value) {
            this.value = value;
            this.lvl = 0;
            this.containerID = id();
        }
        public DTNode(Fluent value, int lvl) {
            this.value = value;
            this.lvl = lvl;
            containerID = id();
        }

        public boolean hasSameFluent(DTNode n) {
            if(value == null) return n.value == null;
            else if(n.value == null) return false;
            else return n.value.equals(value);
        }
        public boolean hasFluent(Fluent f) {
            assert f != null;
            if(value == null) return false;
            else return value.equals(f);
        }

        @Override public int hashCode() {
            return (value != null ? value.hashCode() : 0) + lvl;
        }
        @Override public boolean equals(Object o) {
            if(o instanceof DTNode) {
                if(((DTNode) o).lvl == lvl) {
                    if(value != null && ((DTNode) o).value != null)
                        return ((DTNode) o).value.equals(value);
                    else
                        return value == ((DTNode) o).value;
                }
            }
            return false;
        }

        @Override public String toString() {
            String base = isAccepting(this) ? "(acc) " : "";
            if(value != null) return base+value.toString()+" "+lvl;
            else return base+"null "+lvl;
        }
    }

    public class DTEdge {
        public final DTNode from;
        public final DTNode to;
        public final Action act;
        public final GAction ga;
        public DTEdge(DTNode from, DTNode to, Action act, GAction ga) {
            assert from != null;
            assert to != null;
            this.act = act;
            this.ga = ga;
            this.from = from;
            this.to = to;
        }
        @Override public String toString() {
            if(act != null) return "ag";
            if(ga != null) return "g"; //return ga.toString();
            else return "";
        }
    }

    public abstract Iterator<DTEdge> inEdges(DTNode n);

    public abstract DTNode startNodeForFluent(Fluent f);

    public final int id() { return id; }

        public DTNode baseNode(Fluent f) {
        return new DTNode(f, 0);
    }

    public abstract boolean isAccepting(DTNode n);

    public abstract DTNode possibleEntryPointFrom(DTNode n);

    /** Returns true if the cost of the edges should not be counted (already accounted for another local solution) */
    public abstract boolean areEdgesFree();
}
