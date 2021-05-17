# Native Executables
This page lists the steps to create a native executable from a jar file. 

## JPackage

1. Create the ```inputDir``` folder and add the [jar](https://github.com/borisf/insta-search/releases) there

2. Command

```bash
./jpackage --input inputDir/ \
--name InstaSearch \
--main-jar ./insta-search-7.5.jar \
--main-class com.borisfarber.instasearch.ui.InstaSearch \
--type deb \
--java-options '--enable-preview'
```
More [info](https://docs.oracle.com/en/java/javase/15/docs/specs/man/jpackage.html)

### Linux Install(APT)

```bash
$ sudo chmod 777 ./instasearch_1.0-1_amd64.deb

$ sudo apt install ./instasearch_1.0-1_amd64.deb

$ cd /opt <<< installation folder
```

### Windows Install
Fill in