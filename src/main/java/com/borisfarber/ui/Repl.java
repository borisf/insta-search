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
package com.borisfarber.ui;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;

import com.borisfarber.controllers.Controller;

public class Repl {

    private Repl() {

    }

    public static void repl() {
        Controller controller = new Controller(new JTextPane(),
                new JTextArea(), new JLabel());
        controller.testCrawl();
        Reader inreader = new InputStreamReader(System.in);
        try {
            BufferedReader in = new BufferedReader(inreader);
            String str;
            System.out.print(">>>");
            while ((str = in.readLine()) != null) {
                controller.search(str);
                controller.dump();
                System.out.print(">>>");
            }
            in.close();
        } catch (Exception e) {
        }
    }
}