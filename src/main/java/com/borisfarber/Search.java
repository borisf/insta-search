package com.borisfarber;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Search {


    private final Controller controller;
    private ArrayList<String> allLines;
    private List<ExtractedResult> result;


    public Search(Controller controller) {
        this.controller = controller;
        allLines = new ArrayList<>();
    }

    public void load(File currentFile2) {

    }

    public void testLoad() {
        this.allLines.add("Something ....");
        this.allLines.add("Something else....");
        this.allLines.add("Incremental Search ....");
        this.allLines.add("Incremental Search with Preview");
        this.allLines.add("Something .... other than else");
        this.allLines.add("Something .... clear");

    }

    // todo not sure why needed
    public void removeUpdate() {
    }

    public Map<String, String> filenamesToPaths() {
        return new TreeMap<>();

    }

    public String status() {
        return " status";
    }

    public void search(String value) {
        // todo template or strategy pattern for different search types

        result = FuzzySearch.extractTop(value, allLines, 4);
    }

    public String toString() {
        // TODO clean up with StringBuilder
        for(ExtractedResult res : result) {
            System.out.println(res.getString());
        }

        return "";
    }

    public static void main(String[] args) {
        System.out.println("Search");
        Search search = new Search(new Controller(new JTextArea(), new JLabel(), new JTextArea()));
        search.testLoad();
        search.search("set");
        System.out.println(search);
    }
}
