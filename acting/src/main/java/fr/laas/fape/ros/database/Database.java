package fr.laas.fape.ros.database;


import fr.laas.fape.ros.ROSNode;
import geometry_msgs.Pose;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;
import toaster_msgs.*;

import java.lang.Object;
import java.util.HashMap;
import java.util.Map;

public class Database {

    private static class Property {
        public final String property;
        public final String param;
        public Property(String prop, String param) {
            this.property = prop;
            this.param = param;
        }
        @Override
        public int hashCode() { return property.hashCode() + param.hashCode(); }

        @Override
        public boolean equals(Object o) {
            if(o instanceof Property)
                return ((Property) o).property.equals(property) && ((Property) o).param.equals(param);
            else
                return false;
        }
    }

    private static Database instance;

    public static void initialize() {
        if(instance == null) {
            instance = new Database();
        }
    }

    public static Database getInstance() {
        if(instance == null) {
            initialize();
        }
        return instance;
    }

    private Map<String,DBEntry> db = new HashMap<>();
    private Map<Property,Object> properties = new HashMap<>();

    private Database() {
        ConnectedNode node = ROSNode.getNode();
        Subscriber<RobotListStamped> robotsSub = node.newSubscriber("pdg/robotList", RobotListStamped._TYPE);
        robotsSub.addMessageListener(msg -> {
            for(toaster_msgs.Robot o : msg.getRobotList()) {
                String id = o.getMeAgent().getMeEntity().getName();
                Pose pose = o.getMeAgent().getMeEntity().getPose();
                if(!db.containsKey(id)) {
                    db.put(id, new DBEntry());
                }
                db.get(id).pose = pose;
            }
        });

        Subscriber<ObjectListStamped> objectsSub = node.newSubscriber("pdg/objectList", ObjectListStamped._TYPE);
        objectsSub.addMessageListener(msg -> {
            for(toaster_msgs.Object o : msg.getObjectList()) {
                String id = o.getMeEntity().getName();
                Pose pose = o.getMeEntity().getPose();
                if(!db.containsKey(id)) {
                    db.put(id, new DBEntry());
                }
                db.get(id).pose = pose;
            }
        });
    }

    private class DBEntry {
        geometry_msgs.Pose pose;
    }

//    public static void set(String property, String param, Object value) {
//        getInstance().properties.put(new Property(property, param), value);
//    }

//    public static String getString(String property, String param) {
//        return (String) getInstance().properties.getOrDefault(new Property(property, param), null);
//    }

    public static Pose getPoseOf(String object) {
        Database db = getInstance();
        if(!db.db.containsKey(object))
            throw new IllegalArgumentException("Unknown object: "+object);
        return db.db.get(object).pose;
    }

    public static final String root = "fape/";

    public static void setInt(String param, int i) {
        ROSNode.getNode().getParameterTree().set(root+param, i);
    }


    public static void setBool(String param, boolean i) {
        ROSNode.getNode().getParameterTree().set(root+param, i);
    }

    public static void setString(String param, String i) {
        ROSNode.getNode().getParameterTree().set(root+param, i);
    }

    public static boolean getBool(String param) {
        return ROSNode.getNode().getParameterTree().getBoolean(root+param);
    }

    public static int getInt(String param) {
        return ROSNode.getNode().getParameterTree().getInteger(root+param);
    }

    public static String getString(String param) {
        return ROSNode.getNode().getParameterTree().getString(root+param);
    }

    public static boolean getBool(String param, boolean def) {
        return ROSNode.getNode().getParameterTree().getBoolean(root+param, def);
    }

    public static int getInt(String param, int def) {
        return ROSNode.getNode().getParameterTree().getInteger(root+param, def);
    }

    public static String getString(String param, String def) {
        return ROSNode.getNode().getParameterTree().getString(root+param, def);
    }
}
