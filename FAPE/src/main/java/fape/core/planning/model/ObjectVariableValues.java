package fape.core.planning.model;

import fape.core.planning.temporaldatabases.IUnifiable;
import fape.util.TinyLogger;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;


/**
 * THis contains the set of values that can take an object variable.
 */
//public class ObjectVariableValues extends IUnifiable {
//
//    /**
//     * Constructor for a domain containing multiple values
//     * @param dom A set of values
//     * @param type Type of the variable
//     */
//    public ObjectVariableValues(Collection<String> dom, String type) {
//        domain.addAll(dom);
//        this.type = type;
//        this.mID = IUnifiable.idCounter++;
//    }
//
//    /**
//     * Constructor for a domain containing only one value.
//     * @param dom the unique value
//     * @param type Type of the variable
//     */
//    public ObjectVariableValues(String dom, String type) {
//        domain.add(dom);
//        this.type = type;
//        this.mID = IUnifiable.idCounter++;
//    }
//
//    /**
//     * A set of possible values for the variable
//     */
//    public final LinkedList<String> domain = new LinkedList<>();
//
//    /**
//     * Type of the variable.
//     */
//    public final String type;
//
//    /**
//     * reduces the domain, if elements were removed, returns true
//     *
//     * @param supported
//     * @return
//     */
//    @Override
//    public boolean ReduceDomain(HashSet<String> supported) {
//        LinkedList<String> remove = new LinkedList<>();
//        for (String v : domain) {
//            if (!supported.contains(v)) {
//                remove.add(v);
//            }
//        }
//        if (!remove.isEmpty()) {
//            TinyLogger.LogInfo("Reducing domain " + this.mID + " by: " + remove.toString());
//        }
//        domain.removeAll(remove);
//        return !remove.isEmpty();
//    }
//
//    @Override
//    public List<String> GetDomainObjectConstants() {
//        List<String> ret = new LinkedList<>(); // TODO: can we return the domain directly ?
//        for (String sv : domain) {
//            ret.add(sv);
//        }
//        return ret;
//    }
//
//    @Override
//    public int GetUniqueID() {
//        return mID;
//    }
//
//    @Override
//    public boolean EmptyDomain() {
//        return domain.isEmpty();
//    }
//
//    @Override
//    public String Explain() {
//        return " Action parameter";
//    }
//
//    public ObjectVariableValues DeepCopy() {
//        return new ObjectVariableValues(this.domain, this.type);
//    }
//}