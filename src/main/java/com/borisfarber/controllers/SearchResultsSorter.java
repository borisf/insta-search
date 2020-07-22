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
import java.util.Comparator;

public class ResultsSorter implements Comparator<String> {
    //  "log.txt:2424:something";

    @Override
    public int compare(String s1, String s2) {
        String[] s1Parts = s1.split(":");
        String[] s2Parts = s2.split(":");
        if(!s1Parts[0].equals(s2Parts[0])) {
            return s1Parts[0].compareTo(s2Parts[0]);
        }
        return Integer.valueOf(s1Parts[1]).compareTo(Integer.valueOf(s2Parts[1]));
    }
}
