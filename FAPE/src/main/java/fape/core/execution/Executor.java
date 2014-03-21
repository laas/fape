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

import fape.util.FileHandling;
import fape.core.acting.Actor;
import fape.core.execution.model.AtomicAction;
import fape.exceptions.FAPEException;
import fape.util.Pair;
import fape.util.TimePoint;
import fape.util.TinyLogger;
import planstack.anml.model.concrete.ActRef;
import planstack.anml.model.concrete.VarRef;
import planstack.anml.parser.ANMLFactory;
import planstack.anml.parser.ParseResult;
import scala.util.parsing.combinator.Parsers;

import java.io.IOException;


/**
*
* @author FD
*/
public class Executor {

    public static int eventCounter = 0;

    Actor mActor;
    Listener mListener;

    // TODO: use regexp to parse messaged
    //Pattern failurePattern = Pattern.compile("\\(PRS-Action-Report (\\d+) FAILURE (\\d+) \"([^\"]*)\" \"([^\"]*)\"\\).*");

    /**
     *
     * @param a
     * @param l
     */
    public void bind(Actor a, Listener l) {
        mActor = a;
        mListener = l;
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
     * performs the translation between openPRS and ANML model, with some
     * message interpretatiton
     *
     * @param message
     */
    public void eventReceived(String message) {
        String tokens[] = message.split(" ");
        String msgType = tokens[0].split("\\(")[1];
        switch (msgType) {
            case "PRS-Action-Report":
                ActRef actionID = new ActRef(Integer.parseInt(tokens[1]));
                AtomicAction.EResult result = AtomicAction.EResult.valueOf((tokens[2]));
                int realEndTime = Integer.parseInt(tokens[3]);
                switch (result) {
                    case SUCCESS:
                        mActor.ReportSuccess(actionID, realEndTime);
                        break;
                    case FAILURE: // format = (PRS-Action-Report actionID FAILURE endTime "human readable comment" "anml block")
                        if(message.split("\"").length == 5) {
                            // this message contains an ANML block
                            String anmlBlock = message.split("\"")[3];
                            ParseResult block = ANMLFactory.parseAnmlString(anmlBlock);
                            mActor.PushEvent(block);
                        } else {
                            TinyLogger.LogInfo("No ANML block attached to this failure message");
                        }
                        mActor.ReportFailure(actionID);
                        break;
                    default:
                        throw new FAPEException("Unknown result: " + result.name());
                }
                break;
            case "PRS-ANML-Update":
                String anmlBlock = message.split("\"")[1];
                String logFileName = "msg_" + (eventCounter++) + ".log";
                FileHandling.writeFileOutput(logFileName, anmlBlock);
                ParseResult block = ANMLFactory.parseAnmlString(anmlBlock);
                mActor.PushEvent(block);
                break;
            default:
                throw new FAPEException("Unknown message type: " + msgType);
        }
    }

    /**
     * performs the translation between openPRS and ANML model
     *
     * @param acts
     */
    public void executeAtomicActions(AtomicAction acts) {


            String msg = "(FAPE-action "
                    + acts.id + " "
                    + acts.mStartTime + " "
                    + (acts.mStartTime + acts.duration)
                    + " "
                    + acts.GetDescription()
                    + ")\n";
            this.mListener.sendMessage(msg);

    }
}
