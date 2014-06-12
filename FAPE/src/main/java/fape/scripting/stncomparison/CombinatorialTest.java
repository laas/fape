/*
 * Author:  Filip Dvoøák <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2013 Filip Dvoøák <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using this program is prohibited
 * without previous written permission of the author. Publishing or providing
 * further the contents of this file is prohibited without previous written
 * permission of the author.
 */
package fape.scripting.stncomparison;

import java.util.HashSet;
import java.util.Random;

/**
 *
 * @author FD
 */
public class CombinatorialTest {

    public static void main(String[] args) {
        int cnt = 10000;
        int nodes = 250;
        double avg = 0;
        while (cnt-- > 0) {
            Random rg = new Random();
            HashSet<Integer> set = new HashSet<>();
            int ct = 2*nodes*nodes/5;
            while (ct-- > 0) {
                set.add(rg.nextInt(nodes*nodes/2));
            }
            avg += set.size();
        }
        System.out.println(avg/(10000*nodes*nodes));
    }
}
