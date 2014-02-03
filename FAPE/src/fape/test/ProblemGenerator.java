/*
 * Author:  Filip Dvořák <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2013 Filip Dvořák <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using this program is prohibited
 * without previous written permission of the author. Publishing or providing
 * further the contents of this file is prohibited without previous written
 * permission of the author.
 */
package fape.test;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author FD
 */
public class ProblemGenerator {

    public static Random rg = new Random(0);//System.currentTimeMillis());
    public static String domain = "/**\n"
            + " * Dream World domain in ANML\n"
            + " * \n"
            + " * so far adapted for the clean table example\n"
            + " *   \n"
            + " * Filip Dvořák <filip.dvorak@runbox.com>\n"
            + " * LAAS-CNRS\n"
            + " *  \n"
            + " * version 0.1 [draft]    \n"
            + " * version 0.2 [prepared for parsing]    \n"
            + " * version 0.3 [getting ready for execution]    \n"
            + " * version 0.4 [realistic example]     \n"
            + " * version 0.5 [action-addition example]\n"
            + " * version 0.6 [running addition example]       \n"
            + " * version 0.7 [this is a generated problem]       \n"
            + " * \n"
            + " * TODO:\n"
            + " *  - sanity checks\n"
            + " *     \n"
            + " */ \n"
            + "\n"
            + "\n"
            + "type Location < object;\n"
            + "\n"
            + "type Gripper < Location with {\n"
            + "  variable boolean empty;\n" 
            + "};\n"
            + "\n"
            + "type Robot < object with {\n"
            + "  variable Location mLocation;\n"
            + "  variable Gripper left;\n"
            + "  variable Gripper right;\n"
            + "};\n"
            + "\n"
            + "type Item < object with {\n"
            + "  variable Location mLocation;\n"
            + "  variable boolean onTable;\n"
            + "};\n"
            + "\n"
            + "/**\n"
            + " * defining actions\n"
            + " */\n"
            + " \n"
            + " \n"
            + "/**\n"
            + " * pick some item s with robor r, at location l \n"
            + " */  \n"
            + "\n"
            + "action PickWithRightGripper(Robot r, Gripper g, Item s, Location l){\n"
            + "        \n"
            + "  [all]{\n"
            + "    r.right == g;\n"
            + "    g.empty == true :-> false;\n"
            + "    r.mLocation == l;\n"
            + "    s.mLocation == l :-> g; \n"
            + "  }\n"
            + "};\n"
            + "\n"
            + "action DropWithRightGripper(Robot r, Gripper g, Item s, Location l){\n"
            + "  \n"
            + "  [all]{\n"
            + "    r.right == g;\n"
            + "    g.empty == false :-> true;\n"
            + "    r.mLocation == l;\n"
            + "    s.mLocation == g :-> l; \n"
            + "  };\n"
            + "   \n"
            + "}; \n"
            + "\n"
            + "action Move(Robot r, Location a, Location b){\n"
            + "  \n"
            + "  [all]{\n"
            + "    r.mLocation == a :-> b;\n"
            + "  };\n"
            + "  \n"
            + "}; \n\n";

    public static class Problem {

        public static int problemCounter = 0;
        
        int numberOfRobots;
        int numberOfItems;
        int numberOfLocations;
        int numberOfTransports; // < numberofitems

        public void Output() {
            String name = "Dream_" + (problemCounter++)+ "_" + numberOfRobots + "_" + numberOfItems + "_" + numberOfTransports + ".anml";
            String path = "problems/generated/"+name;

            String instanceRobot = "instance Robot ", instanceGripper = "instance Gripper ", instanceItem = "instance Item ", instanceLocation = "instance Location ",
                    startContent = "", allContent = "", goalContent = "";

            for (int i = 0; i < numberOfLocations; i++) {
                instanceLocation += "L" + i + ((i < numberOfLocations - 1) ? ", " : "");
            }
            instanceLocation += ";\n";

            //generate robots and grippers
            for (int i = 0; i < numberOfRobots; i++) {
                instanceRobot += "R" + i + ((i < numberOfRobots - 1) ? ", " : "");
                instanceGripper += "G" + (2 * i) + ", " + "G" + (2 * i + 1) + ((i < numberOfRobots - 1) ? ", " : "");
                allContent += "R" + i + ".left := " + "G" + (2 * i) + ";\n";
                allContent += "R" + i + ".right := " + "G" + (2 * i + 1) + ";\n";
                startContent += "G" + (2 * i) + ".empty := true;\n";
                startContent += "G" + (2 * i+1) + ".empty := true;\n";                               
                startContent += "R" + i + ".mLocation := " + "L" + rg.nextInt(numberOfLocations) + ";\n";
            }
            instanceRobot += ";\n";
            instanceGripper += ";\n";
            allContent += "\n";

            //items
            for (int i = 0; i < numberOfItems; i++) {
                instanceItem += "I" + i + ((i < numberOfItems - 1) ? ", " : "");
                startContent += "I" + i + ".mLocation := " + "L" + rg.nextInt(numberOfLocations) + ";\n";
            }
            instanceItem += ";\n";
            startContent += "\n";
            
            //adding goals
            for (int i = 0; i < numberOfTransports; i++) {
                goalContent += "I" + i + ".mLocation == " + "L" + rg.nextInt(numberOfLocations) + ";\n";
            }
            goalContent += "\n";
            
            

            String instances = instanceRobot + instanceGripper + instanceItem + instanceLocation;

            //String out = domain;
            String start = "[start] {\n"
                    + startContent
                    + "}\n\n";

            String all = "[all]{ \n"
                    + allContent
                    + "}\n\n";

            String goal = "[end] {\n"
                    + goalContent
                    + "}\n\n";

            String out = domain + instances + start + all + goal;

            fLib.utils.io.FileHandling.writeFileOutput(path, out);
        }
    }

    public static void Generate() {

        List<Problem> l = new LinkedList<>();

        for (int i = 0; i < 20; i++) {
            Problem p = new Problem();
            p.numberOfLocations = 100;
            p.numberOfItems = rg.nextInt(100);
            p.numberOfRobots = 1;
            p.numberOfTransports = 1;
            l.add(p);
        }

        for (int i = 0; i < 20; i++) {
            Problem p = new Problem();
            p.numberOfLocations = 20;
            p.numberOfItems = 15;
            p.numberOfRobots = 1;
            p.numberOfTransports = rg.nextInt(15);
            l.add(p);
        }

        for (int i = 0; i < 20; i++) {
            Problem p = new Problem();
            p.numberOfLocations = 20;
            p.numberOfItems = 15;
            p.numberOfRobots = 15;
            p.numberOfTransports = rg.nextInt(15);
            l.add(p);
        }
        
        for(Problem p:l){
            p.Output();
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        Generate();
    }
}
