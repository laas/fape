package fr.laas.fape.ros.database;


import fr.laas.fape.ros.ROSNode;
import org.ros.exception.ParameterClassCastException;
import org.ros.node.parameter.ParameterTree;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class Attachments {

    private static final String root = "fape/attachments";

    public static void setAttachment(String object, String armName, long pickID) {
        ParameterTree params = ROSNode.getNode().getParameterTree();
        params.set(root+"/"+object+"/arm", armName);
        params.set(root+"/"+object+"/pickID", (int) pickID);
    }

    public static void removeAttachment(String object) {
        ROSNode.getNode().getParameterTree()
                .delete(root+"/"+object);
    }

    public static void clearAllAttachments() {
        if(ROSNode.getNode().getParameterTree().has(root))
            ROSNode.getNode().getParameterTree().delete(root);
    }

    public static boolean isAttached(String object) {
        return ROSNode.getNode().getParameterTree()
                .has(root+"/"+object);
    }

    public static String getHoldingArm(String object) {
        return ROSNode.getNode().getParameterTree()
                .getString(root+"/"+object+"/arm");
    }

    public static int getPickID(String object) {
        try {
            return ROSNode.getNode().getParameterTree()
                    .getInteger(root + "/" + object + "/pickID");
        } catch (ParameterClassCastException e) {
            return (int) ROSNode.getNode().getParameterTree()
                    .getDouble(root + "/" + object + "/pickID");
        }
    }

    public static Collection<String> attachedObjects() {
        if(ROSNode.getNode().getParameterTree().has(root))
            return ROSNode.getNode().getParameterTree()
                    .getMap(root)
                    .keySet().stream()
                    .map(x -> (String) x)
                    .collect(Collectors.toList());
        else
            return Collections.emptyList();
    }
}
