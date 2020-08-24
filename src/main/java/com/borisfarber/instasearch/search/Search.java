package com.borisfarber.search;

import com.borisfarber.data.Pair;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public interface Search {
    void crawl(File file);

    void search(String query);

    LinkedList<Pair<String, Integer>> getFileNameAndPosition(String line);

    String getPreview(String resultLine);

    List<String> getResults();

    String getResultSetCount();

    Path getPathPerFileName(String fileName);

    void testCrawl(ArrayList<String> testLoad);

    void close();

    void emptyQuery();
}
