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

 import com.borisfarber.data.Pair;
 import me.xdrop.fuzzywuzzy.model.ExtractedResult;

 import static java.nio.file.FileVisitResult.CONTINUE;
 import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

 public class FuzzySearch implements Search {
     private final ArrayList<String> allLines;
     private final TreeMap<String, Integer> preview;
     private final Controller controller;
     private TreeMap<String, Path> nameToPaths;
     private final ArrayList<Pair<Integer, String>> numLinesToFiles;
     private List<ExtractedResult> resultSet;

     public FuzzySearch(Controller controller) {
         allLines = new ArrayList<>();
         preview = new TreeMap<>();
         nameToPaths = new TreeMap<>();
         numLinesToFiles = new ArrayList<>();
         resultSet = new ArrayList<>();
         this.controller = controller;
     }

     @Override
     public void crawl(File file) {
         if (file == null || !file.exists()) {
             return;
         }

         allLines.clear();
         numLinesToFiles.clear();
         nameToPaths.clear();
         preview.clear();
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
                             numLinesToFiles.add(pair);
                             nameToPaths.put(path.getFileName().toString(), path);
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

         int index = 0;
         for (String line : this.allLines) {
             preview.put(line, index);
             index++;
         }
         // not a problem, pretty quick on one thread
         // System.out.println("finished crawling ==> " + allLines.size() + " elements");
     }

     @Override
     public void search(String query) {
         //long start = System.currentTimeMillis();
         resultSet = me.xdrop.fuzzywuzzy.FuzzySearch.extractTop(query, allLines, 15);
         //System.out.println(": " + ( System.currentTimeMillis() - start));
         controller.onUpdateGUI();
     }

     @Override
     public Pair<String, Integer> getFileNameAndPosition(String line) {
         // TODO parameter checks
         String[] split = line.split(":");
         return new Pair<>(split[0], Integer.parseInt(split[1]));
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
     public String getPreview(int resultIndex) {
         if (resultSet.isEmpty()) {
             return "";
         }

         int allLinesIndex = preview.get(getResultSetRawLines().get(resultIndex));

         int lower = allLinesIndex - 7;
         int upper = allLinesIndex + 7;

         if (lower < 0) {
             lower = 0;
         }

         if (upper >= allLines.size()) {
             upper = allLines.size() - 1;
         }

         StringBuilder builder = new StringBuilder();

         for (int i = lower; i < upper; i++) {
             builder.append(allLines.get(i) + "\n");
         }

         return builder.toString();
     }

     @Override
     public List<String> getResultSet() {
         ArrayList<String> result = new ArrayList<>(resultSet.size());

         for (ExtractedResult er : resultSet) {

             Pair<String, Integer> filenameAndPosition = getFileNameAndPositionFromRawLine(er.getString());

             String resultLine = filenameAndPosition.t + ":" + filenameAndPosition.u + ":" + er.getString();

             result.add(resultLine);
         }

         return result;
     }

     @Override
     public String getResultSetCount() {
         return Integer.toString(getResultSetRawLines().size());
     }

     public String toString() {
         for (String res : getResultSetRawLines()) {
             System.out.println(res);
         }
         return "";
     }

     @Override
     public void testCrawl(ArrayList<String> testLoad) {
         allLines.clear();
         allLines.addAll(testLoad);
         numLinesToFiles.clear();
         preview.clear();

         int index = 0;
         for (String line : this.allLines) {
             preview.put(line, index);
             index++;
         }
     }

     private Pair<String, Integer> getFileNameAndPositionFromRawLine(String line) {

         int index = preview.get(line).intValue();
         int base = 0;

         for (Pair<Integer, String> pair : numLinesToFiles) {
             if (index >= base && index < (base + pair.t.intValue() - 1)) {
                 return new Pair<>(pair.u, (index - base));
             }

             base += pair.t.intValue();
         }

         return new Pair<>("file.txt", 0);
     }

     private List<String> getResultSetRawLines() {
         ArrayList<String> result = new ArrayList<>(resultSet.size());

         for (ExtractedResult er : resultSet) {
             result.add(er.getString());
         }

         return result;
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

     @Override
     public TreeMap<String, Path> getNameToPaths() {
         return nameToPaths;
     }
 }