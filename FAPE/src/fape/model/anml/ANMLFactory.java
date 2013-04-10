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
package fape.model.anml;

import fape.model.compact.ANMLBlock;
import fape.model.compact.Action;
import fape.model.compact.Instance;
import fape.model.compact.Parameter;
import fape.model.compact.Reference;
import fape.model.compact.TemporalConstraint;
import fape.model.compact.TemporalInterval;
import fape.model.compact.statements.Assign;
import fape.model.compact.statements.Equality;
import fape.model.compact.statements.Statement;
import fape.model.compact.types.Type;
import fape.util.Pair;
import gov.nasa.anml.parsing.ANMLToken;
import java.util.LinkedList;
import java.util.List;
import org.antlr.runtime.tree.Tree;

/**
 *
 * @author FD
 */
public class ANMLFactory {

    public static ANMLBlock Parse(Tree t) {
        ANMLBlock b = new ANMLBlock();

        //types
        for (int i = 0; i < t.getChild(0).getChildCount(); i++) {
            b.types.add(parseType(t.getChild(0).getChild(i)));
        }

        //instances
        for (int i = 0; i < t.getChild(2).getChildCount(); i++) {
            b.instances.add(parseInstance(t.getChild(2).getChild(i)));
        }

        //statements
        for (int i = 0; i < t.getChild(5).getChildCount(); i++) {
            if (!t.getChild(5).getChild(i).getText().equals("Skip")) {
                b.statements.addAll(parseStatements(t.getChild(5).getChild(i)));
            }
        }

        //actions
        for (int i = 0; i < t.getChild(4).getChildCount(); i++) {
            b.actions.add(parseAction(t.getChild(4).getChild(i)));
        }

        return b;
    }

    private static Type parseType(Tree child) {
        if (!child.getText().equals("Type")) {
            throw new UnsupportedOperationException("Typecheck failed.");
        }
        Type tp = new Type();
        tp.name = child.getChild(0).getText();
        tp.parent = child.getChild(2).getText();
        if (child.getChild(3).getChildCount() > 0) {
            Tree fluents = child.getChild(3).getChild(0).getChild(2);
            for (int i = 0; i < fluents.getChildCount(); i++) {
                tp.instances.add(parseInstance(fluents.getChild(i)));
            }
        }
        return tp;
    }

    private static Instance parseInstance(Tree child) {
        Instance in = new Instance();
        in.name = child.getChild(0).getText();
        in.type = child.getChild(1).getChild(0).getText();
        return in;
    }

    private static Action parseAction(Tree child) {
        Action a = new Action();
        //name
        a.name = child.getChild(0).getText();
        //parameters
        for (int i = 0; i < child.getChild(1).getChildCount(); i++) {
            a.params.add(parseParameter(child.getChild(1).getChild(i)));
        }
        //parse statements
        for (int i = 0; i < child.getChild(7).getChildCount(); i++) {
            a.tques.addAll(parseStatements(child.getChild(7).getChild(i)));
        }
        //parse decompositions
        for (int i = 0; i < child.getChild(8).getChildCount(); i++) {
            a.strongDecompositions.add(parseDecompositions(child.getChild(8).getChild(i)));
        }
        //what more to parse? TODO
        // - duration
        // - hard refinement
        // - soft refinement
        // - weak decompositions  
        return a;
    }

    private static List<Statement> parseStatements(Tree child) {
        List<Statement> ret = new LinkedList<>();
        //3 variants
        switch (child.getText()) {
            case "Skip": {
                //do nothing
                break;
            }
            case "Chain": {
                Assign rt = new Assign();
                rt.interval = parseInterval(child.getChild(0));
                rt.leftRef = parseReference(child.getChild(1));
                ret.add(rt);
                break;
            }
            case "==": {
                Equality rt = new Equality();
                rt.leftRef = parseReference(child.getChild(0));
                rt.rightRef = parseReference(child.getChild(1));
                ret.add(rt);
                break;
            }
            case "TimedStmt": {
                TemporalInterval interval = parseInterval(child.getChild(0));
                List<Statement> statements = new LinkedList<>();
                for (int i = 0; i < child.getChild(1).getChild(4).getChildCount(); i++) {
                    statements.addAll(parseStatements(child.getChild(1).getChild(4).getChild(i)));
                }
                for(Statement s:statements){
                    s.interval = interval;
                }
                ret.addAll(statements);
                break;
            }
            default:
                throw new UnsupportedOperationException();
        }
        return ret;
    }

    private static Reference parseReference(Tree child) {
        Reference r = new Reference();
        while (child.getText().equals("AccessField")) {
            r.refs.addFirst(child.getChild(1).getChild(0).getText()); //->ref->name
            child = child.getChild(0);
        }
        if (child.getChild(0) != null) {
            r.refs.addFirst(child.getChild(0).getText());
        }else{
            r.refs.addFirst(child.getText());
        }
        return r;
    }

    private static TemporalInterval parseInterval(Tree child) {
        TemporalInterval in = new TemporalInterval();
        if (child.getChildCount() == 1) {
            in.e = child.getChild(0).getText();
            in.s = child.getChild(0).getText();
        } else {
            in.e = child.getChild(1).getText();
            in.s = child.getChild(3).getText();
        }
        return in;
    }

    private static Parameter parseParameter(Tree child) {
        Parameter p = new Parameter();
        p.name = child.getChild(0).getText();
        p.type = child.getChild(1).getChild(0).getText();
        return p;
    }

    private static Pair<List<Action>, List<TemporalConstraint>> parseDecompositions(Tree child) {
        //Tree decs = child.getChild(4).getChild(0).getChild(4)
        
        !!!
        return null;
    }
}
