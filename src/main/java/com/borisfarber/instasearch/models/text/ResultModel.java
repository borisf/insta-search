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
 package com.borisfarber.instasearch.models.text;
 import com.borisfarber.instasearch.models.search.Search;

 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.LinkedList;
 import java.util.List;

 import static com.borisfarber.instasearch.ui.InstaSearch.UI_VIEW_LIMIT;

 /** 2 format lines: files and search, optional selector and mandatory
  * new line
  *
  * files   selector filename\n
  * search  selector filename:linenumber:result line\n
  *
  * Extract without the new line
  */
 public class ResultModel {
     private static final String SELECTOR = "==> ";

     // search results, always end with the new line
     private final ArrayList<String> searchResultLines;
     private StringBuilder presentation;

     // diff from search size because files might have duplicate lines
     private int searchResultsCount = 0;

     // selection
     private int selectedLineIndex = 0;

     // export
     private final FilenameAndLineNumber exportedFileAndLineIndex;

     public ResultModel() {
         this.searchResultLines = new ArrayList<>();
         this.exportedFileAndLineIndex =
                 new FilenameAndLineNumber("test.txt", 0);
     }

     public String getBackground() {
         return Background.INTRO;
     }

     public void fillFilenameResults(List<String> fileNameResults) {
         searchResultLines.clear();

         // pagination
         int pageLimit = 1000;

         if (pageLimit > fileNameResults.size()) {
             pageLimit = fileNameResults.size();
         }

         for (int i = 0; i < pageLimit; i++) {
             searchResultLines.add(fileNameResults.get(i) + "\n");
         }

         Arrays.sort(searchResultLines.toArray());
         searchResultsCount = pageLimit;
     }

     public void fillSearchResults(Search search) {
         int resultCount = 0;
         boolean isViewLimitReached = false;
         LinkedList<FilenameAndLineNumber> locations;
         searchResultLines.clear();
         List<String> rawResults = search.getResults();

         while ((resultCount < rawResults.size()) && !isViewLimitReached) {
             String rawLine = rawResults.get(resultCount);

             if (rawLine == null) {
                 // when the UI is drawing the previous search results
                 // while the results are updated
                 return;
             }

             locations = search.getFilenamesAndLineNumbers(rawLine);

             for (FilenameAndLineNumber location : locations) {
                 String result;

                 if(location.lineNumber != Search.NOT_IN_FILE) {
                     result = location.fileName + ":" + location.lineNumber + ":"
                             + rawLine;
                 } else {
                     result = rawLine;
                 }

                 if (!rawLine.endsWith("\n")) {
                     // optimization GrepSearch lines come with \n
                     // don't want to change the logic there for performance
                     result += "\n";
                 }

                 searchResultLines.add(result);
                 resultCount++;

                 if (resultCount >= UI_VIEW_LIMIT) {
                     isViewLimitReached = true;
                     break;
                 }
             }
         }

         searchResultLines.sort(search.getResultsSorter());
         searchResultsCount = resultCount;
     }

     public int getResultCount() {
         return searchResultsCount;
     }

     public void generateResultView() {
         int previewLinesIndex = 0;
         presentation = new StringBuilder();
         for (String str : searchResultLines) {
             if (previewLinesIndex == selectedLineIndex) {
                 presentation.append(SELECTOR).append(str);

                 FilenameAndLineNumber extract = extractFilenameAndLineNumber(str);
                 exportedFileAndLineIndex.fileName = extract.fileName;

                 try {
                     exportedFileAndLineIndex.lineNumber = extract.lineNumber;
                 } catch (NumberFormatException nfe) {
                     exportedFileAndLineIndex.lineNumber = 0;
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

     public void decreaseSelectedLine() {
         if (selectedLineIndex < (searchResultsCount - 1)) {
             selectedLineIndex++;
         }
     }

     public void setSelectedLine(String line) {
         // what comes from mouse click has no new line in the end
         int index = line.endsWith("\n") ?
                 searchResultLines.indexOf(line) : searchResultLines.indexOf((line + "\n"));

         if (index == -1) {
             // hack when the ui screen is small and the selected line
             // is smaller than the result line
             return;
         }

         selectedLineIndex = index;
     }

     public String getSelectedLine() {
         if(selectedLineIndex >= searchResultLines.size()) {
             // for zip dummy dirs
             return "";
         }

         return searchResultLines.get(selectedLineIndex);
     }

     public int getSelectionIndex() {
         return presentation.toString().indexOf(SELECTOR);
     }

     public void exportLine(String line) {
         if(line.endsWith("\n")) {
             line = line.substring(0, line.length() - 1);
         }

         if (!line.contains(":")) {
             String fileName = line;
             if (line.startsWith(SELECTOR)) {
                 fileName = fileName.substring(4);
             }

             exportedFileAndLineIndex.fileName = fileName;
             exportedFileAndLineIndex.lineNumber = 0;
         } else {
             String[] parts = line.split(":");
             String fileName = parts[0];
             String index = parts[1];

             if (fileName.startsWith(SELECTOR)) {
                 fileName = fileName.substring(4);
             }

             exportedFileAndLineIndex.fileName = fileName;
             exportedFileAndLineIndex.lineNumber = Integer.parseInt(index);
         }
     }

     public String getExportedFilename() {
         String result = exportedFileAndLineIndex.fileName;

         if(result.endsWith("\n")) {
             result = result.substring(0, result.length() - 1);
         }

         return result;
     }

     public Integer getExportedLineIndex() {
         return exportedFileAndLineIndex.lineNumber;
     }

     public static String extractPreviewLine(String line) {
         String result;
         if(line.indexOf(":") > 0) {
             String[] parts = line.split(":");
             result = parts[2];
         } else {
             result = line;
         }

         // remove the new line in the end
         if (result.endsWith("\n")) {
             result = result.substring(0, result.length() - 1);
         }

         return result;
     }

     public static int extractLineNumber(String line) {
         int lineNumInt = 0;

         if(line.indexOf(":") > 0) {
             String[] parts  = line.split(":");
             String lineNum = parts[1];
             // todo might be an issue with binary files
             lineNumInt = Integer.parseInt(lineNum);
         }

         return lineNumInt;
     }

     public static FilenameAndLineNumber extractFilenameAndLineNumber(String line) {
         String fileName;
         String lineNumber;

         if(line.indexOf(":") > 0) {
             String[] parts = line.split(":");
             fileName = parts[0];
             lineNumber = parts[1];
         } else {
             fileName = line;
             lineNumber = "0";
         }

         // remove the new line in the end
         if (lineNumber.endsWith("\n")) {
             lineNumber = lineNumber.substring(0, lineNumber.length() - 1);
         }

         if (fileName.endsWith("\n")) {
             fileName = fileName.substring(0, fileName.length() - 1);
         }

         return new FilenameAndLineNumber(fileName, Integer.parseInt(lineNumber));
     }
 }