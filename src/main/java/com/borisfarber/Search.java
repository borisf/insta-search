package com.borisfarber;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;

public class Search {
    private ArrayList<String> allLines;
    private TreeMap<String, Integer> preview;
    private List<ExtractedResult> resultSet;

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
        resultSet = FuzzySearch.extractTop(query, allLines, 10);
    }

    public String getResults() {
        StringBuilder builder = new StringBuilder();

        for (ExtractedResult res : getResultSet()) {
            builder.append(res.getString());
            builder.append("\n");
        }

        return builder.toString();
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

        // TODO stopped here UI doesn't work, empty
        if (resultSet.isEmpty()) {
            return "";
        }

        int allLinesIndex = preview.get(getResultSet().get(resultIndex).getString());

        int lower = allLinesIndex - 5;
        int upper = allLinesIndex + 5;

        if (lower < 0) {
            lower = 0;
        }

        if (upper >= allLines.size()) {
            upper = allLines.size() - 1;
        }

        StringBuilder builder = new StringBuilder();

        for (int i = lower; i < upper; i++) {
            builder.append(allLines.get(i) + "\n");
        }

        return builder.toString();
    }

    public List<ExtractedResult> getResultSet() {
        return resultSet;
    }

    public String getResultCount() {
        return Integer.toString(getResultSet().size());
    }

    public void removeUpdate() {
        // escape pressed, maybe do optimization
    }

    public String toString() {
        for (ExtractedResult res : getResultSet()) {
            System.out.println(res.getString());
        }
        return "";
    }

    public static ArrayList<String> folderToLines(File file) throws IOException {
        if (file == null || !file.exists()) {
            return new ArrayList<>();
        }

        ArrayList<String> result = new ArrayList<>();

        Path pathString = file.toPath();

        PathMatcher matcher =
                FileSystems.getDefault().getPathMatcher("glob:**.{java,kt,md,h,c,cpp,gradle,rs,txt}");

        Files.walkFileTree(pathString, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                    throws IOException {

                if (matcher.matches(path)) {
                    result.addAll(Files.readAllLines(path));
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return result;
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