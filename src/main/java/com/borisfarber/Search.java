package com.borisfarber;

import javax.swing.*;
import java.util.*;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;

public class Search {
    private final Controller controller;
    private ArrayList<String> allLines;
    private TreeMap<String, Integer> preview;
    private List<ExtractedResult> resultSet;

    public Search(Controller controller) {
        this.controller = controller;
        allLines = new ArrayList<>();
        preview = new TreeMap<>();
        resultSet = new LinkedList<>();
    }

    public void crawl(ArrayList<String> allLines) {
        this.allLines = allLines;

        int index = 0;

        for (String line : this.allLines) {
            preview.put(line, index);
            index++;
        }
    }

    public void search(String query) {
        // todo template or strategy pattern for different search types
        resultSet = FuzzySearch.extractTop(query, allLines, 4);
    }

    public String getResults() {
        StringBuilder builder = new StringBuilder();

        for(ExtractedResult res : resultSet) {
            builder.append(res.getString());
            builder.append("\n");
        }

        return builder.toString();
    }

    public String getPreview() {
       // TODO work with the preview map
       return allLines.get(0);
    }

    public String getResultCount() {
        return Integer.toString(resultSet.size());
    }
    // todo not sure why needed
    public void removeUpdate() {
    }

    public Map<String, String> filenamesToPaths() {
        return new TreeMap<>();
    }

    public String toString() {
        // TODO clean up with StringBuilder
        for(ExtractedResult res : resultSet) {
            System.out.println(res.getString());
        }

        return "";
    }

    public static ArrayList<String> testLoad() {
        ArrayList<String> allLines = new ArrayList<>();

        allLines.add("Something ....");
        allLines.add("Something else....");
        allLines.add("Incremental Search ....");
        allLines.add("Incremental Search with Preview");
        allLines.add("Something .... other than else");
        allLines.add("Something .... clear");

        return allLines;
    }

    public static void main(String[] args) {
        System.out.println("Search");
        Search search = new Search(new Controller(new JTextArea(), new JTextArea(), new JLabel()));
        search.crawl(testLoad());
        search.search("set");
        System.out.println(search.getResults());
        System.out.println(search.getPreview());
        System.out.println(search.getResultCount());
    }
}
