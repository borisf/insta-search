 /*
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package com.borisfarber.instasearch.models.search;

 import com.borisfarber.instasearch.contollers.Controller;
 import com.borisfarber.instasearch.models.Pair;

 import javax.swing.*;
 import java.io.File;
 import java.io.IOException;
 import java.nio.file.FileVisitResult;
 import java.nio.file.Files;
 import java.nio.file.Path;
 import java.nio.file.SimpleFileVisitor;
 import java.nio.file.attribute.BasicFileAttributes;
 import java.util.*;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;

 import static com.github.eugenelesnov.NgramSearch.ngramSearch;
 import static java.nio.file.FileVisitResult.CONTINUE;
 import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

 public class FilenameSearch implements Search {
     private final Controller controller;
     private final ExecutorService executorService =
             Executors.newSingleThreadExecutor();

     private final ArrayList<String> allLines;
     private Map<String, Float> matchedSet;

     public FilenameSearch(Controller controller) {
         this.controller = controller;
         allLines = new ArrayList<>();
         matchedSet = new TreeMap<>();
     }

     @Override
     public void crawl(File file) {
         try {
             Files.walkFileTree(file.toPath(), new SimpleFileVisitor<>() {

                 @Override
                 public FileVisitResult preVisitDirectory(Path dir,
                                                          BasicFileAttributes attrs) {
                     if (dir.getFileName().toString().startsWith(".")) {
                         return SKIP_SUBTREE;
                     }
                     return CONTINUE;
                 }

                 @Override
                 public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                     //System.out.println(path);
                     allLines.add(path.toString());
                     return CONTINUE;
                 }
             });
         } catch (IOException e) {
             e.printStackTrace();
         }
     }

     @Override
     public void search(String query) {
         executorService.execute(() -> {
             if (allLines.isEmpty()) {
                 return;
             }

             if (query.length() < 3) {
                 synchronized (this) {
                     matchedSet = ngramSearch(1, 50, query, allLines, String::toString);
                 }
             } else {
                 matchedSet = ngramSearch(3, 50, query, allLines, String::toString);
             }

             Runnable runnable = controller::onSearchFinish;
             SwingUtilities.invokeLater(runnable);
         });
     }

     @Override
     public LinkedList<Pair<String, Integer>> getFileNameAndPosition(String line) {
         Pair<String,Integer> pair = new Pair<>(line,Search.NOT_IN_FILE);

         LinkedList<Pair<String, Integer>> result = new LinkedList<>();
         result.add(pair);
         return result;
     }

     @Override
     public String getPreview(String resultLine) {
         return resultLine;
     }

     @Override
     public List<String> getResults() {
         ArrayList<String> result = new ArrayList<>(matchedSet.size());
         matchedSet.forEach((k, v) -> result.add(k));

         return result;
     }

     @Override
     public String getResultSetCount() {
         return String.valueOf(allLines.size());
     }

     @Override
     public Path extractSelectedFile(String fileName) {
         return Path.of(fileName);
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
         return (s, t1) -> 1;
     }
 }
