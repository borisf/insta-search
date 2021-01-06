/*
 * Copyright 2021 Google, Inc.
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
package com.borisfarber.instasearch.contollers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import static java.nio.file.Files.readAllLines;

public class IgnoreList {
    List<String> strRules = new LinkedList<>();

    public IgnoreList() {
        convertGitIgnoreToRules();
    }

    public boolean contains(String filename) {
        return strRules.contains(filename);
    }

    private void convertGitIgnoreToRules() {
        try {
            strRules = readAllLines(Path.of("ignore.txt"));
        } catch (IOException e) {
           strRules = new LinkedList<>();
            try {
                new File("ignore.txt").createNewFile();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}
