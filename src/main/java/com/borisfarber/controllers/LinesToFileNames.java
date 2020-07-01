package com.borisfarber.search;

import com.borisfarber.data.Pair;

import java.util.LinkedList;

import static com.borisfarber.search.FileSearch.testLoad;

public class LinesToFileNames {





    public static void main (String[] args) {
        LinkedList<Pair> fileNodes = new LinkedList<>();


        System.out.println("Search");
        FileSearch search = new FileSearch();
        search.crawl(testLoad());
        search.search("set");
        System.out.println(search.getResults());
        System.out.println(search.getPreview(0));
        System.out.println(search.getResultCount());

    }
}
