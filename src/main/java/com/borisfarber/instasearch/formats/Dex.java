/*
 * Copyright 2015 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.borisfarber.instasearch.formats;

import com.borisfarber.instasearch.controllers.Pair;
import com.borisfarber.instasearch.controllers.PrivateFolder;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.reference.DexBackedStringReference;
import org.jf.dexlib2.iface.DexFile;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Dex {
    public static Pair<File, String> decompile(Path selectedPath) {
        Pair<File, String> result = new Pair<>(new File(""), "");

        String fileNameWithoutExt =
                new File(selectedPath.toString()).
                        getName().replaceFirst("[.][^.]+$", "");

        String ext = "txt";
        File resultPrivateFile = PrivateFolder.INSTANCE.getTempFile(fileNameWithoutExt, ext);
        BufferedWriter out = null;
        String allStrings = "";

        try {
            out = new BufferedWriter(
                    new FileWriter(resultPrivateFile, false));
            allStrings = dumpStrings(selectedPath.toFile());
            out.write(allStrings );

        } catch (IOException e) {
            try {
                out.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

        result.u = allStrings.substring(0, 120) + "/n/n...";
        result.t = resultPrivateFile;

        return result;
    }

    private static String dumpStrings(File apkFile) {
        File file;
        ZipInputStream zipFile;
        StringBuffer buffer = new StringBuffer();

        try {
            zipFile = new ZipInputStream(new FileInputStream(apkFile));
            ZipEntry zipEntry;
            int i = 0;
            while (true) {
                zipEntry = zipFile.getNextEntry();

                if (zipEntry == null) {
                    break;
                }

                if (zipEntry.getName().endsWith(".dex")) {
                    file =  PrivateFolder.INSTANCE.getTempFile("classes" + i, "dex");
                    file.deleteOnExit();
                    i++;

                    FileOutputStream fos = new FileOutputStream(file);
                    byte[] bytes = new byte[1024];
                    int length;
                    while ((length = zipFile.read(bytes)) >= 0) {
                        fos.write(bytes, 0, length);
                    }

                    fos.close();

                    DexFile dxFile = DexFileFactory.loadDexFile(file, Opcodes.getDefault());
                    DexBackedDexFile dataPack = (DexBackedDexFile) dxFile;

                    List<DexBackedStringReference> stringList = dataPack.getStringReferences();
                    buffer.append("classes" + i + ".dex\n");

                    for(DexBackedStringReference str : stringList) {
                        buffer.append(str.getString() + "\n");
                    }

                    file.delete();
                }
            }
            zipFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }

    public static void main(String[] args) throws Exception {
        File path = new File(System.getProperty("user.home") +
                "/Desktop/test");

        // Open given file in append mode.
        BufferedWriter out = new BufferedWriter(
                new FileWriter("dump.txt", true));

        File [] files = path.listFiles();
        for (int i = 0; i < files.length; i++){
            if (files[i].isFile()){ //this line weeds out other directories/folders
                // Open given file in append mode.
                out.write(dumpStrings(files[i]));
            }
        }

        out.close();
    }
}
