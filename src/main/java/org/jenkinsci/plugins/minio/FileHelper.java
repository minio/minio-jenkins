package org.jenkinsci.plugins.minio;

import java.io.File;

public class FileHelper {

    public static int getSearchPathLength(String workSpace, String filterExpanded) {

        final File file1 = new File(workSpace);
        final File file2 = new File(file1, filterExpanded);

        final String pathWithFilter = file2.getPath();

        final int indexOfWildCard = pathWithFilter.indexOf('*');


        if (indexOfWildCard > 0) {
            final String s = pathWithFilter.substring(0, indexOfWildCard);
            return s.length();
        } else {
            return file2.getParent().length() + 1;
        }
    }
}