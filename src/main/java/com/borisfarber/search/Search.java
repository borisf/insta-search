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

    LinkedList<Pair<String, Integer>> getFileNameAndPosition(String line);

    String getResults();

    String getPreview(String resultLine);

    List<String> getResultSet();

    String getResultSetCount();

    TreeMap<String, Path> getFilenamesToPathes();

    void testCrawl(ArrayList<String> testLoad);

    void close();
}
