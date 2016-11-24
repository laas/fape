package fr.laas.fape.ros;


import fr.laas.fape.ros.action.ApproachAngle;
import fr.laas.fape.ros.database.Database;
import geometry_msgs.Pose;
import gtp_ros_msg.Pt;
import org.ros.internal.message.*;
import org.ros.internal.message.field.Field;
import org.ros.message.MessageFactory;
import org.ros.node.NodeConfiguration;

import java.util.ArrayList;
import java.util.List;

public class ROSUtils {

    static NodeConfiguration nodeConfiguration = NodeConfiguration.newPrivate();
    static MessageFactory messageFactory = nodeConfiguration.getTopicMessageFactory();

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {

        }
    }

    public static <T> T emptyMessageFromType(String type) {
        return messageFactory.newFromType(type);
    }

    public static String format(org.ros.internal.message.Message msg) {
        return format(msg, 0, false);
    }
    private static String format(org.ros.internal.message.Message msg, int depth, boolean listElem) {
        RawMessage m = msg.toRawMessage();
        StringBuilder sb = new StringBuilder();
        for(Field f : m.getFields()) {
            for(int i=0; i<depth ; i++) {
                if(listElem && i==depth-2) {
                    sb.append("-");
                    listElem = false;
                } else {
                    sb.append(" ");
                }
            }
            sb.append(f.getName());
            sb.append(": ");
            if(f.getValue() instanceof org.ros.internal.message.Message) {
                sb.append("\n");
                sb.append(format((org.ros.internal.message.Message) f.getValue(), depth + 2, false));
            } else if(f.getValue() instanceof List) {
                List l = f.getValue();
                if(l.isEmpty())
                    sb.append("[]");
                else if(l.get(0) instanceof org.ros.internal.message.Message) {
                    sb.append("[\n");
                    for(Object o : l) {
                        sb.append(format((org.ros.internal.message.Message) o, depth +3, true));
                    }
                    for(int i=0; i<depth ; i++)
                        sb.append(" ");
                    sb.append("]");
                }
            } else {
                sb.append(f.getValue().toString());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public static List<ApproachAngle> approaches(final int numApproaches) {
        List<ApproachAngle> approaches = new ArrayList<>();
        for(int i=1 ; i<=numApproaches ; i++) {
            final double angle = 360. /numApproaches *i;
            approaches.add(new ApproachAngle(angle));
        }
        return approaches;
    }

    public static double angleTowards(String agent, String target) {
        Pose p1 = Database.getPoseOf(agent);
        Pose p2 = Database.getPoseOf(target);
        double deltaX = p2.getPosition().getX() -p1.getPosition().getX();
        double deltaY = p2.getPosition().getY() -p1.getPosition().getY();
        return Math.atan2(deltaY, deltaX);
    }

    public static double dist(Pt p1, Pt p2) {
        return Math.sqrt(Math.pow(p1.getX()-p2.getX(),2) + Math.pow(p1.getY()-p2.getY(), 2));
    }

    public static double dist(Pose p1, Pose p2) {
        return Math.sqrt(Math.pow(p1.getPosition().getX()-p2.getPosition().getX(),2) + Math.pow(p1.getPosition().getY()-p2.getPosition().getY(), 2));
    }

    public static List<Pt> pointsAround(double x, double y, double radius, int numpoints) {
        List<Pt> points = new ArrayList<>();
        for(int i=1; i<= numpoints ; i++) {
            double theta = Math.PI *2 / numpoints *i;
            double deltaX = Math.cos(theta) * radius;
            double deltaY = Math.sin(theta) * radius;
            points.add(fr.laas.fape.ros.message.MessageFactory.getPoint(x + deltaX, y+deltaY, theta+Math.PI));
        }
        return points;
    }
}
