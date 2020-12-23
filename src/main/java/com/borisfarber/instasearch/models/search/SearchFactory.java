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

import com.borisfarber.instasearch.contollers.Controller;
import com.borisfarber.instasearch.contollers.PathMatchers;
import com.borisfarber.instasearch.contollers.PrivateFolder;

import java.io.File;
import java.nio.file.Path;

public enum SearchFactory {

    INSTANCE;

    public Search createMockSearch(Controller controller) {
        return new MockSearch(controller);
    }

    public Search createSearch(File newFile, Controller controller) {
        if (newFile.isDirectory()) {
            if(PrivateFolder.isSourceFolder(newFile)) {
                return new FolderSearch(controller);
            } else {
                return new FilenameSearch(controller);
            }
        } else {
            if (PathMatchers.ZIP_MATCHER.matches(Path.of(newFile.toURI()))) {
                return new ZipSearch(newFile, controller);
            } else if (PathMatchers.APK_MATCHER.matches(Path.of(newFile.toURI()))) {
                return new APKSearch(newFile, controller);
            } else {
                return new BigFileSearch(controller);
            }
        }
    }
}
