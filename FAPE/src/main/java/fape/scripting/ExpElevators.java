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
        //AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats lcf>abs|soca>lmc --quiet");
        AnotherExperimentRunner.run("C:/ROOT/PROJECTS/fape/FAPE/problems/elevators/anml/", "--planner rpg --strats lcf>abs|fex --quiet");
    }
}
