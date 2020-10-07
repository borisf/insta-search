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
 package com.borisfarber.instasearch.models;

 import com.borisfarber.instasearch.models.search.Search;
 import com.borisfarber.instasearch.models.text.Background;

 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.LinkedList;
 import java.util.List;

 import static com.borisfarber.instasearch.contollers.Controller.UI_VIEW_LIMIT;

 public class ResultPresentation {
     private static final String SELECTOR = "==> ";

     // search results, always end with new line
     private final ArrayList<String> searchResults;

     // diff from search size because files might have duplicate lines
     private int searchResultsCount = 0;

     // inner presentation
     private StringBuilder presentation;

     // selection
     private int selectedLineIndex = 0;

     // export
     private  Pair<String, Integer> exportedFileAndLineIndex;

     public ResultPresentation() {
         this.searchResults = new ArrayList<>();
         this.exportedFileAndLineIndex =
                 new Pair<>("test.txt", 0);
     }

     public String getBackground() {
         return Background.INTRO;
     }

     public void fillFilenameResults(List<String> crawlResults) {
         searchResults.clear();

         // paginator
         int pageLimit = 1000;

         if (pageLimit > crawlResults.size()) {
             pageLimit = crawlResults.size();
         }

         for (int i = 0; i < pageLimit; i++) {
             searchResults.add(crawlResults.get(i) + "\n");
         }

         Arrays.sort(searchResults.toArray());
         searchResultsCount = pageLimit;
     }

     public void fillSearchResults(Search search) {
         int resultCount = 0;
         boolean isViewLimitReached = false;
         LinkedList<Pair<String, Integer>> locations;
         searchResults.clear();
         List<String> rawResults = search.getResults();

         while ((resultCount < rawResults.size()) && !isViewLimitReached) {
             String rawLine = rawResults.get(resultCount);

             if (rawLine == null) {
                 // if the UI is drawing the previous search results
                 // while the results are updated
                 return;
             }

             locations = search.getFileNameAndPosition(rawLine);

             for (Pair<String, Integer> location : locations) {
                 String result = location.t + ":" + location.u + ":"
                         + rawLine;

                 if (!rawLine.endsWith("\n")) {
                     // optimization GrepSearch lines come with \n
                     // don't want to change the logic there for performance
                     result += "\n";
                 }

                 searchResults.add(result);
                 resultCount++;

                 if (resultCount >= UI_VIEW_LIMIT) {
                     isViewLimitReached = true;
                     break;
                 }
             }
         }

         searchResults.sort(search.getResultsSorter());
         searchResultsCount = resultCount;
     }

     public int getResultCount() {
         return searchResultsCount;
     }

     public void generateResultView() {
         int previewLinesIndex = 0;
         presentation = new StringBuilder();
         for (String str : searchResults) {
             if (previewLinesIndex == selectedLineIndex) {
                 presentation.append(SELECTOR).append(str);

                 // TODO - move to export line

                 if (str.indexOf(":") > 0) {

                     String[] parts = str.split(":");
                     String fileName = parts[0];
                     String position = parts[1];

                     // the text line starts after 2 :s
                     exportedFileAndLineIndex.t = fileName;
                     exportedFileAndLineIndex.u = Integer.parseInt(position);
                 } else {
                     exportedFileAndLineIndex.t = str;
                     exportedFileAndLineIndex.u = 0;
                 }
             } else {
                 presentation.append(str);
             }
             previewLinesIndex++;
         }
     }

     public String getResultView() {
         return presentation.toString();
     }

     public void resetSelectedLine() {
         selectedLineIndex = 0;
     }

     public void increaseSelectedLine() {
         if (selectedLineIndex > 0) {
             selectedLineIndex--;
         }
     }

     public void decreaseSelectedLine(Search search) {
         // TODO may be convert search to a field, fits the empty query
         // TODO fix the lower limit
         if ((/*selectedSearchResultIndex <
                 (Integer.parseInt(search.getResultSetCount()) - 1))
                 &&*/ selectedLineIndex < (
                 searchResultsCount - 1))) {
             selectedLineIndex++;
         }
     }

     public void setSelectedLine(String line) {
         // what comes from mouse click has no new line in the end
         int index = line.endsWith("\n") ?
                 searchResults.indexOf(line) : searchResults.indexOf((line + "\n"));

         if (index == -1) {
             // hack when the ui screen is small and the selected line
             // is smaller than the result line
             return;
         }

         selectedLineIndex = index;
     }

     public String getSelectedLine() {
         if(selectedLineIndex >= searchResults.size()) {
             // for zip dummy dirs
             return "";
         }

         return searchResults.get(selectedLineIndex);
     }

     public int getSelectionIndex() {
         return presentation.toString().indexOf(SELECTOR);
     }

     public void exportLine(String line) {
         if(line.endsWith("\n")) {
             line = line.substring(0, line.length() - 1);
         }

         if (line.indexOf(":") < 0) {
             String fileName = line;
             if (line.startsWith(SELECTOR)) {
                 fileName = fileName.substring(4);
             }

             exportedFileAndLineIndex.t = fileName;
             exportedFileAndLineIndex.u = 0;
         } else {
             String[] parts = line.split(":");
             String fileName = parts[0];
             String index = parts[1];

             if (fileName.startsWith(SELECTOR)) {
                 fileName = fileName.substring(4);
             }

             exportedFileAndLineIndex.t = fileName;
             exportedFileAndLineIndex.u = Integer.parseInt(index);
         }
     }

     public String getExportedFilename() {
         String result = exportedFileAndLineIndex.t;

         if(result.endsWith("\n")) {
             result = result.substring(0, result.length() - 1);
         }

         return result;
     }

     public Integer getExportedLineIndex() {
         return exportedFileAndLineIndex.u;
     }

     // todo clean up, more cases with ':'s in such as time stamps
     public String getPreviewLineNoNewLine() {
         String previewLine = getSelectedLine();

         if(getSelectedLine().indexOf(":") <0 ) {
             return getSelectedLine();
         }

         String[] parts = previewLine.split(":");
         String fileName = parts[0];
         String position = parts[1];
         previewLine = parts[2];


         if(previewLine.endsWith("\n\n")) {
             previewLine = previewLine.substring(0, previewLine.length() - 2);
         }

         if(previewLine.endsWith("\n")) {
             previewLine = previewLine.substring(0, previewLine.length() - 1);
         }

         return previewLine;
     }

     // TODO only for ZIP - need to rething the line as shows double entries
     public static String getPreviewLineFromSelectedLineNoNewLine(String from) {
         String result;
         if(from.indexOf(":") > 0) {
             String[] parts = from.split(":");
             result = parts[2];
         } else {
             result = from;
         }

         // remove the new line in the end
         if (result.endsWith("\n")) {
             result = result.substring(0, result.length() - 1);
         }

         return result;
     }

     public static Pair<String, String> getFileNameLineNoNewLine(String from) {
         String fileName;
         String line;

         if(from.indexOf(":") > 0) {
             String[] parts = from.split(":");
             fileName = parts[0];
             line = parts[1];
         } else {
             fileName = from;
             line = "0";
         }

         // remove the new line in the end
         if (line.endsWith("\n")) {
             line = line.substring(0, line.length() - 1);
         }

         if (fileName.endsWith("\n")) {
             fileName = fileName.substring(0, fileName.length() - 1);
         }

         Pair<String, String> result = new Pair<>(fileName, line);
         return result;
     }

     public static int getLineNumber(String resultLine) {
         int lineNumInt = 0;

         if(resultLine.indexOf(":") > 0) {
             String[] parts  = resultLine.split(":");
             String lineNum = parts[1];
             lineNumInt = Integer.parseInt(lineNum);
         }

         return lineNumInt;
     }
 }