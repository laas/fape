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

import fape.util.FileHandling;

import java.io.File;


public class ParseResults {

    public static void main(String[] args) throws InterruptedException {
        String in = FileHandling.getFileContents(new File("res.TXT"));
        String ar[] = in.split("generated\\\\");
        String out = "";
        for (int i = 1; i < ar.length; i++) {
            String name = ar[i].split(" ")[0];
            //out += ar[i].split(" ")[0] + "| " + name.split("_")[2] + "| " + name.split("_")[3] + "| " + name.split("_")[4] + "| " + name.split("_")[5].split("\\.")[0] + "| " + ar[i].split(" ")[2].split("\n")[0].replace(".", ",").replace("s", "") + "\n";
            try {
                out += ar[i].split(" ")[0] + "| " + (Integer.parseInt(name.split("_")[2]) * 3 + Integer.parseInt(name.split("_")[3]) * 1 + Integer.parseInt(name.split("_")[5].split("\\.")[0]) * 1) + "| " + ar[i].split(" ")[2].split("\n")[0].replace(".", ",").replace("s", "") + "\n";
            } catch (Exception e) {
                int xx = 0;
            }
        }
        FileHandling.writeFileOutput("res4.txt", out);
    }
}
