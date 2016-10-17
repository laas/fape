package fr.laas.fape.planning.util;

import java.lang.ref.WeakReference;

public class StrongReference<T> extends WeakReference<T> {
    private final T referent;

    public StrongReference(T referent) {
        super(null);
        this.referent = referent;
    }

    @Override
    public T get() {
        return referent;
    }
}