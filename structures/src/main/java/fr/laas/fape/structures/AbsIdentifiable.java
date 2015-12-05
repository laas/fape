package fr.laas.fape.structures;

public abstract class AbsIdentifiable implements Identifiable {

    private int id = -1;

    @Override public int getID() { return id; }
    @Override public void setID(int id) { this.id = id; }

    @Override final public int hashCode() { assert getID() >= 0; return getID(); }
    @Override final public boolean equals(Object o) {
        return this.getClass() == o.getClass() &&
                getID() == ((Identifiable) o).getID();
    }
}
