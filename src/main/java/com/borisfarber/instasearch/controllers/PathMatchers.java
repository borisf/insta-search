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

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;

public class PathMatchers {
    public static final PathMatcher SOURCE_MATCHER =
            FileSystems.getDefault().getPathMatcher("glob:**.{java,kt,md,h,c,cpp,gradle,rs,cs}");
    public static final PathMatcher SOURCE_OR_TEXT_PATH_MATCHER =
            FileSystems.getDefault().getPathMatcher("glob:**.{java,kt,md,h,c,cpp,gradle,rs,txt,cs}");
    public static final PathMatcher CLASS_MATCHER =
            FileSystems.getDefault().getPathMatcher("glob:**.{class}");
    public static final PathMatcher ZIP_MATCHER =
            FileSystems.getDefault().getPathMatcher("glob:**.{zip,jar}");
    public static final PathMatcher APK_MATCHER =
            FileSystems.getDefault().getPathMatcher("glob:**.{apk}");
    public static final PathMatcher DEX_MATCHER =
            FileSystems.getDefault().getPathMatcher("glob:**.{dex}");

    private PathMatchers() {

    }
}
