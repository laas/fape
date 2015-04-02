package fape.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public class Utils {

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
