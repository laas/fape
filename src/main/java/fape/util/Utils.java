package fape.util;

import fape.core.planning.planner.APlanner;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import fape.exceptions.FAPEException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Utils {

    /**
     * This method tries to find an example of an inconsistent comparison function for flaws.
     * It will exit if a problematic example is found and throw a FAPEException when no example was found.
     * 
     * @param flaws List of flaws to sort
     * @param comp Problematic comparator.
     * @param st State In which the problem arise
     * @param planner Planner in which the problem occured
     */
    public static void showExampleProblemWithFlawComparator(List<Flaw> flaws, Comparator<Flaw> comp, State st, APlanner planner) {
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

    public static int readInt() {
        String line = null;
        int val = 0;
        try {
            BufferedReader is = new BufferedReader(
                    new InputStreamReader(System.in));
            line = is.readLine();
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

}
