package fr.laas.fape.ros.sensing;


import fr.laas.fape.ros.ROSNode;
import gazebo_msgs.ModelStates;
import geometry_msgs.PoseWithCovarianceStamped;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

public class CourseCorrection {

    private static CourseCorrection instance;

    private final double[] cov = {0.25, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.25, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.06853891945200942 };

    private long lastCorrection = 0;
    private long correctionPeriod = 100;

    public boolean ACTIVE = true;
    public static void setSlowCorrection() {
        getInstance().correctionPeriod =1000;
    }
    public static void setFastCorrection() {
        getInstance().correctionPeriod = 50;
    }

    public static CourseCorrection getInstance() {
        if(instance == null) {
            instance = new CourseCorrection();
        }
        return instance;
    }

    public static void spin() {
        getInstance();
    }

    private CourseCorrection() {
        Subscriber<gazebo_msgs.ModelStates> robotsSub = ROSNode.getNode().newSubscriber("gazebo/model_states", ModelStates._TYPE);

        final Publisher<PoseWithCovarianceStamped> publisher =
                ROSNode.getNode().newPublisher("initialpose", PoseWithCovarianceStamped._TYPE);
        robotsSub.addMessageListener(msg -> {
            if(ACTIVE) {
                for (int i = 0; i < msg.getName().size(); i++) {
                    if (msg.getName().get(i).equals("pr2") && System.currentTimeMillis() > lastCorrection + correctionPeriod) {
                        lastCorrection = System.currentTimeMillis();
                        PoseWithCovarianceStamped update = publisher.newMessage();
                        update.getPose().setPose(msg.getPose().get(i));

                        update.getPose().setCovariance(cov);
                        publisher.publish(update);
                    }
                }
            }
        });
    }
}
