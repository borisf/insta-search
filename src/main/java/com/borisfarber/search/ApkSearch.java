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
 package com.borisfarber.search;

 import com.borisfarber.controllers.Controller;
 import com.borisfarber.data.Pair;
 import me.xdrop.fuzzywuzzy.model.ExtractedResult;
 import org.zeroturnaround.zip.ZipUtil;

 import javax.swing.*;
 import java.io.File;
 import java.io.IOException;
 import java.nio.file.Files;
 import java.nio.file.Path;
 import java.nio.file.attribute.PosixFileAttributes;
 import java.nio.file.attribute.PosixFilePermission;
 import java.nio.file.attribute.PosixFilePermissions;
 import java.util.*;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;

 public class ApkSearch implements Search {
     private final Controller controller;
     private File zipFile;
     private final ArrayList<String> allLines = new ArrayList<>();
     private ExecutorService executorService =
             Executors.newSingleThreadExecutor();
     private List<ExtractedResult> resultSet = new ArrayList<>();

     public ApkSearch(File newFile, Controller controller) {
         this.zipFile = newFile;
         this.controller = controller;
         allLines.clear();
     }

     @Override
     public void crawl(File file) {
         this.zipFile = file;
         allLines.clear();
         resultSet.clear();
         ZipUtil.iterate(file, zipEntry -> allLines.add(zipEntry.getName()));
         emptyQuery();
     }

     @Override
     public void search(String query) {
         executorService.execute(() -> {
             if(allLines.isEmpty()) {
                 resultSet = new LinkedList<>();
                 return;
             }

             resultSet = me.xdrop.fuzzywuzzy.FuzzySearch.extractSorted(query, allLines, 50);
             Runnable runnable = () -> controller.onUpdateGUI();
             SwingUtilities.invokeLater(runnable);
         });
     }

     @Override
     public LinkedList<Pair<String, Integer>> getFileNameAndPosition(String line) {
         LinkedList<Pair<String, Integer>> result = new LinkedList<>();

         Pair<String, Integer> pair = new Pair(line, 0);
         result.add(pair);
         return result;
     }

     @Override
     public String getResults() {
         return null;
     }

     @Override
     public String getPreview(String resultLine) {
         if (resultLine.isEmpty()) {
             return "";
         }

         String[] parts  = resultLine.split(":");
         String fileName = parts[0];
         String line = parts[2];

         // remove the new line in the end
         line = line.substring(0, line.length()-1);
         byte[] bytes = ZipUtil.unpackEntry(zipFile, line);

         String result = new String(Arrays.copyOfRange(bytes, 0, 360));
         return result;
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
     public Path getPathPerFileName(String fileName) {
         File tempFile = null;
         try {
             String path = System.getProperty("user.home");
             path += File.separator + ".instasearch";
             File customDir = new File(path);

             if (customDir.exists()) {
                 System.out.println(customDir + " already exists");
             } else if (customDir.mkdirs()) {
                 System.out.println(customDir + " was created");
             } else {
                 System.out.println(customDir + " was not created");
             }

             Set<PosixFilePermission> perms = Files.readAttributes(Path.of(customDir.toURI()), PosixFileAttributes.class).permissions();

             System.out.format("Permissions before: %s%n",  PosixFilePermissions.toString(perms));

             perms.add(PosixFilePermission.OWNER_WRITE);
             perms.add(PosixFilePermission.OWNER_READ);
             perms.add(PosixFilePermission.OWNER_EXECUTE);
             perms.add(PosixFilePermission.GROUP_WRITE);
             perms.add(PosixFilePermission.GROUP_READ);
             perms.add(PosixFilePermission.GROUP_EXECUTE);
             perms.add(PosixFilePermission.OTHERS_WRITE);
             perms.add(PosixFilePermission.OTHERS_READ);
             perms.add(PosixFilePermission.OTHERS_EXECUTE);
             Files.setPosixFilePermissions(Path.of(customDir.toURI()), perms);


             File tempFile1 = new File(customDir + File.separator +  fileName);

             String nnn = tempFile1.getName();

             tempFile = new File(customDir + File.separator +  nnn);
             System.out.println("create file:" + tempFile.createNewFile());


             perms = Files.readAttributes(Path.of(tempFile.toURI()), PosixFileAttributes.class).permissions();

             System.out.format("Permissions before: %s%n",  PosixFilePermissions.toString(perms));

             perms.add(PosixFilePermission.OWNER_WRITE);
             perms.add(PosixFilePermission.OWNER_READ);
             perms.add(PosixFilePermission.OWNER_EXECUTE);
             perms.add(PosixFilePermission.GROUP_WRITE);
             perms.add(PosixFilePermission.GROUP_READ);
             perms.add(PosixFilePermission.GROUP_EXECUTE);
             perms.add(PosixFilePermission.OTHERS_WRITE);
             perms.add(PosixFilePermission.OTHERS_READ);
             perms.add(PosixFilePermission.OTHERS_EXECUTE);
             Files.setPosixFilePermissions(Path.of(tempFile.toURI()), perms);

             ZipUtil.unpackEntry(zipFile, fileName, tempFile);
         } catch (IOException e) {
             e.printStackTrace();
         }

         // TODO fix this
         return Path.of(tempFile.toURI());
     }

     @Override
     public void testCrawl(ArrayList<String> testLoad) {

     }

     @Override
     public void close() {
         executorService.shutdown();

         // TODO delete the folder in controller close
     }

     @Override
     public void emptyQuery() {
         Runnable runnable = () -> {
             StringBuilder builder = new StringBuilder();
             for (String fileName : allLines) {
                 builder.append(fileName + "\n");
             }

             controller.resultTextPane.setText(builder.toString());
         };
         SwingUtilities.invokeLater(runnable);
     }

     public static void main(String[] args) {
         ApkSearch as = new ApkSearch(new File("/home/bfarber/Desktop/shark_v01.apk"), null);
         as.crawl(new File("/home/bfarber/Desktop/shark_v01.apk"));
     }
 }
