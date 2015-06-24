package fape.util;

import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.SimpleJSAP;
import fape.Planning;
import fape.exceptions.FAPEException;

import java.io.*;
import java.nio.charset.Charset;

public class Configuration {

    final JSAPResult userDefined ;
    final JSAPResult defaultConf;

    public Configuration(JSAPResult userDefined, String defaultConfFile) {
        this.userDefined = userDefined;
        File f = new File(defaultConfFile);
        if(f.exists()) {
            InputStream fis = null;
            try {
                fis = new FileInputStream(defaultConfFile);

                InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
                BufferedReader br = new BufferedReader(isr);
                String line = br.readLine();
                String[] args = line.split(" ");

                SimpleJSAP parser = Planning.getCommandLineParser(false);
                defaultConf = parser.parse(line);
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
}
