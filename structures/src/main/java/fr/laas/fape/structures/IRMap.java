package fr.laas.fape.structures;

import java.security.InvalidParameterException;
import java.util.*;

public class IRMap<K, V> implements Map<K,V> {

    int numElem = 0;
    Object[] values = new Object[10];
    final IntRep<K> keyRep;

    public IRMap(IntRep<K> keyRep) { this.keyRep = keyRep; }

    @Override public int size() { return numElem; }

    @Override public boolean isEmpty() { return numElem == 0; }

    @Override @SuppressWarnings("unchecked")
    public boolean containsKey(Object o) {
        if(values.length >= keyRep.asInt((K) o))
            return false;
        else
            return values[keyRep.asInt((K) o)] != null;
    }

    public boolean containsKey(int k) {
        if(values.length >= k)
            return false;
        else
            return values[k] != null;
    }

    @Override
    public boolean containsValue(Object o) {
        for(int i=0 ; i<values.length ; i++)
            if(values[i].equals(o))
                return true;
        return false;
    }

    @Override @SuppressWarnings("unchecked")
    public V get(Object o) {
        if(!(o instanceof Identifiable)) throw new InvalidParameterException("Parameter is not identifiable: "+o);
        if(!containsKey(o)) throw new NoSuchElementException();
        return (V) values[keyRep.asInt((K) o)];
    }

    @SuppressWarnings("unchecked")
    public V get(int k) {
        if(!containsKey(k)) throw new NoSuchElementException();
        return (V) values[k];
    }

    @Override
    public V put(K k, V v) {
        if(v == null) throw new InvalidParameterException("This map does not allow null values");
        if(!containsKey(k)) {
            if(keyRep.asInt(k) >= values.length)
                values = Arrays.copyOf(values, values.length * 2);
            numElem += 1;
        }

        values[keyRep.asInt(k)] = v;
        return v;
    }

    public V put(int k, V v) {
        if(!containsKey(k)) {
            if(k >= values.length)
                values = Arrays.copyOf(values, values.length*2);
            numElem += 1;
        }
        values[k] = v;
        return v;
    }

    @Override @SuppressWarnings("unchecked")
    public V remove(Object o) {
        if(!(o instanceof Identifiable)) throw new InvalidParameterException("Parameter is not identifiable: "+o);
        if(!containsKey(o)) throw new NoSuchElementException();
        V val = (V) values[keyRep.asInt((K) o)];
        values[keyRep.asInt((K) o)] = null;
        return val;
    }

    @SuppressWarnings("unchecked")
    public V remove(int k) {
        if(!containsKey(k)) throw new NoSuchElementException();
        V val = (V) values[k];
        values[k] = null;
        return val;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clear() {
        values = new Object[10];
        numElem = 0;
    }

    @Override @SuppressWarnings("unchecked")
    public Set<K> keySet() {
        HashSet<K> keys = new HashSet<K>();
        for(int i=0 ; i<values.length ; i++)
            if(values[i] != null)
                keys.add((K) keyRep.fromInt(i));
        return keys;
    }

    @Override @SuppressWarnings("unchecked")
    public Collection<V> values() {
        List<V> vals = new ArrayList<>();
        for(Object val : values)
            if(val != null)
                vals.add((V) vals);
        return vals;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
