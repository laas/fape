package fr.laas.fape.structures;

public interface IntRep<T> {

    int asInt(T t);
    T fromInt(int id);
    boolean hasRepresentation(T t);

}
