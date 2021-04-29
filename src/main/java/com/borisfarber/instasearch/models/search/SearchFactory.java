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

import java.io.File;
import java.nio.file.Path;

import static com.borisfarber.instasearch.models.search.Search.CONTENT_SEARCH;

public enum SearchFactory {

    INSTANCE;

    public Search createMockSearch(Mediator mediator) {
        return new MockSearch(mediator);
    }

    public Search createSearch(Mediator mediator, File newFile, String mode) {
        if (newFile.isDirectory()) {
            if(mode.equals(CONTENT_SEARCH)) {
                return new ContentSearch(mediator);
            } else {
                return new FilenameSearch(mediator);
            }
        } else {
            if (PathMatchers.ZIP_MATCHER.matches(Path.of(newFile.toURI()))) {
                return new ZipSearch(newFile, mediator);
            } else if (PathMatchers.APK_MATCHER.matches(Path.of(newFile.toURI()))) {
                return new APKSearch(newFile, mediator);
            } else {
                return new BigFileSearch(mediator);
            }
        }
    }
}
