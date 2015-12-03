package planstack.constraints;

import net.openhft.koloboke.collect.map.hash.*;

public class Kolokobe {

    public static HashIntIntMap getIntIntMap() {
        return HashIntIntMaps.newMutableMap();
    }

    public static <V> HashIntObjMap<V> getIntObjMap() {
        return HashIntObjMaps.getDefaultFactory().newMutableMap();
    }

    public static <V> HashIntObjMap<V> clone(HashIntObjMap<V> toClone) {
        return HashIntObjMaps.<V>newMutableMap(toClone);
    }

    public static HashIntIntMap clone(HashIntIntMap toClone) {
        return HashIntIntMaps.newMutableMap(toClone);
    }
}
