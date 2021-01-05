package com.borisfarber.instasearch.contollers;

import org.eclipse.jgit.ignore.FastIgnoreRule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.Files.readAllLines;

public class GitIgnore {

    private final List<FastIgnoreRule> rules;

    public GitIgnore() {
        rules = new ArrayList<>();;
    }

    public static void main(String[] args) throws IOException {
        System.out.println("test");

        FastIgnoreRule fir = new FastIgnoreRule("");

        System.out.println(fir.isMatch("", true));


        List<String> mmm = readAllLines(Path.of(".gitignore"));

        System.out.println(mmm);
    }
}
