package fape.core.planning.constraints;


import scala.collection.immutable.BitSet;
import scala.collection.immutable.BitSet$;

import java.util.Collection;

/**
 * Holds values for CSP.
 *
 * This implementation is immutable: it won't get any change once the constructor is called.
 * Sca immutable BitSet is used as internal representation, hence the domain
 * <code>{0, 2, 3, 7}</code> is represented by the BitSet <code>10110001</code>.
 *
 * Any modification to a ValuesHolder returns a new ValuesHolder object with an updated BitSet.
 */
public class ValuesHolder {
    BitSet values;

    ValuesHolder(Collection<Integer> values) {
        this.values = BitSet$.MODULE$.empty();
        for(int val : values) {
            this.values = this.values.$plus(val);
        }
    }

    ValuesHolder(BitSet values) {
        this.values = values;
    }

    public boolean equals(ValuesHolder o) {
        return this == o || this.values.equals(o.values);
    }

    public ValuesHolder intersect(ValuesHolder holder) {
        return new ValuesHolder((BitSet) this.values.$amp(holder.values));
    }
}