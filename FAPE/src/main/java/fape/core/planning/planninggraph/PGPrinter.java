package fape.core.planning.planninggraph;

import fape.exceptions.FAPEException;
import planstack.anml.model.concrete.VarRef;
import planstack.graph.printers.NodeEdgePrinter;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

class PGPrinter extends NodeEdgePrinter<PGNode, PGEdgeLabel> {

    final GroundProblem pb;

    public PGPrinter(GroundProblem pb) {
        this.pb = pb;
    }

    String valueOf(VarRef var) {
        for(String instance : pb.liftedPb.instances().allInstances()) {
            if(pb.liftedPb.instances().referenceOf(instance).equals(var)) {
                return instance;
            }
        }
        throw new FAPEException("Unable to find the instance referred to by "+var);
    }

    Collection<String> valuesOf(Collection<VarRef> vars) {
        List<String> ret = new LinkedList<>();
        for(VarRef var : vars) {
            ret.add(valueOf(var));
        }
        return ret;
    }

    @Override
    public String printNode(PGNode n) {
        if(n instanceof GroundAction) {
            GroundAction act = (GroundAction) n;
            return act.act.name() + valuesOf(act.params);
        } else if(n instanceof Fluent) {
            return ((Fluent) n).f.name() + valuesOf(((Fluent) n).params).toString() + valueOf(((Fluent) n).value);
        }
        throw new FAPEException("Unsupported PGNode: "+n);
    }

    @Override
    public String printEdge(PGEdgeLabel l) {
        return "";
    }
}