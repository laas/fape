package fr.laas.fape.structures;

import java.lang.reflect.Constructor;
import java.util.*;

public class IRStorage {

    Map<Class, Map<List<Object>, Identifiable>> instancesByParams = new HashMap<>();
    Map<Class, ArrayList<Identifiable>> instances = new HashMap<>();

    protected static Class getIdentClass(Class clazz) {
        assert clazz.getAnnotation(Ident.class) != null : clazz.toString()+" has no Ident annotation.";
        return ((Ident) clazz.getAnnotation(Ident.class)).value();
    }

    public Object get(Class clazz, List<Object> params) {
        try {
            final Class identClazz = getIdentClass(clazz);

            List<Object> paramsAndClass = new ArrayList<>(params);
            paramsAndClass.add(clazz);

            instancesByParams.putIfAbsent(identClazz, new HashMap<>());
            instances.putIfAbsent(identClazz, new ArrayList<>());

            if (instancesByParams.get(identClazz).containsKey(paramsAndClass)) {
                return instancesByParams.get(identClazz).get(paramsAndClass);
            }

            else {
                Constructor c = null;
                for(Constructor candidate : clazz.getDeclaredConstructors()) {
                    if(candidate.getAnnotationsByType(ValueConstructor.class).length > 0) {
                        if(c != null)
                            throw new RuntimeException("Two annotated constructors on class: "+clazz.getName());
                        c = candidate;
                    }
                }
                if(c == null)
                    throw new RuntimeException("No constructor annotated with @ValueConstructor in class: "+clazz.getName());
                Identifiable n = (Identifiable) c.newInstance(params.toArray());
                n.setID(instances.get(identClazz).size());
                instances.get(identClazz).add(n);
                instancesByParams.get(identClazz).put(paramsAndClass, n);
                return n;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Identifiable get(Class clazz, int id) {
        try {
            return instances.get(getIdentClass(clazz)).get(id);
        } catch (IndexOutOfBoundsException e) {
            throw new NoSuchElementException("No instance of class "+clazz.getName()+" with ID: "+id);
        }
    }

    public int getHigherID(Class clazz) {
        return instances.get(getIdentClass(clazz)).size();
    }

    public void record(Identifiable o) {
        Class identClazz = getIdentClass(o.getClass());
        instances.putIfAbsent(identClazz, new ArrayList<>(50));
        ArrayList<Identifiable> allVals = instances.get(identClazz);
        while(allVals.size() <= 1+o.getID())
            allVals.add(null);
        assert allVals.get(o.getID()) == null || allVals.get(o.getID()) == o;
        allVals.set(o.getID(), o);
    }

    public <T extends Identifiable> IntRep<T> getIntRep(Class<T> clazz) {
        final Class identClazz = getIdentClass(clazz);
        instancesByParams.putIfAbsent(identClazz, new HashMap<>());
        instances.putIfAbsent(identClazz, new ArrayList<>());

        return new IntRep<T>() {
            final ArrayList<Identifiable> values = instances.get(identClazz);
            public int asInt(T t) { return t.getID(); }

            @Override @SuppressWarnings("unchecked")
            public T fromInt(int id) { return (T) values.get(id); }

            @Override
            public boolean hasRepresentation(T t) { return true; }
        };
    }
}
