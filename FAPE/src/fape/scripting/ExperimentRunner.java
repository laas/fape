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
public class ExperimentRunner {

    public static void run(String path) throws InterruptedException {

        File f = new File(path);
        File[] anmls = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File fi) {
                return fi.getName().contains(".anml");
            }
        });

        Planner.debugging = false;
        Planner.logging = false;
        //Planner.actionResolvers = true;
        
        for (File a : anmls) {
            Planner.main(new String[]{a.getAbsolutePath()});
        }
    }
}
