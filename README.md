# Insta Search

Insta Search is a fast, incremental [fuzzy](https://en.wikipedia.org/wiki/Approximate_string_matching) 
search inside files, folders, APK/ZIP/Jar files.

A lot of your time spent on “writing” code on production systems is actually spent on reading code and 
logs.  And a decent chunk of time spent reading, is actually spent on searching.

With Insta Search you can easily and quickly find and navigate
to any text string or regex inside any file in your folders. All done
incrementally.

![Image of InstaSearch](https://github.com/borisf/insta-search/blob/master/images/InstaSearch.png)

### Components
* Search Line - the upper text box, where you type text that are you looking for

* Result View - the middle list, where you see the filtered results
the search line, use arrows or mouse to land to the relevant line

* Preview View - bottom text box, where you see the context near the selected line

* Editor - once found the relevant line, press Enter, or click a mouse, and the file 
will be opened on the relevant line within embedded or system defined editor.

### Howto
The Insta Search supports incremental searching in both files and folders: 

* Log File - Drag and drop your file into the results view

* Source Folder - Open folder via the menu, or run Insta Search inside the relevant folder. 

* APK/Zip File - Drag and drop your file into the results view. Insta Search will show the entries. 
Searching will result of filtering of the filenames. Going over the entries with app/down arrows will
show the selected entry in the preview. You will see AndroidManifest as text, and other binary files 
as a hex dump. For classes dex entries, you will see the strings dump. 

* Jar/Zip File with class files - Drag and drop your file into the results view. Insta Search 
will show the entries. Searching will result of filtering of the filenames. Going over the entries 
with app/down arrow or selecting with a mouse  will show the binary preview. Pressing Enter or double clicking  will open a decompiled class view.

* Binary File - InstaSearch will open its own hex editor in a separate window. 

### File Menu
When you click on File menu you will get the following:
* Open - will open a file selection dialog to open a window

* About - will show you the version

* Exit - exits the InstaSearch (or press ALT + F4)

### Command Line Arguments
* No args - InstaSearch will crawl recursively the current working folder (pwd) 

* File name - a large log file to crawl

### Download & Run
To run, grab the [latest JAR](https://github.com/borisf/insta-search/releases)
and run `java -jar insta-search.jar`

**This is not an official Google product.**

## License

```
Copyright 2020 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License
is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
or implied. See the License for the specific language governing permissions and limitations under
the License.
```
