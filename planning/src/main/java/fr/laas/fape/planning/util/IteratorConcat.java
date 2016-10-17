package fr.laas.fape.planning.util;

import java.util.Iterator;

public class IteratorConcat<T> implements Iterator<T> {

    final Iterator<T>[] iterators;
    int cur = 0;

    @SafeVarargs
    public IteratorConcat(Iterator<T>... iterators) {
        this.iterators = iterators;
        // place cur on the first non empty iterator
        while(cur < iterators.length && !iterators[cur].hasNext()) {
            cur++;
        }
    }

    @Override
    public boolean hasNext() {
        return cur < iterators.length && iterators[cur].hasNext();
    }

    @Override
    public T next() {
        T ret = iterators[cur].next();
        while(cur < iterators.length && !iterators[cur].hasNext()) {
            cur++;
        }
        return ret;
    }
}
