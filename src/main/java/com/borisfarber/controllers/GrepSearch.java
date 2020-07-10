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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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

    private File file = new File("test.txt");
    private ConcurrentLinkedQueue<String> result =
            new ConcurrentLinkedQueue<>();

    private CharBuffer cb1;
    private CharBuffer cb2;
    private CharBuffer cb3;
    private CharBuffer cb4;

    private TreeMap<String, Path> nameToPaths;
    private int qq;
    private List<String> preview;

    // keep the static reference for multi grep
    private static ExecutorService executorService =
            Executors.newFixedThreadPool(4);

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (executorService.isShutdown()) {
                    return;
                }

                executorService.shutdownNow();
            } catch (Throwable e) {

            }
        }));
    }

    public GrepSearch(Controller controller) {
        this.controller = controller;
        nameToPaths = new TreeMap<>();
    }

    @Override
    public void crawl(File file) {
        long start = System.currentTimeMillis();
        try {
            preview = Files.readAllLines(file.toPath(), charset);
            System.out.println("Read file " +
                    (System.currentTimeMillis() - start));

            this.file = file;
            nameToPaths.clear();
            nameToPaths.put(file.getName(), Path.of(file.toURI()));

            FileInputStream fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            int sz = (int)fc.size();
            qq = sz/4;

            fis.close();
            fc.close();

            cb1 = mapToCharBuffer(file, 0 * qq, qq);
            cb2 = mapToCharBuffer(file, 1 * qq, qq);
            cb3 = mapToCharBuffer(file, 2 * qq, qq);
            cb4 = mapToCharBuffer(file, 3 * qq, qq);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Crawl " + (System.currentTimeMillis() - start));
    }

    private CharBuffer mapToCharBuffer(File file, int start, int size) {
        CharBuffer result = null;
        try {
            // Open the file and then get a channel from the stream
            FileInputStream fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, start, size);
            result = decoder.decode(bb);

            fis.close();
            fc.close();

            return result;

        } catch (Exception e) {
        }

        return result;
    }

    @Override
    public void search(String query) {
        if(query.length() < 3) {
            return;
        }

        result.clear();
        compile(query);
        executeGrep();
    }

    // Search for occurrences of the input pattern in the given file
    private void executeGrep() {
        executorService.shutdownNow();
        executorService = Executors.newFixedThreadPool(4);

        executorService.execute(() -> {
            ArrayList<String> partialResults = grep(file, cb1);
            result.addAll(partialResults);
            controller.onUpdateGUI();
        });

        executorService.execute(() -> {
            ArrayList<String> partialResults = grep(file, cb2);
            result.addAll(partialResults);
            controller.onUpdateGUI();
        });

        executorService.execute(() -> {
            ArrayList<String> partialResults = grep(file, cb3);
            result.addAll(partialResults);
            controller.onUpdateGUI();
        });

        executorService.execute(() -> {
            ArrayList<String> partialResults = grep(file, cb4);
            result.addAll(partialResults);
            controller.onUpdateGUI();
        });
    }

    @Override
    public Pair<String, Integer> getFileNameAndPosition(String line) {
        String strkey = line.substring(0, line.length() - 1);
        int result = 0;

        result = preview.indexOf(strkey);

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
    private static ArrayList<String> grep(File f, CharBuffer cb) {
        ArrayList<String> result = new ArrayList<>();

        Matcher lm = linePattern.matcher(cb); // Line matcher
        Matcher pm = null;      // Pattern matcher
        int lines = 0;
        while (lm.find()) {
            lines++;
            CharSequence cs = lm.group();   // The current line
            if (pm == null)
                pm = pattern.matcher(cs);
            else
                pm.reset(cs);
            if (pm.find()) {
                //String s = f.toPath().getFileName().toString();
                //result.add(s + ":" + lines + ":" + cs);
                result.add(cs.toString());
            }

            if (lm.end() == cb.limit())
                break;
        }

        return result;
    }
}