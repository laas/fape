package fape.core.planning.planninggraph;

import fape.exceptions.FAPEException;
import planstack.anml.model.concrete.VarRef;
import planstack.graph.core.Edge;
import planstack.graph.printers.NodeEdgePrinter;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

class PGPrinter extends NodeEdgePrinter<PGNode, PGEdgeLabel, Edge<PGNode>> {

    final GroundProblem pb;
    final RelaxedPlanningGraph rpg;

    public PGPrinter(GroundProblem pb, RelaxedPlanningGraph rpg) {
        this.pb = pb;
        this.rpg = rpg;
    }



    Collection<String> valuesOf(Collection<VarRef> vars) {
        List<String> ret = new LinkedList<>();
        for(VarRef var : vars) {
            ret.add(pb.valueOf(var));
        }
        return ret;
    }

    @Override
    public String printNode(PGNode n) {
        if(n instanceof GroundAction) {
            GroundAction act = (GroundAction) n;
            return act.act.name() + valuesOf(act.params) + " ->"+rpg.distance(n);
        } else if(n instanceof Fluent) {
            return ((Fluent) n).f.name() + valuesOf(((Fluent) n).params).toString() + pb.valueOf(((Fluent) n).value) + " ->"+rpg.distance(n);
        } else if(n instanceof GroundState) {
            return "Init" + " ->"+rpg.distance(n);
        }
        throw new FAPEException("Unsupported PGNode: "+n);
    }

    @Override
    public String printEdge(PGEdgeLabel l) {
        return "";
    }
}