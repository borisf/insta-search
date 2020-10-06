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
 package com.borisfarber.instasearch.textmodels;

 import com.borisfarber.instasearch.search.Search;

 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.LinkedList;
 import java.util.List;

 import static com.borisfarber.instasearch.ui.Controller.UI_VIEW_LIMIT;

 // line format: name, line num, line  and selection with ==>
 public class ResultModel {
     private static final String SELECTOR = "==> ";
     private final Pair<String, Integer> selectedFilenameAndPosition =
             new Pair<>("test.txt", 0);
     private final ArrayList<String> searchResults = new ArrayList<>();
     public int selectedSearchResultIndex = 0;
     private StringBuilder builder;
     private int searchResultsCount = 0;
     private boolean isFullSearch = false;
     private String selectedLine;

     // TODO big thing, fix all search previews for non : lines
     public ResultModel() {

     }

     public void selectedLineUp() {
         if (selectedSearchResultIndex > 0) {
             selectedSearchResultIndex--;
         }
     }

     // TODO may be convert search to a field, fits the empty query
     public void selectedLineDown(Search search) {

         // TODO fix the lower limit
         if ((/*selectedSearchResultIndex <
                 (Integer.parseInt(search.getResultSetCount()) - 1))
                 &&*/ selectedSearchResultIndex < (
                 searchResultsCount - 1))) {
             selectedSearchResultIndex++;
         }
     }

     public void lineSelected(String selectedText) {
         // TODO think of moving to ResultModel
         if(selectedText.endsWith("\n")) {
             selectedText = selectedText.substring(0, selectedText.length() - 1);
         }

         // TODO not sure need this, follow up
         int index = searchResults.indexOf((selectedText + "\n"));

         if (index == -1) {
             //TODO clean up
             // hack when the ui screen is small and the selected line
             // is smaller than the result line
             return;
         }

         selectedSearchResultIndex = index;
     }

     public void lineClicked(String selectedText) {
         if(selectedText.endsWith("\n")) {
             selectedText = selectedText.substring(0, selectedText.length() - 1);
         }

         if (selectedText.indexOf(":") < 0) {

             String fileName = selectedText;
             if (selectedText.startsWith(SELECTOR)) {
                 fileName = fileName.substring(4);
             }

             selectedFilenameAndPosition.t = fileName;
             selectedFilenameAndPosition.u = 0;
             return;
         }

         String[] parts = selectedText.split(":");
         String fileName = parts[0];
         String position = parts[1];

         if (fileName.startsWith(SELECTOR)) {
             fileName = fileName.substring(4);
         }

         selectedFilenameAndPosition.t = fileName;
         selectedFilenameAndPosition.u = Integer.parseInt(position);
     }

     public void crawlFinished(List<String> crawlResults) {
         isFullSearch = false;
         searchResults.clear();

         // TODO paginator
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

     public void searchFinished(Search search) {
         isFullSearch = true;

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

     public void generateResultView() {
         int previewLinesIndex = 0;
         builder = new StringBuilder();
         for (String str : searchResults) {
             if (previewLinesIndex == selectedSearchResultIndex) {
                 builder.append(SELECTOR).append(str);

                 if (str.indexOf(":") > 0) {

                     String[] parts = str.split(":");
                     String fileName = parts[0];
                     String position = parts[1];

                     // the text line starts after 2 :s
                     this.selectedLine = str.substring(parts[0].length() + parts[1].length() + 2);

                     selectedFilenameAndPosition.t = fileName;
                     selectedFilenameAndPosition.u = Integer.parseInt(position);
                 } else {
                     this.selectedLine = str;
                     selectedFilenameAndPosition.t = str;
                     selectedFilenameAndPosition.u = 0;
                 }
             } else {
                 builder.append(str);
             }
             previewLinesIndex++;
         }
     }

     public String getResultView() {
         return builder.toString();
     }

     public int getSelectionIndex() {
         return builder.toString().indexOf(SELECTOR);
     }

     public String getSelectedLine() {
         return searchResults.get(selectedSearchResultIndex);
     }

     public int getResultCount() {
         return searchResultsCount;
     }

     public int resultSize() {
         return searchResults.size();
     }

     public String getSelectedFilename() {
        String result = selectedFilenameAndPosition.t;

        if(result.endsWith("\n")) {
            result = result.substring(0, result.length() - 1);
        }

         return result;
     }

     public Integer getSelectedPosition() {
         return selectedFilenameAndPosition.u;
     }

     public static String fromUItoText(String UIText) {


         return "";
     }
 }