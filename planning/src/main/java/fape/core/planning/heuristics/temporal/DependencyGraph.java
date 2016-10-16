package fape.core.planning.heuristics.temporal;

import fr.laas.fape.structures.AbsIdentifiable;
import fr.laas.fape.structures.Ident;
import fr.laas.fape.structures.ValueConstructor;
import lombok.Getter;
import lombok.Value;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public interface DependencyGraph {

    static boolean isInfty(int num) { return num > 9999999; }

    @Ident(Node.class)
    abstract class Node extends AbsIdentifiable {}
    abstract class ActionNode extends Node {
        public abstract List<TempFluent> getConditions();
        public abstract List<TempFluent> getEffects();
    }

    @Getter
    @Ident(Node.class)
    class FactAction extends ActionNode {
        public final List<TempFluent> effects;

        @ValueConstructor
        @Deprecated
        public FactAction(List<TempFluent> effects) { this.effects = effects; }

        @Override public List<TempFluent> getConditions() { return new ArrayList<>(); }
    }

    @Value
    class MaxEdge {
        public final TempFluent.DGFluent fluent;
        public final ActionNode act;
        public final int delay;
    }
    @Value class MinEdge {
        public final ActionNode act;
        public final TempFluent.DGFluent fluent;
        public final int delay;
    }

    Iterator<MaxEdge> inEdgesIt(ActionNode n);
    Iterator<MinEdge> outEdgesIt(ActionNode n);
    Iterator<MinEdge> inEdgesIt(TempFluent.DGFluent f);
    Iterator<MaxEdge> outEdgesIt(TempFluent.DGFluent f);

    default Iterable<MaxEdge> inEdges(ActionNode n) { return () -> inEdgesIt(n); }
    default Iterable<MinEdge> outEdges(ActionNode n) { return () -> outEdgesIt(n); }
    default Iterable<MinEdge> inEdges(TempFluent.DGFluent f) { return () -> inEdgesIt(f); }
    default Iterable<MaxEdge> outEdges(TempFluent.DGFluent f) { return () -> outEdgesIt(f); }
}
