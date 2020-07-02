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
 import java.util.ArrayList;
 import java.util.List;
 import java.util.TreeMap;

 import com.borisfarber.data.Pair;
 import me.xdrop.fuzzywuzzy.FuzzySearch;
 import me.xdrop.fuzzywuzzy.model.ExtractedResult;

 public class FileSearch {
     private final ArrayList<String> allLines;
     private final TreeMap<String, Integer> preview;
     private final ArrayList<Pair<Integer, String>> numLinesToFiles;
     private List<ExtractedResult> resultSet;

     public FileSearch() {
         allLines = new ArrayList<>();
         preview = new TreeMap<>();
         numLinesToFiles = new ArrayList<>();
         resultSet = new ArrayList<>();
     }

     public void crawl(File file) {
         if (file == null || !file.exists()) {
             return;
         }

         allLines.clear();
         numLinesToFiles.clear();
         preview.clear();
         Path pathString = file.toPath();

         PathMatcher matcher =
                 FileSystems.getDefault().getPathMatcher("glob:**.{java,kt,md,h,c,cpp,gradle,rs,txt,cs}");

         try {
             Files.walkFileTree(pathString, new SimpleFileVisitor<Path>() {
                 @Override
                 public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                         throws IOException {
                     // one thread, check exception by printing path
                     //System.out.println("Thread:" + Thread.currentThread().getName());
                     // path get file name starts with dot, exit
                     if (matcher.matches(path)) {
                         try {
                             // TODO maybe a sync block
                             List<String> allFileLines = Files.readAllLines(path);
                             allLines.addAll(allFileLines);
                             Pair<Integer, String> pair = new Pair<>(allFileLines.size(),
                                     path.getFileName().toString());
                             numLinesToFiles.add(pair);
                         } catch (java.nio.charset.MalformedInputException e) {
                             System.out.println("Bad file format " + path.getFileName().toString());
                         }
                     }
                     return FileVisitResult.CONTINUE;
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
         System.out.println("finished crawling ==> " + allLines.size() + " elements");
     }

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

     public void search(String query) {
         long start = System.currentTimeMillis();
         resultSet = FuzzySearch.extractTop(query, allLines, 15);
         System.out.println(": " + ( System.currentTimeMillis() - start));
     }

     public String getFileName(String line) {
         int index = preview.get(line).intValue();
         int base = 0;

         for (Pair<Integer, String> pair : numLinesToFiles) {
             if (index > base && index < (base + pair.t.intValue())) {
                 return pair.u;
             }

             base += pair.t.intValue();
         }

         return "file.txt";
     }

     public String getResults() {
         StringBuilder builder = new StringBuilder();

         for (ExtractedResult res : getResultSet()) {
             builder.append(res.getString());
             builder.append("\n");
         }

         return builder.toString();
     }

     /**
      * 15 lines
      * result
      * 15 more lines
      *
      * @param resultIndex
      * @return
      */
     public String getPreview(int resultIndex) {
         if (resultSet.isEmpty()) {
             return "";
         }

         int allLinesIndex = preview.get(getResultSet().get(resultIndex).getString());

         int lower = allLinesIndex - 15;
         int upper = allLinesIndex + 15;

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

     public List<ExtractedResult> getResultSet() {
         return resultSet;
     }

     public String getResultCount() {
         return Integer.toString(getResultSet().size());
     }

     public void removeUpdate() {
         // escape pressed, maybe do optimization
     }

     public String toString() {
         for (ExtractedResult res : getResultSet()) {
             System.out.println(res.getString());
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
         System.out.println("Search");
         FileSearch search = new FileSearch();
         search.testCrawl(testLoad());
         search.search("set");
         System.out.println(search.getResults());
         System.out.println(search.getPreview(0));
         System.out.println(search.getResultCount());
     }
 }