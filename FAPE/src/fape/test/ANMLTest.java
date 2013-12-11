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

import fape.core.execution.model.ANMLFactory;
import fape.core.execution.model.ANMLBlock;
import java.io.IOException;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.Tree;

/**
 *
 * @author FD
 */
public class ANMLTest {

    /**
     *
     * @param args
     * @throws RecognitionException
     * @throws IOException
     */
    public static void main(String[] args) throws RecognitionException, IOException {
        Tree t = gov.nasa.anml.Main.getTree("C:\\ROOT\\PROJECTS\\fape\\FAPE\\problems\\Dream.anml");
        ANMLBlock b = ANMLFactory.Parse(t);
        int xx = 0;
    }
}
