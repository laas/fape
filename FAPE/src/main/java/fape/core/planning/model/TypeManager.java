package fape.core.planning.model;

import fape.core.execution.model.Instance;
import fape.exceptions.FAPEException;


import java.util.*;


/**
 * Manages type in a planning domain. For each type, it also stores the instances of this types.
 * Currently supports recursive simple inheritance.
 *
 * Currently this class focus on flexibility and correctness but might lack efficiency (no indexing done).
 * However it probably won't be a bottleneck of the system.
 */
//public class TypeManager {
//
//    /**
//     * Maps from the name of the type to its definition.
//     */
//    HashMap<String, Type> map = new HashMap();
//
//    /**
//     * Maps from an object name to its type.
//     */
//    HashMap<String, String> objectTypes = new HashMap();
//
//    public Type getType(String typeName) {
//        return map.get(typeName);
//    }
//
//    public boolean containsType(String type) {
//        return map.containsKey(type);
//    }
//
//    /**
//     * Given a type t, returns a set containing t and all (possibly indirect) subclasses of t.
//     * @param typeName
//     * @return
//     */
//    public Set<Type> subtypes(String typeName) {
//        Type baseType = map.get(typeName);
//        Set<Type> subTypes = new HashSet<>();
//        subTypes.add(baseType);
//        boolean typeAdded = true;
//        while(typeAdded) {
//            typeAdded = false;
//            for(Type t : map.values()) {
//                if(subTypes.contains(map.get(t.parentTypeName))) {
//                    typeAdded = subTypes.add(t) || typeAdded;
//                }
//            }
//        }
//
//        return subTypes;
//    }
//
//    /**
//     * Returns all instances of a type (including instances of subtypes).
//     * @param type
//     * @return
//     */
//    public List<String> instances(String type) {
//        List<String> instances = new LinkedList<>();
//        for(Type t : subtypes(type)) {
//            for(String instance : t.instances.keySet()) {
//                instances.add(instance);
//            }
//        }
//
//        return instances;
//    }
//
//    /**
//     * Return all known isntances
//     * @return
//     */
//    public List<String> instances() {
//        List<String> instances = new LinkedList<>();
//        for(Type t : map.values()) {
//            instances.addAll(t.instances.keySet());
//        }
//        return instances;
//    }
//
//    public void addInstance(Instance i) {
//        if(!map.containsKey(i.type)) {
//            throw new FAPEException("Error: unknown type: "+i.type);
//        }
//        map.get(i.type).AddInstance(i.name);
//        objectTypes.put(i.name, i.type);
//    }
//
//    public boolean containsObject(String objectName) {
//        return objectTypes.containsKey(objectName);
//    }
//
//    public void addContent(String type, Instance i) {
//        if(!map.containsKey(i.type)) {
//            throw new FAPEException("Error: unknown type: "+i.type);
//        }
//        map.get(type).contents.put(i.name, i.type);
//    }
//
//    /**
//     * Looks recursively for the type of content into the containerType and its parents.
//     * @param containerType
//     * @param content
//     * @return
//     */
//    public String getContentType(String containerType, String content) {
//        assert containsType(containerType);
//        Type t = map.get(containerType);
//        if(t.contents.containsKey(content)) {
//            return t.contents.get(content);
//        } else {
//            return getContentType(t.parentTypeName, content);
//        }
//    }
//
//    public void addType(String name, String parent) {
//        if(map.containsKey(name)) {
//            throw new FAPEException("Error: type "+name+" is already recorded.");
//        }
//        Type t = new Type(name, parent);
//
//        map.put(name, t);
//    }
//
//    public String getObjectType(String objectName) {
//        return objectTypes.get(objectName);
//    }
//
//    public Collection<Type> getTypes() {
//        return map.values();
//    }
//}
