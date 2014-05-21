package fape.core.planning.stn;

import fape.core.planning.printers.Printer;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.TPRef;
import planstack.anml.model.concrete.TemporalInterval;
import planstack.graph.core.LabeledEdge;
import planstack.graph.printers.NodeEdgePrinter;

/**
 * Provides methods to print an STN: converts the node ids of an STN to a human readable string.
 *
 * It assumes that the nodes of the graph are Integers (unchecked casts).
 */
public class STNNodePrinter extends NodeEdgePrinter<Object, Object, LabeledEdge<Object, Object>> {

    private final State st;

    public STNNodePrinter(State st) {
        this.st = st;
    }

    /**
     *
     * @param node Node of the STN. It has to be an Integer.
     * @return A human readable string of the time point.
     */
    @Override
    public String printNode(Object node) {
        return Printer.stnId(st, (Integer) node);
    }

    /**
     *
     */
    @Override
    public boolean excludeNode(Object node) {
        return false;
        /* Restricts printing to the timepoints appearing as actions or problem start/end
        int stnId = (Integer) node;
        TPRef tp = Printer.correspondingTimePoint(st, stnId);
        if(tp == null)
            return false;

        if(tp == st.pb.start())
            return false;
        else if(tp == st.pb.earliestExecution())
            return false;
        else if(tp == st.pb.end())
            return false;

        TemporalInterval interval = Printer.containingInterval(st, tp);
        if(interval == null || interval instanceof Action)
            return false;

        return true;
        */
    }
}
