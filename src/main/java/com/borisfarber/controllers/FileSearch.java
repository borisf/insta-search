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
    private ArrayList<Pair<Integer, String>> numLinesToFiles;
    private List<ExtractedResult> resultSet;

    public FileSearch() {
        allLines = new ArrayList<>();
        preview = new TreeMap<>();
        numLinesToFiles = new ArrayList<>();
        resultSet = new ArrayList<>();
    }

    // think of state changes
    // long operation
    public void crawl(File file) throws IOException {
        if (file == null || !file.exists()) {
            return;
        }

        allLines.clear();
        numLinesToFiles.clear();
        preview.clear();
        Path pathString = file.toPath();

        PathMatcher matcher =
                FileSystems.getDefault().getPathMatcher("glob:**.{java,kt,md,h,c,cpp,gradle,rs,txt,cs}");

        Files.walkFileTree(pathString, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                    throws IOException {

                // one thread, check exception by printing path
                //System.out.println("Thread:" + Thread.currentThread().getName());
                // path get file name starts with dot, exit

                if (matcher.matches(path)) {
                    // TODO maybe sync block
                    List<String> allFileLines = Files.readAllLines(path);
                    allLines.addAll(allFileLines);
                    Pair <Integer, String> pair = new Pair<>(allFileLines.size(),
                            path.getFileName().toString());
                    numLinesToFiles.add(pair);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        // fill in the preview
        // TODO concurrency thought, keep out of the file walker, but IO bound
        // if multithreaded, do producer consumer, in the refactoring month
        // https://stackoverflow.com/questions/17732819/parallel-version-of-files-walkfiletree-java-or-scala
        int index = 0;
        for (String line : this.allLines) {
            preview.put(line, index);
            index++;
        }
    }

    // think of state changes
    public void search(String query) {
        resultSet = FuzzySearch.extractTop(query, allLines, 10);
    }

    public String getFileName(String line) {
        int index = preview.get(line).intValue();

        //boolean found = false
        int base = 0;

        for(Pair<Integer, String> pair : numLinesToFiles) {

            if(index > base && index < (base + pair.t.intValue())) {
                return pair.u;
            }

            base +=pair.t.intValue();

        }

        return "file.txt";
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
     * 15 lines
     * result
     * 15 more lines
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
        // TODO fix test
        //search.crawl(testLoad());
        search.search("set");
        System.out.println(search.getResults());
        System.out.println(search.getPreview(0));
        System.out.println(search.getResultCount());
    }
}