package com.borisfarber.controllers;

import com.borisfarber.data.Pair;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public interface Search {
    void crawl(File file);

    void search(String query);

    Pair<String, LinkedList<Integer>> getFileNameAndPosition(String line);

    String getResults();

    String getPreview(int resultIndex);

    List<String> getResultSet();

    String getResultSetCount();

    TreeMap<String, Path> getNameToPaths();

    void testCrawl(ArrayList<String> testLoad);

    void close();
}
