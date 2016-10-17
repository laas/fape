package fr.laas.fape.planning.util;

public class Counter {
    private int value;

    public Counter(int var1) {
        this.setValue(var1);
    }

    public int hashCode() {
        return this.getValue();
    }

    public boolean equals(Object var1) {
        return var1 instanceof Counter && ((Counter)var1).getValue() == this.getValue();
    }

    public void setValue(int var1) {
        this.value = var1;
    }

    public void increment() {
        this.value++;
    }

    public void decrement() {
        this.value--;
    }

    public int getValue() {
        return this.value;
    }
}