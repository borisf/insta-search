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

 import java.nio.file.FileSystems;
 import java.nio.file.PathMatcher;

 public class BinaryToTextTranslator {

     public static final PathMatcher MATCHER  =
             FileSystems.getDefault().getPathMatcher("glob:**.{java,kt,md,h,c,cpp,gradle,rs,txt,cs}");


    public static void calcTempoExtension(String fileName) {
        String extension = "";

        int i = fileName.indexOf('.');
        if (i > 0) {
            extension = fileName.substring(i+1);
        }
    }

    public static void main(String[] args) {
        String str = "two";
        switch(str)
        {
            case "one":
                System.out.println("one");
                break;
            case "two":
                System.out.println("two");
                break;
            case "three":
                System.out.println("three");
                break;
            default:
                System.out.println("no match");
        }
    }

}
