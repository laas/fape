package planstack.constraints.bindings;


import scala.collection.JavaConversions;
import scala.collection.immutable.BitSet;
import scala.collection.immutable.BitSet$;

import java.util.Collection;
import java.util.Set;

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

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public boolean equals(ValuesHolder o) {
        return this == o || this.values.equals(o.values);
    }

    public ValuesHolder intersect(ValuesHolder holder) {
        return new ValuesHolder((BitSet) this.values.$amp(holder.values));
    }

    /**
     * @return True if the at least one of the given values is present in this domain.
     */
    public boolean containsAtLeastOne(Collection<Integer> possibleValues) {
        for(int val : possibleValues) {
            if(values.contains(val))
                return true;
        }
        return false;
    }

    public Set<Integer> values() {
        return (Set) JavaConversions.setAsJavaSet(values);
    }

    public int size() {
        return values.size();
    }

    public ValuesHolder remove(int value) {
        return new ValuesHolder(values.$minus(value));
    }

    public ValuesHolder remove(ValuesHolder toRemove) {
        return new ValuesHolder((BitSet) values.$amp$tilde(toRemove.values));
    }

    public boolean contains(int value) {
        return values.contains(value);
    }
}