package com.borisfarber.instasearch.contollers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import static java.nio.file.Files.readAllLines;

public class IgnoreList {
    List<String> strRules = new LinkedList<>();

    public IgnoreList() {
        convertGitIgnoreToRules();
    }

    public boolean contains(String filename) {
        return strRules.contains(filename);
    }

    private void convertGitIgnoreToRules() {
        try {
            strRules = readAllLines(Path.of("ignore.txt"));
        } catch (IOException e) {
           strRules = new LinkedList<>();
            try {
                new File("ignore.txt").createNewFile();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}
