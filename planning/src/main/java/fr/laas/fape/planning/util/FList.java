package fr.laas.fape.planning.util;

import scala.collection.JavaConversions;
import scala.collection.immutable.List$;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * This container implements a Java List by using an immutable List (from scala libraries) as the main
 * container.
 *
 * The only allowed modification is to prepend an element with the method add.
 * Copying the list takes O(1) both in time and memory.
 * @param <T>
 */
public class FList<T> implements List<T> {

    scala.collection.immutable.List<T> l;

    public FList() {
        l = List$.MODULE$.empty();
    }

    public FList(FList<T> toCopy) {
        l = toCopy.l;
    }

    @Override
    public int size() {
        return l.size();
    }

    @Override
    public boolean isEmpty() {
        return l.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return l.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return JavaConversions.asJavaIterator(l.iterator());
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T1> T1[] toArray(T1[] t1s) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean add(T t) {
        l = l.$colon$colon(t);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean containsAll(Collection<?> objects) {
        for(Object o : objects)
            if(!l.contains(o))
                return false;
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> ts) {
        for(T t : ts)
            add(t);
        return true;
    }

    @Override
    public boolean addAll(int i, Collection<? extends T> ts) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean removeAll(Collection<?> objects) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean retainAll(Collection<?> objects) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean equals(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int hashCode() {
        return l.hashCode();
    }

    @Override
    public T get(int i) {
        return l.apply(i);
    }

    @Override
    public T set(int i, T t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void add(int i, T t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public T remove(int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int indexOf(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ListIterator<T> listIterator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ListIterator<T> listIterator(int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<T> subList(int i, int i2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
