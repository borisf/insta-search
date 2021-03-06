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

 import com.borisfarber.instasearch.contollers.Mediator;
 import com.borisfarber.instasearch.contollers.PathMatchers;
 import com.borisfarber.instasearch.models.text.FilenameAndLineNumber;
 import com.borisfarber.instasearch.models.text.ResultModel;

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
     private final Mediator mediator;
     // key design idea, no such thing file, it is recreated by line numbers
     private final ArrayList<String> allLines;
     private final TreeMap<String, Path> filenamesToPaths;
     private final ArrayList<LineCountAndFile> lineCountAndFiles;
     private final HashMap<String, List<Integer>> linesToLineNumbers;

     private final ExecutorService executorService =
             Executors.newSingleThreadExecutor();
     private Map<String, Float> matchedSet;
     private File searchRoot;

     public ContentSearch(Mediator mediator) {
         this.mediator = mediator;
         allLines = new ArrayList<>();
         matchedSet = new TreeMap<>();
         filenamesToPaths = new TreeMap<>();
         lineCountAndFiles = new ArrayList<>();
         linesToLineNumbers = new HashMap<>();
     }

     @Override
     public void crawl(File file) {
         allLines.clear();
         lineCountAndFiles.clear();
         filenamesToPaths.clear();
         linesToLineNumbers.clear();
         this.searchRoot = file;

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
                     String currentDir = dir.getFileName().toString();

                     if(currentDir.startsWith(".")) {
                         if (searchRoot.getAbsolutePath().contains(".")) {
                             return CONTINUE;
                         } else {
                             return SKIP_SUBTREE;
                         }
                     }

                     return CONTINUE;
                 }

                 @Override
                 public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                         throws IOException {
                     if (matcher.matches(path)) {
                         try {
                             List<String> allLinesPerFile = Files.readAllLines(path);
                             allLines.addAll(allLinesPerFile);
                             LineCountAndFile pair = new LineCountAndFile(allLinesPerFile.size(),
                                     path.getFileName().toString());
                             lineCountAndFiles.add(pair);
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
             if(linesToLineNumbers.containsKey(allLines.get(index))) {
                 linesToLineNumbers.get(allLines.get(index)).add(index);
             } else {
                 LinkedList<Integer> list = new LinkedList<>();
                 list.add(index);
                 linesToLineNumbers.put(allLines.get(index), list);
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
             Runnable runnable = mediator::onSearchFinish;
             SwingUtilities.invokeLater(runnable);
         });
     }

     @Override
     public LinkedList <FilenameAndLineNumber> getFilenamesAndLineNumbers(String line) {
         LinkedList <FilenameAndLineNumber> result = new LinkedList<>();

         for (Integer occ : linesToLineNumbers.get(line)) {
             FilenameAndLineNumber pair = getFileNameAndPositionFromLineIndex(occ);
             result.add(pair);
         }

         return result;
     }

     private FilenameAndLineNumber getFileNameAndPositionFromLineIndex(int index) {
         int base = 0;
         for (LineCountAndFile pair : lineCountAndFiles) {
             if ((index >= base) && index < (base + pair.lineCount - 1)) {
                 return new FilenameAndLineNumber(pair.filename, (index - base));
             }
             base += pair.lineCount;
         }
         return new FilenameAndLineNumber("", 0);
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
         FilenameAndLineNumber filenameAndLineNumber
                 = ResultModel.extractFilenameAndLineNumber(resultLine);
         String fileName = filenameAndLineNumber.fileName;
         int line = filenameAndLineNumber.lineNumber;

         if(!resultLine.contains(":")) {
             isFileInternals = false;
         }

         int bline = getFileBaseline(fileName);
         StringBuilder builder = new StringBuilder();
         int allLinesIndex = line + bline;
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

         for(LineCountAndFile fData : lineCountAndFiles) {
             if(fData.filename.equals(fileName)) {
                 break;
             }
             bline += fData.lineCount;
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
             ArrayList<String> allFiles = new ArrayList<>(filenamesToPaths.keySet());
             mediator.onCrawlFinish(allFiles);
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

     private static class LineCountAndFile {
         public String filename;
         public int lineCount;

         public LineCountAndFile(int lineCount, String filename) {
             this.filename = filename;
             this.lineCount = lineCount;
         }
     }
 }
