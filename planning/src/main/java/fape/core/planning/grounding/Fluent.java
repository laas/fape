package fape.core.planning.grounding;

import fape.core.inference.Term;
import planstack.anml.model.concrete.InstanceRef;


public final class Fluent implements Term {
    final public GStateVariable sv;
    final public InstanceRef value;
    final public int ID;

    /** Creates a new Fluent representing the state variable sv with value 'value".
     *
     *  This constructor should only be invoked by the Preprocessor.
     *  If you need a new FLuent, you should invoke Preprocessor.getFluent(...)
     */
    public Fluent(final GStateVariable sv, final InstanceRef value, final int ID) {
        this.sv = sv;
        this.value = value;
        this.ID = ID;
    }

    @Override
    public String toString() {
        return sv +"=" + value;
    }
}
