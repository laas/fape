package fape.core.planning.planninggraph;

import planstack.anml.model.AnmlProblem;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.parser.ANMLFactory;

import java.util.LinkedList;

public class Main {

    public static void main(String[] args) {
        String pbFile;
        if(args.length > 0)
            pbFile = args[0];
        else
            pbFile = "problems/Dream3.anml";

        AnmlProblem pb = new AnmlProblem();
        pb.addAnml(ANMLFactory.parseAnmlFromFile(pbFile));

        AbstractAction act = pb.getAction("PickWithLeftGripper");
        LinkedList<String> params = new LinkedList<>();
        params.add("robot");
        params.add("G");
        params.add("LA");
        params.add("LB");
        Action a = new Action(act, params);

        int x = 0;
    }
}
