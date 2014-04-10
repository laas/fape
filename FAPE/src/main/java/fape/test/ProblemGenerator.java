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

import fape.util.FileHandling;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author FD
 */
public class ProblemGenerator {

    public static Random rg = new Random(0);//System.currentTimeMillis());
    public static String domain = FileHandling.getFileContents(new File("problems/BaseDomain.anml"));

    public static class Problem {

        public static int problemCounter = 0;
        
        int numberOfRobots;
        int numberOfItems;
        int numberOfLocations;
        int numberOfTransports; // < numberofitems

        public void Output() {
            String name = "Dream_" + (problemCounter++)+ "_" + numberOfRobots + "_" + numberOfItems + "_" + numberOfTransports + "_" + numberOfLocations + ".anml";
            String path = "problems/generated/"+name;

            String instanceRobot = "instance Robot ", instanceGripper = "instance Gripper ", instanceItem = "instance Item ", instanceLocation = "instance Location ",
                    startContent = "", allContent = "", goalContent = "", seedContent = "";

            int nextUnbindedVar = 0;

            for (int i = 0; i < numberOfLocations; i++) {
                instanceLocation += "L" + i + ((i < numberOfLocations - 1) ? ", " : "");
            }
            instanceLocation += ";\n";

            //generate robots and grippers
            for (int i = 0; i < numberOfRobots; i++) {
                instanceRobot += "R" + i + ((i < numberOfRobots - 1) ? ", " : "");
                instanceGripper += "G" + (2 * i) + ", " + "G" + (2 * i + 1) + ((i < numberOfRobots - 1) ? ", " : "");
                startContent += "R" + i + ".left := " + "G" + (2 * i) + ";\n";
                startContent += "R" + i + ".right := " + "G" + (2 * i + 1) + ";\n";
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
                int destLocation = rg.nextInt(numberOfLocations);
                goalContent += "I" + i + ".mLocation == " + "L" + destLocation + ";\n";
                seedContent += "constant Robot r"+nextUnbindedVar+"_;\n";
                seedContent += "constant Location loc"+(nextUnbindedVar+1)+"_;\n";
                seedContent += "Transport(r" + nextUnbindedVar++ + "_, I" + i + ", loc" + nextUnbindedVar++ + "_, L" + destLocation +");\n";
            }
            goalContent += "\n";
            
            

            String instances = instanceRobot + instanceGripper + instanceItem + instanceLocation;

            //String out = domain;
            String start = "[start] {\n"
                    + startContent
                    + "};\n\n";

            String all = "[all]{ \n"
                    + allContent
                    + "};\n\n";

            String goal = "[end] {\n"
                    + goalContent
                    + "};\n\n";

            String seed = "action Seed(){\n"
                    + " :decomposition{\n"
                    + seedContent + " };\n};\n\n";

            String out = domain + instances + start + all + goal + seed;

            fape.util.FileHandling.writeFileOutput(path, out);
        }
    }

    public static void Generate() {

        List<Problem> l = new LinkedList<>();

        /*for (int i = 0; i < 20; i++) {
            Problem p = new Problem();
            p.numberOfLocations = rg.nextInt(100);
            p.numberOfItems = p.numberOfLocations;
            p.numberOfRobots = 1;
            p.numberOfTransports = 1;
            l.add(p);
        }*/

        /*for (int i = 0; i < 20; i++) {
            Problem p = new Problem();
            p.numberOfLocations = 20;
            p.numberOfItems = 15;
            p.numberOfRobots = 1;
            p.numberOfTransports = rg.nextInt(2)+1;
            l.add(p);
        }*/

        for (int i = 0; i < 200; i++) {
            Problem p = new Problem();
            p.numberOfLocations = rg.nextInt(100)+1;
            p.numberOfItems = rg.nextInt(p.numberOfLocations)+1;
            p.numberOfRobots = rg.nextInt(20)+1;
            p.numberOfTransports = rg.nextInt(p.numberOfItems > 15 ? 15 : p.numberOfItems);
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
