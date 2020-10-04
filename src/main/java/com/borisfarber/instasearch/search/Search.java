package com.borisfarber.instasearch.search;

import com.borisfarber.instasearch.model.Pair;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
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

    void close();

    void emptyQuery();

    Comparator<String> getResultsSorter();
}
