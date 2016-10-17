package fr.laas.fape.planning.util;

import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.SimpleJSAP;
import fr.laas.fape.planning.Planning;
import fr.laas.fape.planning.exceptions.FAPEException;

import java.io.*;
import java.nio.charset.Charset;

public class Configuration {

    final JSAPResult userDefined ;
    final JSAPResult defaultConf;

    public Configuration(JSAPResult userDefined, String defaultConfFile) {
        this.userDefined = userDefined;
        File f = new File(defaultConfFile);
        if(f.exists()) {
            InputStream fis;
            try {
                fis = new FileInputStream(defaultConfFile);

                InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
                BufferedReader br = new BufferedReader(isr);
                String line = br.readLine();

                SimpleJSAP parser = Planning.getCommandLineParser(false);
                defaultConf = parser.parse(line);
                if(parser.messagePrinted()) {
                    throw new FAPEException("Error reading default configuration file: "+defaultConfFile);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                throw new FAPEException("");
            } catch (IOException e) {
                e.printStackTrace();
                throw new FAPEException("");
            } catch (JSAPException e) {
                e.printStackTrace();
                throw new FAPEException("");
            }
        } else {
            try {
                defaultConf = Planning.getCommandLineParser(false).parse("");
            } catch (JSAPException e) {
                e.printStackTrace();
                throw new FAPEException("");
            }
        }
    }

    public boolean specified(String id) {
        return userDefined.userSpecified(id) || defaultConf.userSpecified(id);
    }

    public boolean getBoolean(String id) {
        if(userDefined.userSpecified(id))
            return userDefined.getBoolean(id);
        else
            return defaultConf.getBoolean(id);
    }

    public String getString(String id) {
        if(userDefined.userSpecified(id))
            return userDefined.getString(id);
        else
            return defaultConf.getString(id);
    }

    public int getInt(String id) {
        if(userDefined.userSpecified(id))
            return userDefined.getInt(id);
        else
            return defaultConf.getInt(id);
    }

    public float getFloat(String id) {
        if(userDefined.userSpecified(id))
            return userDefined.getFloat(id);
        else
            return defaultConf.getFloat(id);
    }

    public String[] getStringArray(String id) {
        if(userDefined.userSpecified(id))
            return userDefined.getStringArray(id);
        else
            return defaultConf.getStringArray(id);
    }
}
