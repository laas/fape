package fape.test;

import com.martiansoftware.jsap.*;
import fape.FAPE;

import java.io.File;
import java.io.FileFilter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Provides a way to test FAPE together with an OPRS supervisor.
 *
 * For every given file, it
 *  - launches a message passer
 *  - creates an OPRS instances that connects to the message passer.
 *  - run FAPE on the given anml problem.
 *
 *  FAPE is call as a java method to make debugging easier.
 */
public class PRSTester {

    public static void main(String[] args) throws Exception {
        SimpleJSAP jsap = new SimpleJSAP(
                "FAPE planners",
                "Tests FAPE integration with OPRS.",
                new Parameter[] {
                        new FlaggedOption("repetitions")
                                .setStringParser(JSAP.INTEGER_PARSER)
                                .setShortFlag('n')
                                .setLongFlag(JSAP.NO_LONGFLAG)
                                .setDefault("1")
                                .setHelp("Number of times to repeat all planning activities"),
                        new FlaggedOption("oprs-data")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setShortFlag(JSAP.NO_SHORTFLAG)
                                .setLongFlag("oprsdata")
                                .setDefault("/home/abitmonn/these/openrobots/share/openprs/data")
                                .setHelp("Where to look for OPRS data."),
                        new UnflaggedOption("anml-file")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setRequired(true)
                                .setGreedy(true)
                                .setHelp("ANML problem files on which to run the planners. If it is set " +
                                        "to a directory, all files ending with .anml will be considered. " +
                                        "Every file needs to be associated with an OPRS db that will be " +
                                        "passed to OPRS. (ProblemGenerator generates such databases together " +
                                        "with the anml files.")

                }
        );

        JSAPResult config = jsap.parse(args);
        if(jsap.messagePrinted())
            System.exit(0);

        String[] configFiles = config.getStringArray("anml-file");
        List<String> anmlFiles = new LinkedList<>();

        for(String path : configFiles) {
            File f = new File(path);
            if(f.isDirectory()) {
                File[] anmls = f.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File fi) {
                        return fi.getName().endsWith(".anml");
                    }
                });
                for(File anmlFile : anmls)
                    anmlFiles.add(anmlFile.getPath());
            } else {
                anmlFiles.add(path);
            }
        }

        Collections.sort(anmlFiles);

        // kill all previously recorded message passers and PRS
        ProcessBuilder killMP = new ProcessBuilder("kill-mp");
        killMP.start().waitFor();



        int repetitions = config.getInt("repetitions");
        for(int i=0 ; i<repetitions ; i++) {

            for(String anmlFile : anmlFiles) {
                String fileDB = anmlFile.replaceAll("\\.anml", ".db");

                System.out.println("\n\n\nCurrent domain: "+anmlFile+"\n");


                // make sure everybody is dead, then launch the message passer.
                Thread.sleep(3000);
                ProcessBuilder mp = new ProcessBuilder("mp-oprs", "-v");
                mp.start();
                Thread.sleep(1000);



                ProcessBuilder oprs = new ProcessBuilder("FAPE-PR2-Sim", "-n", "PR2", "-a", "-c", "load db \""+fileDB+"\"");
                oprs.environment().put("OPRS_DATA_PATH", config.getString("oprs-data"));
                oprs.start();

                Thread.sleep(1000);

                String[] fapeArgs = { anmlFile };
                FAPE.main(fapeArgs);

                Thread.sleep(10000);

                killMP = new ProcessBuilder("kill-mp");
                killMP.start().waitFor();
            }
        }
    }
}
