package fape.core.execution;

import fape.core.execution.model.AtomicAction;
import fape.exceptions.FAPEException;
import fape.util.FileHandling;
import fape.util.TinyLogger;
import planstack.anml.model.concrete.ActRef;
import planstack.anml.parser.ANMLFactory;
import planstack.anml.parser.ParseResult;

public class ExecutorPRS extends Executor {

    Listener mListener;

    public ExecutorPRS(String oprs_host, String oprs_manip, String client_name, String socket_mp) {
        mListener = new Listener(oprs_host, oprs_manip, client_name, socket_mp);
        mListener.bind(this);
        int sendMessage = mListener.sendMessage("(FAPE-action -1 -1 -1 (InitializeTime))");
        if(sendMessage != Listener.OK) {
            System.err.println("Problem while contacting the message passer.");
            System.exit(1);
        }
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

    @Override
    public void abort() {
        mListener.abort();
    }

}
