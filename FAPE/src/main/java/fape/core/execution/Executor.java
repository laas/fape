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
package fape.core.execution;

import fape.core.acting.Actor;
import fape.core.execution.model.AtomicAction;
import planstack.anml.parser.ANMLFactory;
import planstack.anml.parser.ParseResult;


/**
*
* @author FD
*/
public abstract class Executor {

    public static int eventCounter = 0;

    protected Actor mActor;


    // TODO: use regexp to parse messaged
    //Pattern failurePattern = Pattern.compile("\\(PRS-Action-Report (\\d+) FAILURE (\\d+) \"([^\"]*)\" \"([^\"]*)\"\\).*");

    /**
     *
     * @param a
     */
    public void bind(Actor a) {
        mActor = a;
    }

    /**
     * Parses an ANML input from a file.
     * @param path Path to the file containing ANML.
    */
    public static ParseResult ProcessANMLfromFile(String path) {
        return ANMLFactory.parseAnmlFromFile(path);
    }

    public static ParseResult ProcessANMLString(String anml) {
        return ANMLFactory.parseAnmlString(anml);
    }


    /**
     * performs the translation between openPRS and ANML model
     *
     * @param acts
     */
    public abstract void executeAtomicActions(AtomicAction acts);

    public void abort() {
    }
}
