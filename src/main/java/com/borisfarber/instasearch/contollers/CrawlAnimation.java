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

public class CrawlAnimation implements Runnable {
    private final Mediator mediator;
    private final String absolutePath;
    private int uiCounter;

    public CrawlAnimation(Mediator mediator, String absolutePath) {
        this.mediator = mediator;
        this.uiCounter = 0;
        this.absolutePath = absolutePath;
    }

    @Override
    public void run() {
        StringBuilder textBuilder = new StringBuilder();
        textBuilder.append(absolutePath);

        textBuilder.append("\nYour results will appear here shortly, crawling   ");

        for (int j = 0; j < uiCounter; j++) {
            if(j % 12 == 0) {
                textBuilder.append("\n");
            }
            textBuilder.append(". . ");
        }
        uiCounter++;
        textBuilder.append("><)))'>");

        String finalText = textBuilder.toString();
        SwingUtilities.invokeLater(() ->
                this.mediator.onCrawlUpdate(finalText));
    }
}
