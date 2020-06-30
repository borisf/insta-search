package com.borisfarber;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class ISFileChooser {

    private File currentFile;

    public ISFileChooser() {
        final File file = new File(System.getProperty("user.dir"));
        final Controller controller = new Controller(null, null, null);
        if (controller == null) {

        }
        controller.crawl(file);
    }

    private final boolean isDirectory() {
        if (this.currentFile != null) {

            if (this.currentFile.exists()) {

                return currentFile.isDirectory();
            }

            return false;
        }
        return false;
    }

    private final String getAbsolutePath() {
        if (this.currentFile != null) {
            final File currentFile = this.currentFile;

            if (currentFile.exists()) {
                return this.currentFile.getAbsolutePath();
            }
        }
        return "";
    }

    private final Map<String, String> filenamesToPaths() {
        //final Search search = this.search;

        //return search.filenamesToPaths();

        return null;
    }

    private final int getLineNumber(final String selectedLine) {
        // TODO implement

        return 10;
    }

    private final int countFileCharacters(final String filename, final int tillLine) throws IOException {
        return 0;
    }

    public final void reCrawl() {
        if (this.currentFile != null) {
            final File currentFile = this.currentFile;

            if (currentFile.exists()) {
                final File currentFile2 = this.currentFile;

                //this.crawl(currentFile2);
            }
        }
    }

    private final File downloadedTempTextFile() throws Exception {
        final URL website = new URL("http://www.gutenberg.org/cache/epub/18362/pg18362.txt");
        final File target = File.createTempFile("tempDict", ".txt");
        final InputStream ll = website.openStream();
        Files.copy(ll, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    private final void setFileChooserFont(final Component[] comp) {
        // TODO fill in
    }
}
