package fr.laas.fape.planning.core.planning.search.flaws.flaws;

import java.util.List;

public class Flaws {

    /** provide a simple characterization of flaws. */
    public static Object hash(List<Flaw> flaws) {
        return flaws.size();
//        StringBuilder sb = new StringBuilder();
//        sb.append("n:");
//        sb.append(flaws.size());
//        for(Flaw f : flaws) {
//            sb.append(" ");
//            sb.append(f.getClass().getSimpleName());
//        }
//        return sb.toString();
    }
}
