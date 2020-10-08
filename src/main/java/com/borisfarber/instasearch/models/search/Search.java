package com.borisfarber.instasearch.models.search;

import com.borisfarber.instasearch.models.Pair;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public interface Search {

    int NOT_IN_FILE = -1;

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
