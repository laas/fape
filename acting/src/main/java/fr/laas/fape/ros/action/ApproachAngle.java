package fr.laas.fape.ros.action;


import fr.laas.fape.ros.MainTesting;
import gtp_ros_msg.Pt;

public class ApproachAngle {

    public final double approachAngle;

    public ApproachAngle(double approachAngleDegrees) {
        this.approachAngle = approachAngleDegrees;
    }

    public double asRadian() {
        return approachAngle *2 * Math.PI /360;
    }

    public Pt getApproachPose(Pt target, double distance) {
        double deltaX = Math.cos(asRadian()) * distance;
        double deltaY = Math.sin(asRadian()) * distance;
        return fr.laas.fape.ros.message.MessageFactory.getPoint(target.getX()+ deltaX, target.getY()+deltaY, asRadian()+Math.PI);
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof ApproachAngle)
            return ((ApproachAngle) o).approachAngle == approachAngle;
        else
            return false;
    }

    @Override
    public int hashCode() {
        return (int) approachAngle*1000;
    }
}
