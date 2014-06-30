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

package fape.scripting;

/**
 *
 * @author FD
 */
public class ExpElevators {
    public static void main(String[] args) throws InterruptedException, Exception {
        
        //lfc / abs test
        /*
        //AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats lcf>abs|soca>lmc --quiet");
        AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats lcf>abs|lfr --quiet");
        AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats lcf>abs|fex --quiet");
        AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats lcf>abs|soca --quiet");
        AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats lcf>abs|psaoca --quiet");
        //AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats lcf>abs|lmc --quiet");
        
        AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats lcf|lfr --quiet");
        AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats lcf|fex --quiet");
        AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats lcf|soca --quiet");
        AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats lcf|psaoca --quiet");
        //AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats lcf|lmc --quiet");
        
        AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats abs|lfr --quiet");
        AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats abs|fex --quiet");
        AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats abs|soca --quiet");
        AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats abs|psaoca --quiet");
        //AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats abs|lmc --quiet");
        */
        
        //lmc-test
        
        //AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats lcf>abs|fex>lmc --quiet");
        //AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats lcf>abs|fex --quiet");
        //AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats lcf>abs|lfr --quiet");
        //AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats lcf>abs|lmc>lfr --quiet");
        
        
        //AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats lcf>abs|lmc>soca --quiet");
        //AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats lcf>abs|lmc>psaoca --quiet");
        
        //AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anmlComplicate/", "--planner rpg --strats lcf>abs|lfr --quiet");
        //AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anmlHierarchy/", "--planner rpg --strats lcf|lfr --quiet");
        
        //scaling
        AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/scaling/", "--planner base --strats lcf|fex --quiet");
        
    }
}
