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
 import java.nio.file.FileSystems;
 import java.nio.file.Files;
 import java.nio.file.Path;
 import java.nio.file.PathMatcher;
 import java.nio.file.attribute.PosixFileAttributes;
 import java.nio.file.attribute.PosixFilePermission;
 import java.util.Comparator;
 import java.util.Set;

 public enum PrivateFolder {

     INSTANCE;

     public static final PathMatcher SOURCE_MATCHER =
             FileSystems.getDefault().getPathMatcher("glob:**.{java,kt,md,h,c,cpp,gradle,rs,txt,cs}");

     public static final PathMatcher CLASS_MATCHER =
             FileSystems.getDefault().getPathMatcher("glob:**.{class}");

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
