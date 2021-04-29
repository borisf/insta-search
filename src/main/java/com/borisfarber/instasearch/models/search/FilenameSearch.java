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
 import com.borisfarber.instasearch.contollers.IgnoreList;
 import com.borisfarber.instasearch.models.text.FilenameAndLineNumber;
 import com.illucit.instatrie.index.PrefixIndex;
 import com.illucit.instatrie.index.TriePrefixIndex;
 import com.illucit.instatrie.splitter.StringWordSplitter;

 import javax.swing.*;
 import java.io.File;
 import java.io.IOException;
 import java.nio.file.*;
 import java.nio.file.attribute.BasicFileAttributes;
 import java.util.*;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;

 import static java.nio.file.FileVisitResult.CONTINUE;
 import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

 public class FilenameSearch implements Search {
     private final Mediator mediator;
     private final ExecutorService executorService =
             Executors.newSingleThreadExecutor();

     private final ArrayList<String> allLines;
     private final PrefixIndex<String> index =
             new TriePrefixIndex<>(StringWordSplitter.IdentityStringWordSplitter.instance());
     private List<String> searchResults = new LinkedList<>();
     private File searchRoot = new File("");

     public FilenameSearch(Mediator mediator) {
         this.mediator = mediator;
         this.allLines = new ArrayList<>();
     }

     @Override
     public void crawl(File file) {
         this.searchRoot = file;
         try {
             Files.walkFileTree(file.toPath(), new FilenameVisitor());
         } catch (IOException e) {
             e.printStackTrace();
         }

         index.createIndex(allLines);
         emptyQuery();
     }

     @Override
     public void search(String query) {
         executorService.execute(() -> {
             if (allLines.isEmpty()) {
                 return;
             }

             searchResults = index.search(query);
             Runnable runnable = mediator::onSearchFinish;
             SwingUtilities.invokeLater(runnable);
         });
     }

     @Override
     public LinkedList<FilenameAndLineNumber> getFilenamesAndLineNumbers(String line) {
         FilenameAndLineNumber pair = new FilenameAndLineNumber(line,Search.NOT_IN_FILE);
         LinkedList<FilenameAndLineNumber> result = new LinkedList<>();
         result.add(pair);
         return result;
     }

     @Override
     public String getPreview(String resultLine) {
         return resultLine;
     }

     @Override
     public List<String> getResults() {
         return searchResults;
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
         Runnable runnable = () -> {
             ArrayList<String> allFiles = new ArrayList<>();
             allFiles.addAll(allLines);
             mediator.onCrawlFinish(allFiles);
         };
         SwingUtilities.invokeLater(runnable);
     }

     @Override
     public Comparator<String> getResultsSorter() {
         return (s, t1) -> 1;
     }

     private class FilenameVisitor extends SimpleFileVisitor<Path> {
         IgnoreList ignoreList;

         FilenameVisitor() {
             super();
             ignoreList = new IgnoreList();
         }

         @Override
         public FileVisitResult preVisitDirectory(Path dir,
                                                  BasicFileAttributes attrs) {
             String currentDir = dir.getFileName().toString();

             if(ignoreList.contains(dir.toAbsolutePath().toString())) {
                 return SKIP_SUBTREE;
             }
             
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
         public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
             allLines.add(path.toString());
             return CONTINUE;
         }

         @Override
         public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
             if (exc instanceof AccessDeniedException) {
                 return FileVisitResult.SKIP_SUBTREE;
             }

             return super.visitFileFailed(file, exc);
         }
     }
 }
