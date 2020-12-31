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
 import com.borisfarber.instasearch.contollers.PathMatchers;
 import com.borisfarber.instasearch.models.ResultModel;

 import javax.swing.*;
 import java.io.File;
 import java.io.IOException;
 import java.nio.file.*;
 import java.nio.file.attribute.BasicFileAttributes;
 import java.util.*;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;

 import static com.github.eugenelesnov.NgramSearch.ngramSearch;
 import static java.nio.file.FileVisitResult.CONTINUE;
 import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

 public class ContentSearch implements Search {
     private final Controller controller;
     // key design idea, no such thing file, it is recreated by line numbers
     private final ArrayList<String> allLines;
     private final TreeMap<String, Path> filenamesToPaths;
     private final ArrayList<Pair<Integer, String>> numLinesToFilenames;
     private final HashMap<String, List<Integer>> occurrences;

     private final ExecutorService executorService =
             Executors.newSingleThreadExecutor();
     private Map<String, Float> matchedSet;

     public ContentSearch(Controller controller) {
         this.controller = controller;
         allLines = new ArrayList<>();
         matchedSet = new TreeMap<>();
         filenamesToPaths = new TreeMap<>();
         numLinesToFilenames = new ArrayList<>();
         occurrences = new HashMap<>();
     }

     @Override
     public void crawl(File file) {
         allLines.clear();
         numLinesToFilenames.clear();
         filenamesToPaths.clear();
         occurrences.clear();

         if (file == null || !file.exists()) {
             return;
         }

         if(file.isDirectory() && Objects.requireNonNull(file.list()).length ==0) {
             return;
         }

         Path pathString = file.toPath();
         PathMatcher matcher = PathMatchers.SOURCE_OR_TEXT_MATCHER;

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
                     // one thread, check exception by printing path
                     // System.out.println("Thread:" + Thread.currentThread().getName());
                     if (matcher.matches(path)) {
                         try {
                             List<String> allFileLines = Files.readAllLines(path);
                             allLines.addAll(allFileLines);
                             Pair<Integer, String> pair = new Pair<>(allFileLines.size(),
                                     path.getFileName().toString());
                             numLinesToFilenames.add(pair);
                             filenamesToPaths.put(path.getFileName().toString(), path);
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

         processDuplicates(allLines);

         emptyQuery();
     }

     private void processDuplicates(List<String> allLines) {
         for(int index=0; index < allLines.size(); index++){
             if(occurrences.containsKey(allLines.get(index))) {
                 occurrences.get(allLines.get(index)).add(index);
             } else {
                 LinkedList<Integer> list = new LinkedList<>();
                 list.add(index);
                 occurrences.put(allLines.get(index), list);
             }
         }
     }

     @Override
     public void search(String query) {
         executorService.execute(() -> {
             if(allLines.isEmpty()) {
                 return;
             }

             if(query.length() < 3) {
                 matchedSet = ngramSearch(1,50, query, allLines, String::toString);
             } else {
                 matchedSet = ngramSearch(3,50, query, allLines, String::toString);
             }
             Runnable runnable = controller::onSearchFinish;
             SwingUtilities.invokeLater(runnable);
         });
     }

     @Override
     public LinkedList <Pair<String,Integer>> getFileNameAndPosition(String line) {
         LinkedList <Pair<String,Integer>> result = new LinkedList<>();

         for (Integer occ : occurrences.get(line)) {
             Pair<String, Integer> pair = getFileNameAndPositionFromLineIndex(occ);
             result.add(pair);
         }

         return result;
     }

     private Pair<String, Integer> getFileNameAndPositionFromLineIndex(int index) {
         int base = 0;
         for (Pair<Integer, String> pair : numLinesToFilenames) {
             if ((index >= base) && index < (base + pair.t - 1)) {
                 return new Pair<>(pair.u, (index - base));
             }
             base += pair.t;
         }
         return new Pair<>("", 0);
     }

     /**
      * 7 lines
      * result
      * 7 more lines
      *
      * @param resultLine the result string
      * @return preview
      */
     @Override
     public String getPreview(String resultLine) {
         if (resultLine.isEmpty()) {
             return "";
         }

         boolean isFileInternals = true;

         Pair<String, String> previewData = ResultModel.extractFilenameAndLineNumber(resultLine);
         String fileName = previewData.t;
         String line = previewData.u;

         if(!resultLine.contains(":")) {
             isFileInternals = false;
         }

         int bline = getFileBaseline(fileName);
         StringBuilder builder = new StringBuilder();
         int allLinesIndex = Integer.parseInt(line) + bline;
         int lower, upper;

         if(isFileInternals) {
             lower = allLinesIndex - 7;
             upper = allLinesIndex + 7;

             if (lower < 0) {
                 lower = 0;
             }
         } else {
             lower = allLinesIndex;
             upper = lower + 120;
         }

         if (upper >= allLines.size()) {
             upper = allLines.size() - 1;
         }

         for (int i = lower; i < upper; i++) {
             builder.append(allLines.get(i)).append("\n");
         }

         return builder.toString();
     }

     private int getFileBaseline(String fileName) {
         int bline = 0;

         for(Pair<Integer,String> fData : numLinesToFilenames) {
             if(fData.u.equals(fileName)) {
                 break;
             }
             bline += fData.t;
         }
         return bline;
     }

     @Override
     public Path extractSelectedFile(String fileName) {
         if (fileName.endsWith("/n")) {
             return filenamesToPaths.get(fileName.substring(0, fileName.length() - 1));
         }

         return filenamesToPaths.get(fileName);
     }

     @Override
     public List<String> getResults() {
         ArrayList<String> result = new ArrayList<>(matchedSet.size());
         result.addAll(matchedSet.keySet());

         return result;
     }

     @Override
     public String getResultSetCount() {
         return Integer.toString(this.getResults().size());
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
         Runnable runnable = () -> {
             ArrayList<String> allFiles = new ArrayList<>();
             allFiles.addAll(filenamesToPaths.keySet());
             controller.onCrawlFinish(allFiles);
         };
         SwingUtilities.invokeLater(runnable);
     }

     @Override
     public Comparator<String> getResultsSorter() {
         return (s, t1) -> 1;
     }

     public String toString() {
         for (String res : this.getResults()) {
             System.out.println(res);
         }
         return "";
     }
 }
