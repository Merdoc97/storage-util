package com.example.storageutil.util;

import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.util.Strings;

@UtilityClass
public class PathUtil {

    public String buildFullPath(String userName, String path, String fileName) {
        return new StringBuffer().append("/")
                .append(replace(userName))
                .append("/")
                .append(replace(path))
                .append("/")
                .append(replace(fileName))
                .toString();
    }

    private String replace(String s) {
        return s.replaceAll("/", Strings.EMPTY);
    }
}
