# ClassyShark Insta Search

A lot of your time spent on “writing” code on production systems is actually spent on reading large logs. 
And a decent chunk of time spent reading logs is actually spent on searching logs.

ClassyShark Insta Search is a fast, incremental [fuzzy](https://en.wikipedia.org/wiki/Approximate_string_matching) search in folders or in a large log file.

With ClassyShark Insta Search you can easily and quickly find and navigate
to any text string or regex inside any file in your folders. All done
incrementally.

![Image of ClassySearch](https://github.com/borisf/insta-search/blob/master/images/InstaSearch.png)

## Flow

* Drap and drop your file into the results view

* Search line - where you type text that are you looking for

* Results view - where you see file set that include the text from
the search line, use arrows to land to the relevant line

* Once found the relevant line, press Enter, and the log file will be opened on the relevant line within your defined
editor


## Download & Run
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
