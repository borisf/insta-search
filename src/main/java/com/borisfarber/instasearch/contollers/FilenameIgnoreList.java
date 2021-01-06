package com.borisfarber.instasearch.contollers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import static java.nio.file.Files.readAllLines;

public class FilenameIgnoreList {
    List<String> strRules = new LinkedList<>();

    public FilenameIgnoreList() {
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
           // TODO not sure if want to create a new file
        }
    }
}
