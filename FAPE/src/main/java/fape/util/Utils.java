package fape.util;

import java.util.*;

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
