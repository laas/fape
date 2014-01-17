/*
 * Author:  Filip Dvořák <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2012 Filip Dvořák <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using of this program is prohibited
 * without previous written permission of author. Publishing or providing further
 * of the contents of this file is prohibited without previous written permission
 * of the author.
 */
package fLib.utils.io;

import java.io.*;

/**
 * I/O file handler, nothing fancy
 *
 * @author Filip Dvořák
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
        File fr = new File(file_path);
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
