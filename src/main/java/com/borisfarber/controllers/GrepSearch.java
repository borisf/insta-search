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
package com.borisfarber.controllers;

import com.borisfarber.data.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class GrepSearch implements Search {

    private static Charset charset = Charset.forName("ISO-8859-15");
    public static CharsetDecoder decoder = charset.newDecoder();

    // Pattern used to parse lines
    private static Pattern linePattern= Pattern.compile(".*\r?\n");

    // The input pattern that we're looking for
    private static Pattern pattern;
    private final Controller controller;

    // our stuff
    private File file = new File("test.txt");
    private ConcurrentLinkedQueue<String> result =
            new ConcurrentLinkedQueue<>();
    private CharBuffer cb1;
    private CharBuffer cb2;
    private CharBuffer cb3;
    private CharBuffer cb4;

    private TreeMap<String, Path> nameToPaths;
    private int qq;

    // TODO disposing the resources
    public GrepSearch(Controller controller) {
        this.controller = controller;
        nameToPaths = new TreeMap<>();
    }

    @Override
    public void crawl(File file) {
        long start = System.currentTimeMillis();
        try {
            this.file = file;

            nameToPaths.clear();
            nameToPaths.put(file.getName(), Path.of(file.toURI()));

            FileInputStream fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            int sz = (int)fc.size();

            qq = sz/4;

            // Open the file and then get a channel from the stream
            FileInputStream fis1 = new FileInputStream(file);
            FileChannel fc1 = fis1.getChannel();
            MappedByteBuffer bb1 = fc1.map(FileChannel.MapMode.READ_ONLY, 0, qq);
            cb1 = decoder.decode(bb1);

            FileInputStream fis2 = new FileInputStream(file);
            FileChannel fc2 = fis2.getChannel();
            MappedByteBuffer bb2 = fc2.map(FileChannel.MapMode.READ_ONLY, qq, qq);
            cb2 = decoder.decode(bb2);

            FileInputStream fis3 = new FileInputStream(file);
            FileChannel fc3 = fis3.getChannel();
            MappedByteBuffer bb3 = fc3.map(FileChannel.MapMode.READ_ONLY,2* qq, qq);
            cb3 = decoder.decode(bb3);

            FileInputStream fis4 = new FileInputStream(file);
            FileChannel fc4 = fis4.getChannel();
            MappedByteBuffer bb4 = fc4.map(FileChannel.MapMode.READ_ONLY,3* qq, qq);
            cb4 = decoder.decode(bb4);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Crawl " + (System.currentTimeMillis() - start));
    }

    @Override
    public void search(String query) {
        if(query.length() < 3) {
            return;
        }

        result.clear();
        compile(query);
        grep();
    }

    ExecutorService executorService = Executors.newFixedThreadPool(4);

    // Search for occurrences of the input pattern in the given file
    //
    private void grep() {
        executorService.execute(new Runnable() {
            public void run() {
                ArrayList<String> mmm = GrepSearch.grep(file, cb1, 0);
                result.addAll(mmm);
                controller.onUpdateGUI();

            }
        });

        executorService.execute(new Runnable() {
            public void run() {
                long start = System.currentTimeMillis();
                ArrayList<String> mmm = GrepSearch.grep(file, cb2, qq);
                result.addAll(mmm);
                controller.onUpdateGUI();

                System.out.println("Time to grep " + (System.currentTimeMillis() - start));
            }
        });

        executorService.execute(new Runnable() {
            public void run() {
                long start = System.currentTimeMillis();
                ArrayList<String> mmm = GrepSearch.grep(file, cb3, 2*qq);
                result.addAll(mmm);
                controller.onUpdateGUI();

                System.out.println("Time to grep " + (System.currentTimeMillis() - start));
            }
        });

        executorService.execute(new Runnable() {
            public void run() {
                long start = System.currentTimeMillis();
                ArrayList<String> mmm = GrepSearch.grep(file, cb4, 3*qq);
                result.addAll(mmm);
                controller.onUpdateGUI();

                System.out.println("Time to grep " + (System.currentTimeMillis() - start));
            }
        });

        // TODO find way to tear down
        //executorService.shutdown();
    }

    @Override
    public Pair<String, Integer> getFileNameAndPosition(String line) {
        String[] splitted = line.split(":");
        int result;

        try {
            result  = Integer.parseInt(splitted[1]);
        } catch (Exception e) {
            result = 0;
        }

        return new Pair<>(file.getName(), result);
    }

    @Override
    public String getResults() {
        return "";
    }

    @Override
    public String getPreview(int resultIndex) {
        return "";
    }

    @Override
    public List<String> getResultSet() {
        System.out.println("size" + result.size());
        String[] tmpArray = new String[result.size()];
        result.toArray(tmpArray);

        return Arrays.asList(tmpArray);
    }

    @Override
    public String getResultSetCount() {
        return String.valueOf(result.size());
    }

    @Override
    public TreeMap<String, Path> getNameToPaths() {
        return nameToPaths;
    }

    @Override
    public void testCrawl(ArrayList<String> testLoad) {

    }

    // Compile the pattern from the command line
    // case insensitive
    private static void compile(String pat) {
        try {
            pattern = Pattern.compile(pat, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException x) {
            System.err.println(x.getMessage());
            System.exit(1);
        }
    }

    // Use the linePattern to break the given CharBuffer into lines, applying
    // the input pattern to each line to see if we have a match
    //
    private static ArrayList<String> grep(File f, CharBuffer cb, int i) {
        ArrayList<String> result = new ArrayList<>();

        Matcher lm = linePattern.matcher(cb); // Line matcher
        Matcher pm = null;      // Pattern matcher
        int lines = i;
        while (lm.find()) {
            lines++;
            CharSequence cs = lm.group();   // The current line
            if (pm == null)
                pm = pattern.matcher(cs);
            else
                pm.reset(cs);
            if (pm.find()) {
                // TODO stopped here map f.name ==> f
                String s = f.toPath().getFileName().toString();

                result.add(s + ":" + lines + ":" + cs);
            }

            if (lm.end() == cb.limit())
                break;
        }

        return result;
    }

    public static void main(String[] args) {

    }
}