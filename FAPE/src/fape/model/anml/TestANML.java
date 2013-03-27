/*
 * Author:  Filip Dvořák <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2012 Filip Dvořák <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using this program is prohibited
 * without previous written permission of the author. Publishing or providing
 * further the contents of this file is prohibited without previous written
 * permission of the author.
 */
package fape.model.anml;

import gov.nasa.anml.PDDL;
import gov.nasa.anml.PDDLContext;
import gov.nasa.anml.PDDLE;
import gov.nasa.anml.lifted.Domain;
import gov.nasa.anml.parsing.ANMLCharStream;
import gov.nasa.anml.parsing.ANMLFileStream;
import gov.nasa.anml.parsing.ANMLLexer;
import gov.nasa.anml.parsing.ANMLParser;
import gov.nasa.anml.parsing.ANMLTree;
import gov.nasa.anml.parsing.ANMLTreeAdaptor;
import gov.nasa.anml.utility.OutputChannel;
import gov.nasa.anml.utility.OutputChannelLogHandler;
import gov.nasa.anml.utility.SimpleString;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.BufferedTreeNodeStream;
import org.antlr.runtime.tree.Tree;

/**
 *
 * @author FD
 */
public class TestANML {

    public static SimpleString dName;
    //public static ANMLCharStream input;
    public static PrintStream output, astOutput;
    public static Level logLevel;
    public static boolean outputPDDLE;
    public static boolean useUndef;
    public static final String AST_OUTPUT_SWITCH = "-a";
    public static final String OUTPUT_SWITCH = "-o";
    public static final String VERBOSE_MESSAGE_SWITCH = "-v";
    public static final String SUPPRESS_WARNING_SWITCH = "-w";
    public static final String PDDL_E_SWITCH = "-pddl-e";
    public static final String UNDEFINED_SWITCH = "-use-undef";

    public static void main(String[] args) throws IOException {
        ANMLCharStream input = new ANMLFileStream("C:\\ROOT\\PROJECTS\\fape\\FAPE\\problems\\dreamWorld.anml");
        //ANMLCharStream input = new ANMLFileStream("C:\\ROOT\\PROJECTS\\fape\\FAPE\\problems\\petro.anml");
        output = System.out;
        astOutput = System.out;
        logLevel = Level.WARNING;
        dName = Domain.defaultName;
        outputPDDLE = false;
        useUndef = false;
        Domain d = new Domain(dName);
        Logger logger = Logger.getLogger("ANMLLogger");
        logger.setUseParentHandlers(false);
        logger.addHandler(new OutputChannelLogHandler());
        logger.setLevel(logLevel);
        OutputChannel buf = new OutputChannel(new StringBuilder(100000), logger);


        // setup character->token, token->tree
        ANMLLexer lex = new ANMLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lex);
        ANMLParser parser = new ANMLParser(tokens);
        ANMLTreeAdaptor adaptor = new ANMLTreeAdaptor(ANMLParser.tokenNames);
        parser.setTreeAdaptor(adaptor);


        // do the initial parse
        ANMLParser.model_return r;
        try {
            r = parser.model(d);
            Tree t = (Tree) r.getTree();

            if (parser.errors) {
                System.exit(1);
            }

            // show the AST produced, for debugging
            String sTree = t.toStringTree();
            sTree = sTree.replaceAll("\0", "");
            astOutput.println(sTree);
            //System.out.println("\n\n\n\n\n");

            // setup tree->anml-model
            // For ANTLR versions < 3.2, use CommonTreeNodeStream instead.
            BufferedTreeNodeStream nodes = new BufferedTreeNodeStream(adaptor, t);
            nodes.setTokenStream(tokens);
            ANMLTree walker = new ANMLTree(nodes);
            walker.logger = buf;

            // process the tree 
            walker.model(d);

            
            int xx = 0;
        } catch (RecognitionException e) {
            e.printStackTrace();
        }
    }
}
