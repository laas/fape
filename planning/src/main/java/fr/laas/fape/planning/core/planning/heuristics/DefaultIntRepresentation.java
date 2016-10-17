package fr.laas.fape.planning.core.planning.heuristics;

import java.util.ArrayList;
import java.util.HashMap;

public class DefaultIntRepresentation<T> implements IntRepresentation<T> {

    private HashMap<T, Integer> ids = new HashMap<>();
    private ArrayList<T> objs = new ArrayList<>();

    @Override
    public final int asInt(T t) {
        if(!ids.containsKey(t)) {
            final int id = objs.size();
            ids.put(t, id);
            objs.add(t);
        }
        return ids.get(t);
    }

    @Override
    public final T fromInt(int id) {
        assert id < objs.size() : "No object recorded with this ID: "+id;
        return objs.get(id);
    }

    @Override
    public final boolean hasRepresentation(T t) {
        return ids.containsKey(t);
    }
}
