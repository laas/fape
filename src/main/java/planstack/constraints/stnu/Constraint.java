package planstack.constraints.stnu;

public class Constraint<TPRef> {
    public final TPRef a, b;
    public final int weight;

    public Constraint(TPRef a, TPRef b, int weight) {
        this.a = a;
        this.b = b;
        this.weight = weight;
    }
}