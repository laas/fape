package fape.core.inference;

public class Predicate implements Term {
    public final String name;
    public final Object var;

    public Predicate(String name, Object var) {
        this.name = name;
        this.var = var;
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
        return name.hashCode() + 42*var.hashCode();
    }
}
