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

    static boolean isInfty(int num) { return num > 99999; }

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
        public final TempFluent.Fluent fluent;
        public final ActionNode act;
        public final int delay;
    }
    @Value class MinEdge {
        public final ActionNode act;
        public final TempFluent.Fluent fluent;
        public final int delay;
    }

    Iterator<MaxEdge> inEdges(ActionNode n);
    Iterator<MinEdge> outEdges(ActionNode n);
    Iterator<MinEdge> inEdges(TempFluent.Fluent f);
    Iterator<MaxEdge> outEdges(TempFluent.Fluent f);
}
