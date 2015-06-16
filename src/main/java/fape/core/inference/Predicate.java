package fape.core.inference;

public class Predicate implements Term {
    public final String name;
    public final Object var;
    private final int hash;

    public Predicate(String name, Object var) {
        this.name = name;
        this.var = var;
        hash = name.hashCode() + 42*var.hashCode();
    }

    @Override
    public String toString() {
        return name+"("+var+")";
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Predicate)
            return ((Predicate) o).name.equals(name) && ((Predicate) o).var.equals(var);
        else
            return false;
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
