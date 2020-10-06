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
package com.borisfarber.instasearch.models.search;

import com.borisfarber.instasearch.contollers.*;
import com.borisfarber.instasearch.models.Pair;
import com.borisfarber.instasearch.models.text.SearchResultsSorter;
import com.borisfarber.instasearch.contollers.Controller;
import com.jramoyo.io.IndexedFileReader;

import javax.swing.*;
import java.io.*;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

public class GrepSearch implements Search {
    private static final Charset charset = Charset.forName("ISO-8859-15");
    private static final CharsetDecoder decoder = charset.newDecoder();

    // Pattern used to parse lines
    private static final Pattern linePattern= Pattern.compile(".*\r?\n");
    public static final int NUMBER_OF_TASKS = 4;

    // The input pattern that we're looking for
    private static Pattern pattern;
    private final Controller controller;

    private File file = new File("test.txt");
    private CharBuffer cb1;
    private CharBuffer cb2;
    private CharBuffer cb3;
    private CharBuffer cb4;
    private final TreeMap<String, Path> nameToPaths;
    private int qq;
    private List<String> preview= new ArrayList<>();
    private HashMap<String, LinkedList<Integer>> occurrences;
    private final ExecutorService executorService =
            Executors.newFixedThreadPool(NUMBER_OF_TASKS);
    static AtomicInteger finishedTasks = new AtomicInteger(0);

    private final ConcurrentLinkedQueue<String> result =
            new ConcurrentLinkedQueue<>();
    private String query;

    public GrepSearch(Controller controller) {
        this.controller = controller;
        nameToPaths = new TreeMap<>();
    }

    @Override
    public void crawl(File file) {
        if(file.isDirectory()) {
            this.file = dumpFolderToFile(file);
        }
        else {
            this.file = file;
        }

        try {
            preview = Files.readAllLines(this.file.toPath(), charset);
            processDuplicates(preview);
            nameToPaths.clear();
            nameToPaths.put(this.file.getName(), Path.of(this.file.toURI()));

            FileInputStream fis = new FileInputStream(this.file);
            FileChannel fc = fis.getChannel();
            int sz = (int)fc.size();
            qq = sz/4;

            fis.close();
            fc.close();

            cb1 = mapToCharBuffer(this.file, 0 * qq, qq);
            cb2 = mapToCharBuffer(this.file, 1 * qq, qq);
            cb3 = mapToCharBuffer(this.file, 2 * qq, qq);
            cb4 = mapToCharBuffer(this.file, 3 * qq, qq);

            int upper = 10;
            if(upper > preview.size()) {
                upper = preview.size();
            }

            StringBuilder builder = new StringBuilder();
            for(String str : preview.subList(0, upper)) {
                builder.append(str).append("\n");
            }

            builder.append("...");
            controller.onCrawlFinish(preview.subList(0,upper));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File dumpFolderToFile(File file) {
        Path pathString = file.toPath();
       
        try {
            Files.walkFileTree(pathString, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir,
                                                         BasicFileAttributes attrs) {
                    if (dir.getFileName().toString().startsWith(".")) {
                        return SKIP_SUBTREE;
                    }
                    return CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                        throws IOException {
                    if (PathMatchers.SOURCE_OR_TEXT_PATH_MATCHER.matches(path)) {
                        try {
                            List<String> allFileLines = Files.readAllLines(path);
                            preview.addAll(allFileLines);
                        } catch (java.nio.charset.MalformedInputException e) {
                            System.out.println("Bad file format " + path.getFileName().toString());
                        }
                    }
                    return CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            File dumpFile = PrivateFolder.INSTANCE.getTempFile("dump.txt");
            BufferedWriter outputWriter = new BufferedWriter(new FileWriter(dumpFile));

            for (String s : preview) {
                outputWriter.write(s);
                outputWriter.newLine();
            }

            outputWriter.flush();
            outputWriter.close();

            return dumpFile;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return file;
    }

    private void processDuplicates(List<String> preview) {
        occurrences = new HashMap<>();
        for(int index=0; index < preview.size(); index++){
            if(occurrences.containsKey(preview.get(index))) {
                occurrences.get(preview.get(index)).add(index);
            } else {
                LinkedList<Integer> list = new LinkedList<>();
                list.add(index);
                occurrences.put(preview.get(index), list);
            }
        }
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
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public void search(String query) {
        if(query.length() < 3) {
            return;
        }

        result.clear();
        this.query = query;
        compile(query);
        executeGrep();
    }

    private void executeGrep() {
        executorService.execute(() -> grepTask(file, result, controller, cb1));
        executorService.execute(() -> grepTask(file, result, controller, cb2));
        executorService.execute(() -> grepTask(file, result, controller, cb3));
        executorService.execute(() -> grepTask(file, result, controller, cb4));
    }

    private static void grepTask(File file, ConcurrentLinkedQueue<String> result,
                                 Controller controller, CharBuffer charBuffer) {
        ArrayList<String> partialResults = grep(file, charBuffer);
        result.addAll(partialResults);

        int task = finishedTasks.incrementAndGet();

        if(task == NUMBER_OF_TASKS) {
            Runnable runnable = () -> controller.onSearchFinish();
            SwingUtilities.invokeLater(runnable);
            finishedTasks.set(0);
        }
    }

    // Use the linePattern to break the given CharBuffer into lines, applying
    // the input pattern to each line to see if we have a match
    private static ArrayList<String> grep(File f, CharBuffer cb) {

        if (cb == null) {
            return new ArrayList<>();
        }

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

    @Override
    public LinkedList <Pair<String,Integer>> getFileNameAndPosition(String line) {
        if(line == null) {
            return new LinkedList<>();
        }

        String strkey = line.substring(0, line.length() - 1);
        LinkedList <Pair<String,Integer>> result = new LinkedList<>();

        if(!occurrences.containsKey(strkey)) {
            String awkMessage =
                    "awk 'BEGIN{RS=\"\\1\";ORS=\"\";getline;gsub(\"\\r\",\"\");" +
                            "print>ARGV[1]}' logcat.txt";
            System.err.println("Wrong new line formatting of the text file. If you are on linux and " +
                    "the file was generated with Windows use this:" + awkMessage);
            Pair<String, Integer> pair = new Pair<>(file.getName(), 0);
            result.add(pair);
            return result;
        }

        for (Integer occ : occurrences.get(strkey)) {
            Pair<String, Integer> pair = new Pair<>(file.getName(), occ);
            result.add(pair);
        }

        return result;
    }

    @Override
    public String getPreview(String resultLine) {

        long waitingTasksCount = ((ThreadPoolExecutor)(executorService)).getActiveCount();
        if(waitingTasksCount > 1) {
            return "";
        }

        executorService.execute(() -> {
            try {
                // TODO move to result model
                String[] parts  = resultLine.split(":");
                String lineNum = parts[1];
                int lineNumInt = Integer.parseInt(lineNum);

                int lowerBound = lineNumInt - 7;
                if(lowerBound < 1) {
                    lowerBound = 1;
                }

                IndexedFileReader reader = new IndexedFileReader(file);

                int upperBound = lineNumInt + 7;

                if(upperBound > reader.getLineCount()) {
                    upperBound = reader.getLineCount() - 1;
                }

                SortedMap<Integer, String> lines = reader.readLines(lowerBound,upperBound);

                StringBuilder builder = new StringBuilder();

                for (Map.Entry<Integer, String> entry : lines.entrySet()) {
                    builder.append(entry.getValue());
                    builder.append("\n");
                }

                String result =  builder.toString();

                Runnable runnable = () -> controller.onUpdatePreview(result);

                SwingUtilities.invokeLater(runnable);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return "building";
    }

    @Override
    public List<String> getResults() {
        String[] tmpArray = new String[result.size()];
        result.toArray(tmpArray);
        return Arrays.asList(tmpArray);
    }

    @Override
    public String getResultSetCount() {
        return String.valueOf(result.size());
    }

    @Override
    public Path getPathPerFileName(String fileName) {
        return nameToPaths.get(fileName);
    }

    @Override
    public void close() {
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

    @Override
    public void emptyQuery() {

    }

    @Override
    public Comparator<String> getResultsSorter() {
        return new SearchResultsSorter();
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
}
