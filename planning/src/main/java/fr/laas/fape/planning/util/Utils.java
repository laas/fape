package fr.laas.fape.planning.util;

import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaw;
import fr.laas.fape.planning.core.planning.states.Printer;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.exceptions.FAPEException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Utils {

    private static long lastTick = 0;
    public static void tick() { lastTick = System.currentTimeMillis(); }
    public static void printAndTick(String msg) {
        long tmp = lastTick;
        tick();
        System.out.println(msg + " ["+(lastTick-tmp)+"]");
    }

    public static boolean eq(Object o1, Object o2) {
        if(o1 == null) return o2 == null;
        else if(o2 == null) return o1 == null;
        else return o1.equals(o2);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> asImmutableList(Collection<T> coll) {
        return Arrays.asList(coll.toArray((T[]) new Object[coll.size()]));
    }

    /**
     * This method tries to find an example of an inconsistent comparison function for flaws.
     * It will exit if a problematic example is found and throw a FAPEException when no example was found.
     *
     * @param flaws List of flaws to sort
     * @param comp Problematic comparator.
     * @param st State In which the problem arise
     * @param planner Planner in which the problem occured
     */
    public static void showExampleProblemWithFlawComparator(List<Flaw> flaws, Comparator<Flaw> comp, State st, Planner planner) {
        for(int i=0 ; i< flaws.size() ; i++) {
            Flaw a = flaws.get(i);
            for(int j=i+1 ; j<flaws.size() ; j++) {
                Flaw b = flaws.get(j);
                for(int k=j+1 ; k<flaws.size() ; k++) {
                    Flaw c = flaws.get(k);
                    int ab = comp.compare(a, b);
                    int ac = comp.compare(a, c);
                    int bc = comp.compare(b, c);
                    if(ab < 0 && bc < 0 || ab < 0 && bc == 0 || ab < 0 && bc == 0) {
                        if(ac >= 0) {
                            System.err.println("Problem with flaw comparison function, it is not transitive: ");
                            System.err.println(" a: num-resolvers="+a.getNumResolvers(st, planner) +" -- flaw: "+ Printer.p(st, a));
                            System.err.println(" b: num-resolvers="+b.getNumResolvers(st, planner) +" -- flaw: "+ Printer.p(st, b));
                            System.err.println(" c: num-resolvers="+c.getNumResolvers(st, planner) +" -- flaw: "+ Printer.p(st, c));
                            System.err.printf("comp(a,b)=%d comp(b,c)=%d comp(a,c)=%d\n", ab, bc, ac);
                            System.exit(1);
                        }
                    }

                }
            }
        }
        throw new FAPEException("Unknown problem with flaw comparison function");
    }

    public static final int[] copyIntoBigger(int[] array, int newSize, int defaultValue) {
        assert newSize > array.length;
        int[] bigger = Arrays.copyOf(array, newSize);
        Arrays.fill(bigger, array.length, newSize, defaultValue);
        return bigger;
    }

    /** Reads an integer from standard input. Default to 0 if an empty line is given */
    public static int readInt() {
        String line = null;
        int val = 0;
        try {
            BufferedReader is = new BufferedReader(
                    new InputStreamReader(System.in));
            line = is.readLine();
            if(line.equals(""))
                val = 0;
            else
                val = Integer.parseInt(line);
        } catch (NumberFormatException ex) {
            System.err.println("Not a valid number: " + line);
            return readInt();
        } catch (IOException e) {
            System.err.println("Unexpected IO ERROR: " + e);
        }
        return val;
    }

    /**
     * Returns true if at least one element is present in both collection (ie. if they
     * have a non empty intersection).
     */
    public static <T> boolean nonEmptyIntersection(Collection<T> as, Collection<T> bs) {
        Collection<T> inter = new HashSet<>(as);
        inter.retainAll(bs);
        return !inter.isEmpty();
    }

    /**
     * Prints all items in a collection to a string, separated by a custom separator.
     * @param coll Set of items to be printed
     * @param sep The separator to add between each elements.
     * @param <T>
     * @return A string representation of the collection.
     */
    public static <T> String print(Iterable<T> coll, String sep) {
        StringBuilder builder = new StringBuilder();
        Iterator<T> it = coll.iterator();
        while(it.hasNext()) {
            T item = it.next();
            builder.append(item.toString());
            if(it.hasNext())
                builder.append(sep);
        }
        return builder.toString();
    }

    /**
     * Prints all items in an array to a string, separated by a custom separator.
     * @param coll Set of items to be printed
     * @param sep The separator to add between each elements.
     * @param <T>
     * @return A string representation of the collection.
     */
    public static <T> String print(T[] coll, String sep) {
        StringBuilder builder = new StringBuilder();
        for(int i=0 ; i<coll.length ; i++) {
            builder.append(coll[i]);
            if(i<coll.length-1)
                builder.append(sep);
        }
        return builder.toString();
    }

    /**
     * [[a1, a2], [b], [c1, C2]]
     *  => [ [a1, b, c1], [a1, b, c2], [a2, b, c1], [a2, b, c2]]
     * @param valuesSets
     * @return
     */
    public static <T> List<List<T>> allCombinations(List<List<T>> valuesSets) {
        return allCombinations(valuesSets, 0, new LinkedList<T>());
    }

    private static <T> List<List<T>> allCombinations(List<List<T>> valuesSets, int startWith, List<T> baseValues) {
        List<List<T>> ret = new LinkedList<>();

        if(startWith >= valuesSets.size()) {
            ret.add(baseValues);
            return ret;
        }
        for(T val : valuesSets.get(startWith)) {
            List<T> newBaseValues = new LinkedList<>(baseValues);
            newBaseValues.add(val);
            ret.addAll(allCombinations(valuesSets, startWith+1, newBaseValues));
        }

        return ret;
    }

}
