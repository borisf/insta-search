/*
 * Copyright (c) 2001, 2014, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.borisfarber.instasearch.search;

import com.borisfarber.instasearch.controllers.Controller;
import com.borisfarber.instasearch.controllers.Pair;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MockSearch implements Search {
    private final Controller controller;
    private String query = "query";

    public MockSearch(Controller controller) {
        this.controller = controller;
    }

    @Override
    public void crawl(File file) {

    }

    @Override
    public void search(String query) {
        this.query = query;
        controller.onUpdateGUI();
    }

    @Override
    public LinkedList<Pair<String, Integer>> getFileNameAndPosition(String line) {
        Pair<String, Integer> pair = new Pair<>("DummyFile.txt", 0);
        LinkedList<Pair<String, Integer>> result = new LinkedList<>();
        result.add(pair);

        return result;
    }

    @Override
    public String getPreview(String resultLine) {
        return getSearchValue();
    }

    @Override
    public List<String> getResults() {
        String search = getSearchValue();
        LinkedList<String> result = new LinkedList<>();
        result.add(search);
        return result;
    }

    private String getSearchValue() {
        return "Dummy Search result" + query + "\n\n";
    }

    @Override
    public String getResultSetCount() {
        return "1";
    }

    @Override
    public Path getPathPerFileName(String fileName) {
        return null;
    }

    @Override
    public void testCrawl(ArrayList<String> testLoad) {

    }

    @Override
    public void close() {

    }

    @Override
    public void emptyQuery() {

    }
}
