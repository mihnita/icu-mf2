// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2x;

import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// TODO: Debug utilities, to remove
class DbgUtil {
    private static final Logger LOGGER = Logger.getLogger(DbgUtil.class.getName());

    private static final Gson GSON = new GsonBuilder()
            // .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create();

    static boolean debug = true;

    static void spy(String label, Object obj) {
        spy(false, label, obj, "");
    }

    static void spy(String label, Object obj, String xtras) {
        spy(false, label, obj, xtras);
    }

    static void spy(boolean force, String label, Object obj, String xtras) {
        if (debug) {
            String message = String.format("SPY: %s%s: %s", label, xtras, GSON.toJson(obj));
            if (force) {
                LOGGER.info(message);
                // System.out.printf("\033[97m" + message + "\033[m\n");
            } else {
                LOGGER.fine(message);
                // System.out.printf("\033[37m" + message + "\033[m\n");
            }
        }
    }
}
