# Insta Search

Insta Search is a fast, incremental [fuzzy](https://en.wikipedia.org/wiki/Approximate_string_matching) 
search inside files, folders, APK/ZIP/Jar files.

A lot of your time spent on “writing” code on production systems is actually spent on reading code and 
logs. And a decent chunk of time spent reading, is actually spent on searching.

With Insta Search you can easily and quickly find and navigate
to any text string or regex inside any file in your folders. All done
incrementally.

![Image of InstaSearch](https://github.com/borisf/insta-search/blob/master/images/InstaSearch.png)

### Howto
*  Toolbar
     * Open folder file - pressing the icon will pop up folder chooser dialog to select a folder 
       to search
     * Select search mode from the following:  
         * Content - search for content inside files (grep)
         * Filenames - search for a file. ClassyShark Insta search will look for dot(hidden) files, 
           only if a hidden file was selected as root.
     * Ignore list - opens the `ignore.txt` from your home folder via the system editor. This file 
       holds the ignored paths` data while crawling for files.
     * About - shows the version number

* Search Line - the upper text box, where you type text that are you looking for

* Result View - the middle list, where you see the filtered results
the search line, use arrows or mouse to land to the relevant line

* Preview View - bottom text box, where you see the context near the selected line

* Editor - once found the relevant line, press Enter, or click a mouse, and the file 
will be opened on the relevant line within embedded or system defined editor.

### Search
The Insta Search supports incremental searching in both files and folders: 

* Log File - Drag and drop your file into the results view

* Folder - Open folder via the menu, and search either for content or for a file name. 

* APK/Jar/Zip File - Drag and drop your file into the results view. Insta Search will show the entries. 
Searching will result of filtering of the filenames. Going over the entries with app/down arrows will
show the selected entry in the preview: 
    * AndroidManifest.xml (APK) ==> text 
    * Classes dex (APK) ==> string dump
    * Class files (Jar) ==> decompiled class view 
    * Binary files ==> hex dump   

Pressing Enter or double-clicking on an entry will open its own hex editor in a separate window.

### Command Line Arguments

* File name - a large log file to crawl

### Download & Run
**Jar**

Grab the [latest JAR](https://github.com/borisf/insta-search/releases)
and run `java -jar insta-search.jar`

**Native Executable**

Run the [steps](https://github.com/borisf/insta-search/blob/master/NativeExecutable.md) to create a platform specific executable.

**This is not an official Google product.**

## License

```
Copyright 2021 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License
is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
or implied. See the License for the specific language governing permissions and limitations under
the License.
```
