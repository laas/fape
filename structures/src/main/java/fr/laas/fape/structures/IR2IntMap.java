package fr.laas.fape.structures;

import java.security.InvalidParameterException;
import java.util.*;
import java.util.stream.Collectors;

public class IR2IntMap<K> implements Map<K, Integer> {
    private static int NIL = Integer.MIN_VALUE;

    int numElem = 0;
    int[] values;
    final IntRep<K> keyRep;

    public IR2IntMap(IntRep<K> keyRep) {
        this.keyRep = keyRep;
        values = new int[10];
        Arrays.fill(values, NIL);
    }

    public IR2IntMap(IR2IntMap<K> toClone) {
        this.numElem = toClone.numElem;
        this.values = Arrays.copyOf(toClone.values, toClone.values.length);
        this.keyRep = toClone.keyRep;
    }

    @Override
    public int size() {
        return numElem;
    }

    @Override
    public boolean isEmpty() {
        return numElem == 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean containsKey(Object o) {
        if(values.length <= keyRep.asInt((K) o))
            return false;
        else
            return values[keyRep.asInt((K) o)] != NIL;
    }

    public boolean containsKey(int k) {
        if(values.length <= k)
            return false;
        else
            return values[k] != NIL;
    }

    @Override
    public boolean containsValue(Object o) {
        if (o == null || NIL == ((int) o)) throw new InvalidParameterException("This map uses "+NIL+" to represent the absence of value");
        for (int i = 0; i < values.length; i++)
            if (values[i] == (int) o)
                return true;
        return false;
    }

    public boolean containsValue(int val) {
        for (int i = 0; i < values.length; i++)
            if (values[i] == val)
                return true;
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Integer get(Object o) {
        if (!(o instanceof Identifiable)) throw new InvalidParameterException("Parameter is not identifiable: " + o);
        if (!containsKey(o)) throw new NoSuchElementException();
        return values[keyRep.asInt((K) o)];
    }

    @SuppressWarnings("unchecked")
    public int get(int k) {
        if (!containsKey(k)) throw new NoSuchElementException();
        return values[k];
    }

    @Override
    public Integer getOrDefault(Object o, Integer n) {
        return containsKey(o) ? get(o) : n;
    }

    /** Make sure the values table is big enough for a key with the given index */
    private void ensureSpace(int index) {
        if(index >= values.length) {
            int oldSize = values.length;
            int newSize = index+1 > values.length * 2 ? index+1 : values.length * 2;
            values = Arrays.copyOf(values, newSize);
            Arrays.fill(values, oldSize, newSize, NIL);
        }
    }

    @Override
    public Integer put(K k, Integer v) {
        if (v == null || NIL == (v)) throw new InvalidParameterException("This map uses "+NIL+" to represent the absence of value");
        if (!containsKey(k)) {
            ensureSpace(keyRep.asInt(k));
            numElem += 1;
        }

        values[keyRep.asInt(k)] = v;
        return v;
    }

    public Integer put(int k, int v) {
        if(v == NIL) throw new InvalidParameterException("This map uses "+NIL+" to represent the absence of value");
        if (!containsKey(k)) {
            ensureSpace(k);
            numElem += 1;
        }
        values[k] = v;
        return v;
    }

    @Override public Integer putIfAbsent(K k, Integer v) {
        if (v == null || NIL == v) throw new InvalidParameterException("This map uses "+NIL+" to represent the absence of value");
        if(!containsKey(k))
            return put(k, v);
        else
            return get(k);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Integer remove(Object o) {
        if (!(o instanceof Identifiable)) throw new InvalidParameterException("Parameter is not identifiable: " + o);
        if (!containsKey(o)) return null; //throw new NoSuchElementException();
        Integer val = values[keyRep.asInt((K) o)];
        values[keyRep.asInt((K) o)] = NIL;
        numElem--;
        return val;
    }

    @SuppressWarnings("unchecked")
    public int remove(int k) {
        if (!containsKey(k)) throw new NoSuchElementException();
        int val = values[k];
        values[k] = NIL;
        numElem--;
        return val;
    }

    @Override
    public void putAll(Map<? extends K, ? extends Integer> map) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clear() {
        values = new int[10];
        numElem = 0;
    }

    @Override
    public IR2IntMap<K> clone() {
        return new IR2IntMap<K>(this);
    }

    @Override // this might be very slow
    @SuppressWarnings("unchecked")
    public Set<K> keySet() {
        HashSet<K> keys = new HashSet<>();
        for (int i = 0; i < values.length; i++)
            if (values[i] != NIL)
                keys.add(keyRep.fromInt(i));
        return keys;
    }

    public Iterable<K> keys() {
        return () -> new Iterator<K>() {
            int cur = 0;
            public boolean hasNext() {
                while(cur < values.length && values[cur] == NIL) cur++;
                return cur < values.length;
            }

            @Override
            public K next() {
                return keyRep.fromInt(cur++);
            }
        };
    }

    public PrimitiveIterator.OfInt keysIterator() {
        return new PrimitiveIterator.OfInt() {
            int i = 0;
            @Override
            public int nextInt() {
                while(i<values.length && !containsKey(i))
                    i++;
                return i++;
            }

            @Override
            public boolean hasNext() {
                while(i<values.length && !containsKey(i))
                    i++;
                return i<values.length;
            }
        };
    }

    public Collection<Integer> values() {
        List<Integer> vals = new ArrayList<>();
        for (int val : values)
            if (val != NIL)
                vals.add(val);
        return vals;
    }

    @Override
    public Set<Entry<K, Integer>> entrySet() {
        return keySet().stream().map(k -> new AbstractMap.SimpleEntry<K,Integer>(k, get(k))).collect(Collectors.toSet());
    }
}

