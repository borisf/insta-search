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
 package com.borisfarber.instasearch.controllers;

 import java.io.File;
 import java.io.IOException;
 import java.nio.file.*;
 import java.nio.file.attribute.BasicFileAttributes;
 import java.nio.file.attribute.PosixFileAttributes;
 import java.nio.file.attribute.PosixFilePermission;
 import java.util.Comparator;
 import java.util.Set;

 import static java.nio.file.FileVisitResult.CONTINUE;
 import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

 public enum PrivateFolder {

     INSTANCE;

     private String rootPath;

     PrivateFolder() {
         try {
             rootPath = System.getProperty("user.home");
             rootPath += File.separator + ".instasearch";
             File customDir = new File(rootPath);

             if (customDir.exists()) {
                 System.out.println(customDir + " already exists");
             } else if (customDir.mkdirs()) {
                 System.out.println(customDir + " was created");
             } else {
                 System.out.println(customDir + " was not created");
             }

             setPosixPermissions(customDir);
         } catch(IOException e) {

         }
     }

     public static boolean isSourceFolder(File folder) {
         Path pathString = folder.toPath();
         PathMatcher matcher = Controller.SOURCE_MATCHER;

         final int[] allFiles = {0};
         final int[] srcFiles = {0};
         final boolean[] largeTextFile = {false};

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
                 public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                     allFiles[0]++;
                     if (matcher.matches(path)) {
                        srcFiles[0]++;
                     }

                     if(path.endsWith(".txt")) {
                         // no more than 50 KB
                         if(Files.size(path) > 50 * 1000) {
                             largeTextFile[0] = true;
                         }
                     }
                     return CONTINUE;
                 }
             });
         } catch (IOException e) {
             e.printStackTrace();
         }

         if(largeTextFile[0]) {
             return false;
         }

         return srcFiles[0] > 0;
     }

     public File getTempFile(String name, String ext) {
         return getTempFile(name + "." + ext);
     }

     public File getTempFile(String fileName) {
         File result = new File("");
         try {
             result = new File(rootPath + File.separator +  fileName);
             result.createNewFile();
             setPosixPermissions(result);
         } catch(IOException e) {
             e.printStackTrace();
         }
         return result;
     }

     public void shutdown() {
         try {
             Files.walk(Path.of(rootPath))
                     .sorted(Comparator.reverseOrder())
                     .map(Path::toFile)
                     .forEach(File::delete);
         } catch (IOException e) {
             e.printStackTrace();
         }
     }

     private void setPosixPermissions(File file) throws IOException {
         Set<PosixFilePermission> perms = Files.readAttributes(Path.of(file.toURI()), PosixFileAttributes.class).permissions();

         perms.add(PosixFilePermission.OWNER_WRITE);
         perms.add(PosixFilePermission.OWNER_READ);
         perms.add(PosixFilePermission.OWNER_EXECUTE);
         perms.add(PosixFilePermission.GROUP_WRITE);
         perms.add(PosixFilePermission.GROUP_READ);
         perms.add(PosixFilePermission.GROUP_EXECUTE);
         perms.add(PosixFilePermission.OTHERS_WRITE);
         perms.add(PosixFilePermission.OTHERS_READ);
         perms.add(PosixFilePermission.OTHERS_EXECUTE);
         Files.setPosixFilePermissions(Path.of(file.toURI()), perms);
     }
 }
