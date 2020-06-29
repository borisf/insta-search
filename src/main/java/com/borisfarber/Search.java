package com.borisfarber;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public class Search {
    private ArrayList<String> allLines;
    private TreeMap<String, Integer> preview;

    // todo encapsulate
    public List<ExtractedResult> resultSet;

    public Search() {
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
        resultSet = FuzzySearch.extractTop(query, allLines, 10);
    }

    public String getResults() {
        StringBuilder builder = new StringBuilder();

        for(ExtractedResult res : resultSet) {
            builder.append(res.getString());
            builder.append("\n");
        }

        return builder.toString();
    }


    private static boolean isBetween(int x, int lower, int upper) {
        return lower <= x && x <= upper;
    }

    /**
     * 5 lines
     * result
     * 5 more lines
     *
     * @param resultIndex
     * @return
     */
    public String getPreview(int resultIndex) {

        int allLinesIndex = preview.get(resultSet.get(resultIndex).getString());

        int lower = allLinesIndex - 5;
        int upper = allLinesIndex + 5;

        if (lower < 0) {
            lower = 0;
        }

        if (upper >= allLines.size()) {
            upper = allLines.size()-1;
        }

        StringBuilder builder = new StringBuilder();

        for(int i = lower; i < upper; i++) {
            builder.append(allLines.get(i) + "\n");
        }

        return builder.toString();
    }

    public String getResultCount() {
        return Integer.toString(resultSet.size());
    }

    public void removeUpdate() {
        // escape pressed, maybe do optimization
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
        Search search = new Search();
        search.crawl(testLoad());
        search.search("set");
        System.out.println(search.getResults());
        System.out.println(search.getPreview(0));
        System.out.println(search.getResultCount());
    }
}
