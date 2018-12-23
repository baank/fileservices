package io.metamorphic.fileservices;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Naming {

    private static final Logger log = LogManager.getLogger(Naming.class);

    public static String underscoreFormat(String name) {
        if (name == null) {
            log.warn("null name");
            return "unknown";
        }
        String s = name.replaceAll("(\\s+|-+|_{2,})", "_");
        s = s.replaceAll("([a-z])([A-Z])", "$1_$2");
        s = s.replaceAll("([A-Z]{2,})([a-z])", "$1_$2");
        return s.toLowerCase();
    }
}
