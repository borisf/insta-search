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

import org.eclipse.jgit.ignore.FastIgnoreRule;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static java.nio.file.Files.readAllLines;
// TODO git ignore for the content search --- file oriented
// TODO git ignore is both for files and folders (and not only folders)

public class IgnoreListGit {
    private final List<FastIgnoreRule> gitIgnoreRules;

    public IgnoreListGit() {
        gitIgnoreRules = new ArrayList<>();
        convertFromGitIgnore();
    }

    public boolean contains(String filename) {
        for (FastIgnoreRule fir : gitIgnoreRules) {
            if(fir.isMatch(filename, true)) {
                if(fir.getResult() == true) {
                    return true;
                }
            }
        }

        return false;
    }

    private void convertFromGitIgnore() {
        List<String> strRules = new LinkedList<>();

        try {
            strRules = readAllLines(Path.of(".gitignore"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(String strRule: strRules) {
            gitIgnoreRules.add(new FastIgnoreRule(strRule));
        }
    }
}
