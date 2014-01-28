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

import fLib.utils.io.FileHandling;
import fape.core.acting.Actor;
import fape.core.execution.model.ANMLBlock;
import fape.core.execution.model.ANMLFactory;
import fape.core.execution.model.AtomicAction;
import fape.exceptions.FAPEException;
import fape.util.Pair;
import fape.util.TimePoint;
import fape.util.TinyLogger;
import gov.nasa.anml.Main;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.Tree;

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
     *
     * @param path
     * @return
     */
    public ANMLBlock ProcessANMLfromFile(String path) {
        ANMLBlock b;
        try {
            Tree t = Main.getTree(path);
            b = ANMLFactory.Parse(t);
        } catch (RecognitionException | IOException e) {
            throw new FAPEException("System failed to read path: " + path);
        }
        return b;
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
                int actionID = Integer.parseInt(tokens[1]);
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
                            System.out.println(anmlBlock);
                            String logFileName = "msg_" + (eventCounter++) + ".log";
                            FileHandling.writeFileOutput(logFileName, anmlBlock);
                            ANMLBlock block = this.ProcessANMLfromFile(logFileName);
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
                ANMLBlock block = this.ProcessANMLfromFile(logFileName);
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
                    + acts.mID + " "
                    + acts.mStartTime + " "
                    + (acts.mStartTime + acts.duration)
                    + " "
                    + acts.GetDescription()
                    + ")\n";
            this.mListener.sendMessage(msg);

    }
}
