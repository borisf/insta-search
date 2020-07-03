# ClassyShark Insta Search

A lot of your time spent “writing” code is actually spent reading code. And a decent chunk of time spent
reading code is actually spent searching code. And we all work with a larger codebase and open source 
libraries (as third party dependencies) .

ClassyShark Insta Search is a fast, incremental [fuzzy](https://en.wikipedia.org/wiki/Approximate_string_matching) search in folders or in large log file.

With ClassyShark Insta Search you can easily and quickly find and navigate
to any text string or regex inside any file in your folders. All done
incrementally.

![Image of ClassySearch](https://github.com/borisf/classysearch/blob/master/images/ClassySharkInstaSearch.png)

## UI
* Search line - where you type text that are you looking for

* Results view - where you see file set that include the text from
the search line

* File view - where you see the file opened on the specific line from the
results view

## Search and Navigate

### In folders

1. Open your root folder by pressing right arrow
2. Start typing text in the search line
3. Inspect results in results view
4. Click on the relevant line
5. See the file in the file view, opened on the line from above

### In file

1. Drap and drop your file into the results view
2. Start typing text in the search line
3. Inspect results in the results view
4. Click on the relevant line
5. See the file in the file view, opened on the line from
the results view

### Folder from command line
When you call ClassyShark Insta Search from command line it will load 
and index the current folder (pwd).


**This is not an official Google product.**

### License

```
Copyright 2017 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License
is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
or implied. See the License for the specific language governing permissions and limitations under
the License.
```
