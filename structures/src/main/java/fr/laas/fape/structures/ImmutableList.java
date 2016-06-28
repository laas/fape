package fr.laas.fape.structures;

import java.util.*;

@SuppressWarnings("unchecked")
/**
 * This class is simply an immutable list that caches its hashcode.
 */
public class ImmutableList<T> implements List<T> {

    private  final T[] arr;
    private int hashCode;
    public ImmutableList(Collection<T> coll) {
        arr = (T[]) new Object[coll.size()];
        int i=0;
        for(T o : coll)
            arr[i++] = o;
    }

    public ImmutableList(Collection<T> coll, T additionalElement) {
        arr = (T[]) new Object[coll.size()+1];
        int i=0;
        for(T o : coll)
            arr[i++] = o;
        arr[i] = additionalElement;
    }

    @Override
    public final int hashCode() {
        if(hashCode == 0) {
            hashCode = 1;
            for(T o : arr)
                hashCode = 31*hashCode + (o==null ? 0 : o.hashCode());
        }
        return hashCode;
    }

    @Override public final boolean equals(Object o) {
        if(!(o instanceof List))
            return false;
        List l = (List) o;
        if(l.size() != size())
            return false;
        for(int i=0 ; i<arr.length ; i++) {
            if(!(arr[i] == null ? l.get(i) == null : arr[i].equals(l.get(i))))
                return false;
        }
        return true;
    }

    @Override
    public int size() {
        return arr.length;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) { return Arrays.asList(arr).contains(o); }

    @Override public Iterator<T> iterator() { return Arrays.asList(arr).iterator(); }

    @Override
    public Object[] toArray() { return arr; }

    @Override
    public <T1> T1[] toArray(T1[] t1s) { return (T1[]) arr; }

    @Override
    public boolean add(T t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return collection.stream().allMatch(this::contains);
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean addAll(int i, Collection<? extends T> collection) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public final T get(int i) {
        return arr[i];
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
        return Arrays.asList(arr).listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int i) {
        return Arrays.asList(arr).listIterator(i);
    }

    @Override
    public List<T> subList(int i, int i1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
