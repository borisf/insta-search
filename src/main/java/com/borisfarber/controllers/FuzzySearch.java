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
 package com.borisfarber.controllers;

 import java.io.File;
 import java.io.IOException;
 import java.nio.file.*;
 import java.nio.file.attribute.BasicFileAttributes;
 import java.util.*;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;

 import com.borisfarber.data.Pair;
 import me.xdrop.fuzzywuzzy.model.ExtractedResult;

 import javax.swing.*;

 import static java.nio.file.FileVisitResult.CONTINUE;
 import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

 public class FuzzySearch implements Search {
     private final Controller controller;
     // key design idea, no such thing file, it is recreated by line numbers
     private final ArrayList<String> allLines;
     private List<ExtractedResult> resultSet;
     private final TreeMap<String, Path> filenamesToPathes;
     private final ArrayList<Pair<Integer, String>> numLinesToFilenames;
     private HashMap<String, List<Integer>> occurrences;

     private ExecutorService executorService =
             Executors.newSingleThreadExecutor();

     public FuzzySearch(Controller controller) {
         this.controller = controller;
         allLines = new ArrayList<>();
         resultSet = new ArrayList<>();
         filenamesToPathes = new TreeMap<>();
         numLinesToFilenames = new ArrayList<>();
     }

     @Override
     public void crawl(File file) {
         if (file == null || !file.exists()) {
             return;
         }

         allLines.clear();
         numLinesToFilenames.clear();
         filenamesToPathes.clear();
         Path pathString = file.toPath();

         PathMatcher matcher =
                 FileSystems.getDefault().getPathMatcher("glob:**.{java,kt,md,h,c,cpp,gradle,rs,txt,cs}");

         try {
             Files.walkFileTree(pathString, new SimpleFileVisitor<Path>() {

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
                             filenamesToPathes.put(path.getFileName().toString(), path);
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

         Runnable runnable = () -> {
             StringBuilder builder = new StringBuilder();
             for (String file1 : filenamesToPathes.keySet()) {
                 builder.append(file1 + "\n");
             }
             controller.resultTextPane.setText(builder.toString());
         };
         SwingUtilities.invokeLater(runnable);
     }

     private void processDuplicates(List<String> allLines) {
         occurrences = new HashMap<>();
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
             resultSet = me.xdrop.fuzzywuzzy.FuzzySearch.extractTop(query, allLines, 10);
             Runnable runnable = () -> controller.onUpdateGUI();
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
         return new Pair<>("file.txt", 0);
     }

     @Override
     public String getResults() {
         StringBuilder builder = new StringBuilder();

         for (ExtractedResult res : resultSet) {
             builder.append(res.getString());
             builder.append("\n");
         }

         return builder.toString();
     }

     /**
      * 7 lines
      * result
      * 7 more lines
      *
      * @param resultIndex
      * @return
      */
     @Override
     public String getPreview(String resultLine) {
         if (resultLine.isEmpty()) {
             return "";
         }

         String[] parts  = resultLine.split(":");
         String fileName = parts[0];
         String line = parts[1];

         int bline = 0;

         for(Pair<Integer,String> fData : numLinesToFilenames) {
             if(fData.u.equals(fileName)) {
                 break;
             }
             bline += fData.t;
         }

         StringBuilder builder = new StringBuilder();
         int allLinesIndex = Integer.parseInt(line) + bline;

         int lower = allLinesIndex - 7;
         int upper = allLinesIndex + 7;

         if (lower < 0) {
             lower = 0;
         }

         if (upper >= allLines.size()) {
             upper = allLines.size() - 1;
         }

         for (int i = lower; i < upper; i++) {
             builder.append(allLines.get(i) + "\n");
         }

         return builder.toString();
     }

     @Override
     public TreeMap<String, Path> getFilenamesToPathes() {
         return filenamesToPathes;
     }

     @Override
     public List<String> getResultSet() {
         ArrayList<String> result = new ArrayList<>(resultSet.size());

         for (ExtractedResult er : resultSet) {
             result.add(er.getString());
         }

         return result;
     }

     @Override
     public String getResultSetCount() {
         return Integer.toString(getResultSet().size());
     }

     @Override
     public void testCrawl(ArrayList<String> testLoad) {
         allLines.clear();
         allLines.addAll(testLoad);
         numLinesToFilenames.clear();
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

     public String toString() {
         for (String res : getResultSet()) {
             System.out.println(res);
         }
         return "";
     }

     public static ArrayList<String> testLoad() {
         ArrayList<String> allLines = new ArrayList<>();

         allLines.add("Something ....");
         allLines.add("Something else....");
         allLines.add("Incremental Search ....");
         allLines.add("Incremental Search with Preview");
         allLines.add("Something .... other than else");
         allLines.add("Something .... clear");

         return allLines;
     }

     public static void main(String[] args) {
         /*
         System.out.println("Search");
         FuzzySearch search = new FuzzySearch();
         search.testCrawl(testLoad());
         search.search("set");
         System.out.println(search.getResults());
         System.out.println(search.getPreview(0));
         System.out.println(search.getResultSetCount());*/
     }
 }