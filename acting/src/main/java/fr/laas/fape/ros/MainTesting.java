package fr.laas.fape.ros;

import fr.laas.fape.ros.action.*;
import fr.laas.fape.ros.database.Attachments;
import fr.laas.fape.ros.database.Database;
import fr.laas.fape.ros.sensing.CourseCorrection;


public class MainTesting {

    public static void show(org.ros.internal.message.Message msg) {
        System.out.println(ROSUtils.format(msg));
    }

    public final static String AGENT = "PR2_ROBOT";

    public static void main(String[] argv)  {
        try {
            Database.initialize();
            CourseCorrection.spin();
            MoveTorsoActionServer.moveTorso(0.3f);
            ROSUtils.sleep(1000); // wait for initial population of database


            MoveBlind.turnTowards(ROSUtils.angleTowards("PR2_ROBOT","GREY_TAPE_1"));

//            MoveBlind.moveBackward(1);
//            show(MoveBaseClient.sendGoTo(1, 1, 0));
//            Attachments.clearAllAttachments();
//            GripperOperator.openGripper("right");
//            MoveArmToQ.moveLeftToManipulationPose();
//            MoveArmToQ.moveRightToManipulationPose();
//            MoveArmToQ.moveLeftToNavigationPose();
//            MoveArmToQ.moveRightToNavigationPose();

//            MoveBlind.moveForward(1);

//            GripperOperator.openGripper("left");

//            GoToPick.goToPick(AGENT, "GREY_TAPE_0");
//            MoveBlind.moveBackward(1.2);
//            GoToPick.goToPlace(AGENT, "GREY_TAPE_0", "TABLE_0");
//            MoveBlind.moveBackward(1);
//            MoveArmToQ.moveLeftStraight();
//            MoveBlind.moveForward(1);
//            GripperOperator.openGripper("left");

//            LootAt.lookAt("TABLE_0");
            System.out.println(Attachments.attachedObjects());

//            GTPUpdate.update();
//            GTPPlace place = new GTPPlace(AGENT, "GREY_TAPE_0", "TABLE_0");
//            place.send();
//            place.execute();

//

//            GTPUpdate.update();
//            GoToPick.engage(AGENT, "GREY_TAPE", null);

////
//            new GTPUpdate().send();
//            GTPMoveTo mv = GTPMoveTo.leftToManipulationPose(AGENT);
//            mv.send();
//            mv.getDetails();
//            mv.execute();
//
//            GTPMoveTo mv = GTPMoveTo.rightToManipulationPose(AGENT);
//            mv.send();
//            mv.getDetails();
//            mv.execute();
//
//
//            MoveBlind.moveBackward(1);


//            System.out.println(GripperOperator.openGripper("left"));
//
//            System.out.println(GripperOperator.openGripper("right"));
//
//            GTPUpdate update = new GTPUpdate();
//            update.send();
//
//            GTPPick pick = new GTPPick(AGENT, "GREY_TAPE");
//            pick.send();
//            pick.execute();

//            GTPMoveTo mv = GTPMoveTo.leftToRestPose(AGENT);
//            mv.send();
//            mv.getDetails();
//            mv.executeTrajectory(0);
//            show(mv.details);

//            Pose greyTapePose = Database.getPoseOf("GREY_TAPE");
//
//            Pt manipPoint = null;
//            for(Pt pt : ROSUtils.pointsAround(greyTapePose.getPosition().getX(), greyTapePose.getPosition().getY(), 0.8, 10)) {
//                GTPNavigateTo nav = new GTPNavigateTo("PR2_ROBOT", pt.getX(), pt.getY(), pt.getZ());
//                nav.send();
//                show(nav.result);
//                if(nav.result.getAns().getSuccess()) {
//                    manipPoint = pt;
//                    break;
//                }
//                nav.getDetails();
//                Thread.sleep(300);
//            }
//            if(manipPoint != null) {
//                System.out.println("Got manipulation point:");
//                show(manipPoint);
//                MoveBaseClient.sendGoTo(manipPoint.getX(), manipPoint.getY(), manipPoint.getZ());
//            }
//        System.out.println(ROSUtils.format(GTP.getInstance().sendGoTo(new ));
//        System.out.println(ROSUtils.format(GTP.getInstance().sendGoTo(new GTPPick("PR2_ROBOT", "GREY_TAPE"))));

//            GTPPick pick = new GTPPick("PR2_ROBOT", "GREY_TAPE");
//            pick.send();

//            MoveBlind.moveBackward(1.5);
//            goPick("GREY_TAPE");
//            new GTPUpdate().send();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            ROSNode.getNode().shutdown();
            System.exit(1);
        }
    }
}
