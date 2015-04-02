package fape.core.planning.planninggraph;

import java.util.HashSet;
import java.util.Set;

public class GroundState implements PGNode {

    public final Set<Fluent> fluents = new HashSet<>();

}
