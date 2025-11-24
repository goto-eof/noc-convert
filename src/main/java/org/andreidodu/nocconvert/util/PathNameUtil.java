package org.andreidodu.nocconvert.util;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class PathNameUtil {

    private String normalizeString(String string, int maxLength) {
        if (string == null || string.isEmpty()) {
            return "";
        }

        if (string.length() > maxLength) {
            return string.substring(0, maxLength) + "...";
        }

        return string;
    }

    public static String normalizePath(String pathString, int maxLength) {
        if (pathString == null || pathString.length() <= maxLength) {
            return pathString;
        }

        int contentBudget = maxLength - 3;
        int startLength = contentBudget / 2;
        int endLength = contentBudget - startLength;
        String start = pathString.substring(0, startLength);
        String end = pathString.substring(pathString.length() - endLength);
        return start + "..." + end;
    }

    public static String normalizePathAdvanced(String pathString, int maxLength) {
        if (pathString == null || pathString.length() <= maxLength) {
            return pathString;
        }

        final String separator = File.separator;

        String standardizedPath = pathString.replace('/', separator.charAt(0));
        if (separator.equals("/")) {
            standardizedPath = standardizedPath.replace('\\', separator.charAt(0));
        }

        List<String> components = Arrays.stream(standardizedPath.split(java.util.regex.Pattern.quote(separator)))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedList::new));

        String root = "";

        if (pathString.startsWith(separator)) {
            root = separator;
        }

        if (components.size() > 0 && components.get(0).endsWith(":")) {
            root = components.get(0) + separator;
            components.remove(0);
        }

        String ELLIPSIS = "...";
        if (components.size() <= 2) {
            return pathString.substring(0, maxLength - ELLIPSIS.length()) + ELLIPSIS;
        }

        String first = components.get(0);
        String last = components.get(components.size() - 1);
        String ELLIPSIS_PATH = File.separator + ELLIPSIS + File.separator;
        String shortPath = root + first + ELLIPSIS_PATH + last;

        if (shortPath.length() <= maxLength) {
            return shortPath;
        } else {
            return pathString.substring(0, maxLength - ELLIPSIS.length()) + ELLIPSIS;
        }
    }

}
