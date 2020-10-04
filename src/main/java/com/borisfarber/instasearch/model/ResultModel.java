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
 package com.borisfarber.instasearch.model;

 import com.borisfarber.instasearch.search.Search;

 import java.util.ArrayList;
 import java.util.LinkedList;
 import java.util.List;
 import static com.borisfarber.instasearch.ui.Controller.UI_VIEW_LIMIT;

 // line format: name, line num, line  and selection with ==>
 public class ResultModel {
     private static final String SELECTOR = "==> ";

     private final ArrayList<String> searchResults = new ArrayList<>();
     private int searchResultsCount = 0;
     public final Pair<String, Integer> selectedFilenameAndPosition =
             new Pair<>("test.txt",0);
     public int selectedSearchResultIndex = 0;
     private String selectedLine = "";

     // TODO from here external ones

     public StringBuilder builder;

     public ResultModel() {

     }

     public void selectedLineUp() {
         if(selectedSearchResultIndex > 0) {
             selectedSearchResultIndex--;
         }
     }

     // TODO may be convert search to a field, fits the empty query
     public void selectedLineDown(Search search) {
         if((selectedSearchResultIndex <
                 (Integer.parseInt(search.getResultSetCount()) - 1))
                 && selectedSearchResultIndex < (
                 searchResultsCount - 1)) {
             selectedSearchResultIndex++;
         }
     }

     public void lineSelected(String selectedText) {

         int index = searchResults.indexOf((selectedText + "\n"));

         if(index == -1) {
             //TODO clean up
             // hack when the ui screen is small and the selected line
             // is smaller than the result line
             return;
         }

         selectedSearchResultIndex = index;
     }

     public void lineClicked(String selectedText) {
         String[] parts  = selectedText.split(":");
         String fileName = parts[0];
         String position = parts[1];

         if(fileName.startsWith(SELECTOR)) {
             fileName = fileName.substring(4);
         }

         selectedFilenameAndPosition.t = fileName;
         selectedFilenameAndPosition.u = Integer.parseInt(position);
     }

     public void setCrawl(String toString) {
         // search finishes crawling

         // todo add stuff to search results
     }

     public void prepareResults(Search search) {
         int resultCount = 0;
         boolean isViewLimitReached = false;
         LinkedList<Pair<String, Integer>> locations;
         searchResults.clear();
         List<String> rawResults = search.getResults();

         while ((resultCount < rawResults.size()) && !isViewLimitReached) {
             String rawLine = rawResults.get(resultCount);

             if(rawLine == null) {
                 // if the UI is drawing the previous search results
                 // while the results are updated
                 return;
             }

             locations = search.getFileNameAndPosition(rawLine);

             for(Pair<String, Integer> location : locations) {
                 String result = location.t + ":" + location.u +":"
                         + rawLine;

                 if(!rawLine.endsWith("\n")) {
                     // optimization GrepSearch lines come with \n
                     // don't want to change the logic there for performance
                     result += "\n";
                 }

                 searchResults.add(result);
                 resultCount++;

                 if(resultCount >= UI_VIEW_LIMIT) {
                     isViewLimitReached = true;
                     break;
                 }
             }
         }

         searchResults.sort(search.getResultsSorter());
         searchResultsCount = resultCount;
     }

     public void updateSelection() {
         int previewLinesIndex = 0;
         builder = new StringBuilder();
         for (String str : searchResults) {
             if (previewLinesIndex == selectedSearchResultIndex) {
                 builder.append(SELECTOR).append(str);
                 String[] parts = str.split(":");
                 String fileName = parts[0];
                 String position = parts[1];

                 // the text line starts after 2 :s
                 this.selectedLine = str.substring(parts[0].length() + parts[1].length() + 2);

                 selectedFilenameAndPosition.t = fileName;
                 selectedFilenameAndPosition.u = Integer.parseInt(position);
             } else {
                 builder.append(str);
             }
             previewLinesIndex++;
         }
     }

     public int getSelectorIndex() {
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
 }
