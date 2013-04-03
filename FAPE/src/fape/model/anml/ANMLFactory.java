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
import fape.model.compact.statements.Statement;
import fape.model.compact.types.Type;
import gov.nasa.anml.parsing.ANMLCharStream;
import gov.nasa.anml.parsing.ANMLFileStream;
import gov.nasa.anml.parsing.ANMLLexer;
import gov.nasa.anml.parsing.ANMLParser;
import gov.nasa.anml.parsing.ANMLToken;
import gov.nasa.anml.parsing.ANMLTreeAdaptor;
import gov.nasa.anml.parsing.Domain;
import gov.nasa.anml.utility.OutputChannel;
import gov.nasa.anml.utility.OutputChannelLogHandler;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.Tree;

/**
 *
 * @author FD
 */
public class ANMLFactory {

    public static Tree ParseInput(String in) throws IOException, RecognitionException {
        //InputStream is = new ByteArrayInputStream( in.getBytes() );        
        ANMLCharStream input = new ANMLFileStream("C:\\ROOT\\PROJECTS\\fape\\FAPE\\problems\\dreamWorld.anml");

        // setup character->token, token->tree
        ANMLLexer lex = new ANMLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lex);
        ANMLParser parser = new ANMLParser(tokens);
        ANMLTreeAdaptor adaptor = new ANMLTreeAdaptor(ANMLParser.tokenNames);
        parser.setTreeAdaptor(adaptor);
        Domain d = new Domain();

        ANMLParser.model_return r = parser.model(d);
        Tree t = (Tree) r.getTree();

        return t;
    }

    public static ANMLBlock ParseTree(Tree t) {
        ANMLBlock ret = new ANMLBlock();
        
        
        ANMLToken types = (ANMLToken)t.getChild(0);
        ANMLToken constants = (ANMLToken)t.getChild(1);
        ANMLToken fluents = (ANMLToken)t.getChild(2);
        ANMLToken actions = (ANMLToken)t.getChild(3);
        ANMLToken statements = (ANMLToken)t.getChild(4);
        
        for(ANMLToken tr:(List<ANMLToken>)types.getChildren()){
            Type s = parseType(tr);
            ret.types.add(s);
        }
        
        for(ANMLToken tr:(List<ANMLToken>)actions.getChildren()){
            Action s = parseAction(tr);
            ret.actions.add(s);
        }
        
        for(ANMLToken tr:(List<ANMLToken>)statements.getChildren()){
            Statement s = parseStatement(tr);
            ret.statements.add(s);
        }
        
        
        
        
        
        
        
        return null;
    }
    
    public static void main(String[] args) throws IOException, RecognitionException {
        ANMLBlock b = ParseTree(ParseInput("none"));
    }

    private static Type parseType(ANMLToken tr) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private static Action parseAction(ANMLToken tr) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private static Statement parseStatement(ANMLToken tr) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
