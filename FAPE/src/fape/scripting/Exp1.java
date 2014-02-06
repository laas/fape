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

import fape.core.planning.Planner;
import java.io.File;
import java.io.FileFilter;

/**
 *
 * @author FD
 */
public class Exp1 extends ExperimentRunner {

    public static void main(String[] args) throws InterruptedException {
        run("problems/generated");
    }

}
