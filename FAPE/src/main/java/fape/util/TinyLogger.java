/*
 * Author:  Filip Dvořák <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2011 Filip Dvořák <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using of this program is prohibited
 * without previous written permission of author. Publishing or providing further
 * of the contents of this file is prohibited without previous written permission
 * of the author.
 */
package fape.util;

//import fape.core.planning.Planner;

import fape.core.planning.Planner;

/**
 *
 * @author Filip Dvořák
 */
public class TinyLogger {
    //public static boolean logging = false;
    public static void LogInfo(String st) {
        if (Planner.logging) {
            System.out.println("Logger:" + st);
        }
    }

}
