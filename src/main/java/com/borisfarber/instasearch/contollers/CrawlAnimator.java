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
package com.borisfarber.instasearch.contollers;

import javax.swing.*;

public class CrawlAnimator implements Runnable{
    private final Controller controller;
    private int uiCounter;

    public CrawlAnimator(Controller controller) {
        this.controller = controller;
        this.uiCounter = 0;
    }

    @Override
    public void run() {
        String text = "";
        for (int j = 0; j < uiCounter; j++) {
            text += "==";
        }

        uiCounter++;
        text += ">";

        String finalText = text;
        SwingUtilities.invokeLater(() ->
                this.controller.setResultText(finalText));
    }
}
