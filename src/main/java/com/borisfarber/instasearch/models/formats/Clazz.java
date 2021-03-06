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
package com.borisfarber.instasearch.models.formats;

import com.borisfarber.instasearch.contollers.PrivateFolder;
import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.PlainTextOutput;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

 public class Clazz {

    public static BinaryFileModel.DecompilationModel decompile(Path selectedPath) {
        BinaryFileModel.DecompilationModel result = new BinaryFileModel.DecompilationModel();

        String fileNameWithoutExt =
                new File(selectedPath.toString()).
                        getName().replaceFirst("[.][^.]+$", "");
        String ext = "java";
        StringWriter writer = new StringWriter();

        try {
            PlainTextOutput pto = new PlainTextOutput(writer);
            Decompiler.decompile(selectedPath.toString(), pto);

            String content = writer.toString();

            File javaFile = PrivateFolder.INSTANCE.getTempFile(fileNameWithoutExt, ext);
            try (PrintWriter out = new PrintWriter(javaFile)) {
                out.println(content);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            result.text = content;
            result.fileName = javaFile;

        } finally {
            writer.flush();
        }

        return result;
    }
}
