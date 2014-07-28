package fape.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public class Utils {

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
