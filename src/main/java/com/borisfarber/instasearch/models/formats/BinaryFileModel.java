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

import com.borisfarber.instasearch.contollers.PathMatchers;
import java.io.File;
import java.nio.file.Path;

public class BinaryFileModel {

    static class DecompilationModel {
        public File fileName;
        public String text;
    }

    private DecompilationModel decompilationModel;

    public  BinaryFileModel (Path viewPath, File file) {
        decompilationModel = new DecompilationModel();

        if (PathMatchers.CLASS_MATCHER.matches(viewPath)) {
            decompilationModel = Clazz.decompile(viewPath);
        } else if (PathMatchers.DEX_MATCHER.matches(viewPath)) {
            decompilationModel = Dex.decompile(file.toPath());
        } else if(PathMatchers.ANDROID_BINARY_XML_MATCHER.matches(viewPath)) {
            decompilationModel = BinaryXml.decompile(viewPath);
        } else {
            // TODO may be irrelevant ?
        }
    }

    public File getFileName() {
        return decompilationModel.fileName;
    }

    public String getText() {
        return decompilationModel.text;
    }
}
