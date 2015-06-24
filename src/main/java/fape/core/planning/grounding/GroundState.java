package fape.core.planning.grounding;

import fape.core.planning.planninggraph.PGNode;

import java.util.HashSet;
import java.util.Set;

public class GroundState implements PGNode {

    public final Set<Fluent> fluents = new HashSet<>();

}
