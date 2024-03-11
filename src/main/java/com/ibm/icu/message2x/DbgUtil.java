// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2x;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// TODO: Debug utilities, to remove
class DbgUtil {

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
            if (force) {
                System.out.printf("\033[97mSPY: %s%s: %s\033[m%n", label, xtras, GSON.toJson(obj));
            } else {
                System.out.printf("\033[37mSPY: %s%s: %s\033[m%n", label, xtras, GSON.toJson(obj));
            }
        }
    }
}
