package fr.laas.fape.planning.util;

import java.io.*;

/**
 * I/O file handler, nothing fancy
 */
public class FileHandling {

    /**
     * buffering, reading one line at a time
     *
     * @param file_path
     * @return
     */
    public static String readFileContents(String file_path) {
        StringBuilder contents = new StringBuilder();
        try (BufferedReader input = new BufferedReader(new FileReader(file_path))) {
            String line;
            while ((line = input.readLine()) != null) {
                contents.append(line);
                contents.append(System.getProperty("line.separator"));
            }
        } catch (IOException ex) {
            return null;
        }
        return contents.toString();
    }

    /**
     * buffering, reading one line at a time
     * @param f
     * @return
     */
    public static String getFileContents(File f) {
        return readFileContents(f.getAbsolutePath());
    }

    /**
     * write to a file
     *
     * @param filePath
     * @param content
     */
    public static void writeFileOutput(String filePath, String content) {
        try (FileWriter fw = new FileWriter(new File(filePath))) {
            fw.write(content);
            fw.flush();
        } catch (IOException e) {
            throw new UnsupportedOperationException("Could not write the file: " + filePath);
        }
    }
}
