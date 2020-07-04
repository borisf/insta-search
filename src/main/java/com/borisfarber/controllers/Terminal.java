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

import java.io.IOException;

public class Terminal {
    static int executeInTerminal(String command) throws IOException, InterruptedException {
        final String[] wrappedCommand;
        //if (isWindows) {
        //    wrappedCommand = new String[]{ "cmd", "/c", "start", "/wait", "cmd.exe", "/K", command };
        //}
        //else if (isLinux) {
        // TODO works
        wrappedCommand = new String[]{ "gnome-terminal", "-e", command};
        // }
        //else if (isMac) {
        //    wrappedCommand = new String[]{"osascript",
        //            "-e", "tell application \"Terminal\" to activate",
        //            "-e", "tell application \"Terminal\" to do script \"" + command + ";exit\""};
        //}
        //else {
        //  throw new RuntimeException("Unsupported OS ☹");
        //}

        Process process = new ProcessBuilder(wrappedCommand)
                .redirectErrorStream(true)
                .start();

        // TODO follow up with errors or exception ?
        return process.waitFor();
    }
}
