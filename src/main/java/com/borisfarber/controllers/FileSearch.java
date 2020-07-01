package com.borisfarber.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import com.borisfarber.data.Pair;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;

public class FileSearch {
    private ArrayList<String> allLines;
    private TreeMap<String, Integer> preview;
    private List<ExtractedResult> resultSet;

    public FileSearch() {
        allLines = new ArrayList<>();
        preview = new TreeMap<>();
        resultSet = new ArrayList<>();
    }

    // think of state changes
    // TODO merge into folderToLines method
    public void crawl(ArrayList<String> allLines) {
        this.allLines = allLines;

        int index = 0;

        for (String line : this.allLines) {
            preview.put(line, index);
            index++;
        }
    }

    public static Pair<ArrayList<String>, LinkedList<Pair>> folderToLines(File file) throws IOException {
        if (file == null || !file.exists()) {
            return new Pair(new ArrayList<>(), new LinkedList<>());
        }

        ArrayList<String> lines = new ArrayList<>();
        LinkedList<Pair<Integer, String>> linesToFiles= new LinkedList<>();

        Path pathString = file.toPath();

        PathMatcher matcher =
                FileSystems.getDefault().getPathMatcher("glob:**.{java,kt,md,h,c,cpp,gradle,rs,txt,cs}");

        Files.walkFileTree(pathString, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                    throws IOException {

                if (matcher.matches(path)) {
                    List<String> mmm = Files.readAllLines(path);
                    lines.addAll(mmm);
                    Pair <Integer, String> pair = new Pair<>(mmm.size(), path.getFileName().toString());
                    linesToFiles.add(pair);

                }
                return FileVisitResult.CONTINUE;
            }
        });

        Pair result = new Pair(lines, linesToFiles);

        return result;
    }

    // think of state changes
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
        if (resultSet.isEmpty()) {
            return "";
        }

        int allLinesIndex = preview.get(getResultSet().get(resultIndex).getString());

        int lower = allLinesIndex - 15;
        int upper = allLinesIndex + 15;

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
        FileSearch search = new FileSearch();
        search.crawl(testLoad());
        search.search("set");
        System.out.println(search.getResults());
        System.out.println(search.getPreview(0));
        System.out.println(search.getResultCount());
    }
}