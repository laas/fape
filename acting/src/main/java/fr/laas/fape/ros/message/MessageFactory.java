package fr.laas.fape.ros.message;

import fr.laas.fape.ros.ROSUtils;
import geometry_msgs.Pose;
import geometry_msgs.Quaternion;
import gtp_ros_msg.*;


public class MessageFactory {

    public static Ag getAg(String role, String agent) {
        Ag ag = ROSUtils.emptyMessageFromType("gtp_ros_msg/Ag");
        ag.setActionKey(role);
        ag.setAgentName(agent);
        return ag;
    }

    public static Obj getObj(String role, String agent) {
        Obj obj = ROSUtils.emptyMessageFromType("gtp_ros_msg/Obj");
        obj.setActionKey(role);
        obj.setObjectName(agent);
        return obj;
    }

    public static Data getData(String role, String value) {
        Data d = ROSUtils.emptyMessageFromType("gtp_ros_msg/Data");
        d.setDataKey(role);
        d.setDataValue(value);
        return d;
    }

    public static Points getRoledPoint(String role, double x, double y, double z) {
        Points ag = ROSUtils.emptyMessageFromType("gtp_ros_msg/Points");
        ag.setPointKey(role);
        ag.setValue(getPoint(x, y, z));
        return ag;
    }
    public static Pt getPoint(double x, double y, double z) {
        Pt pt = ROSUtils.emptyMessageFromType("gtp_ros_msg/Pt");
        pt.setX(x);
        pt.setY(y);
        pt.setZ(z);
        return pt;
    }

    public static Pt getXYYawFromPose(Pose pose) {
        Quaternion q = pose.getOrientation();
        double q0 = q.getX();
        double q1 = q.getY();
        double q2 = q.getZ();
        double q3 = q.getW();
        //refer <></>o roll in http://stackoverflow.com/questions/5782658/extracting-yaw-from-a-quaternion
        double yaw = Math.atan2(2.0 * (q0 * q1 + q3 * q2), q3 * q3 + q0 * q0 - q1 * q1 - q2 * q2);
        return getPoint(
                pose.getPosition().getX(),
                pose.getPosition().getY(),
                yaw);
    }

    public static Quaternion quaternionFromYaw(double yaw) {
        return quaternionFromEuler(0, 0, yaw);
    }

    public static Quaternion quaternionFromEuler(double eulerX, double eulerY, double eulerZ ) {
        double sx = Math.sin(eulerX / 2);
        double sy = Math.sin(eulerY / 2);
        double sz = Math.sin(eulerZ / 2);
        double cx = Math.cos(eulerX / 2);
        double cy = Math.cos(eulerY / 2);
        double cz = Math.cos(eulerZ / 2);
//        double cycz = cy * cz;
//        double sysz = sy * sz;
//        double d = cycz * cx - sysz * sx;
//        double a = cycz * sx + sysz * cx;
//        double b = sy * cz * cx + cy * sz * sx;
//        double c = cy * sz * cx - sy * cz * sx;

        double qw = (cx * cy * cz) + (sx * sy * sz);
        double qx = (sx * cy * cz) - (cx * sy * sz);
        double qy = (cx * sy * cz) + (sx * cy * sz);
        double qz = (cx * cy * sz) - (sx * sy * cz);

        double norm = qx*qx + qy*qy + qz*qz + qw*qw;

        Quaternion quat = ROSUtils.emptyMessageFromType("geometry_msgs/Quaternion");
        quat.setX(qx / norm);
        quat.setY(qy / norm);
        quat.setZ(qz / norm);
        quat.setW(qw / norm);
        return quat;
    }
}
