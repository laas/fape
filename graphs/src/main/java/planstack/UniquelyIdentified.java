package planstack;

public interface UniquelyIdentified {

    /**
     * An integer that gies a unique identifier to this object.
     * This id will then be used for primitive int to X hash maps
     */
    int id();
}
