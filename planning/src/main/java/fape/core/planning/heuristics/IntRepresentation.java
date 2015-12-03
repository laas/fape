package fape.core.planning.heuristics;

public interface IntRepresentation<T> {

    int asInt(T t);
    T fromInt(int id);
    boolean hasRepresentation(T t);

}
