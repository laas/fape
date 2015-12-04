package fr.laas.fape.structures;

import java.util.*;

@SuppressWarnings("unchecked")
public class IRList<T> implements List<T> {

    int[] list = new int[10];
    int size = 0;

    final IntRep<T> intRep;

    public IRList(IntRep<T> intRep) { this.intRep = intRep; }

    @Override
    public int size() { return size; }

    @Override
    public boolean isEmpty() { return size == 0; }

    @Override
    public boolean contains(Object o) {
        int id = intRep.asInt((T) o);
        for(int i=0 ; i<size ; i++)
            if(i == id)
                return true;
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException("Not supported yet.");
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
        if(size == list.length) {
            list = Arrays.copyOf(list, list.length * 2);
        }
        list[size++] = intRep.asInt(t);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        throw new UnsupportedOperationException("Not supported yet.");
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
    public T get(int i) {
        throw new UnsupportedOperationException("Not supported yet.");
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
    public List<T> subList(int i, int i1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
