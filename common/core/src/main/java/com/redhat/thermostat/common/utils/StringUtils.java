/*
 * Copyright 2012-2017 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.common.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class StringUtils {

    private StringUtils() {
        /* should not be instantiated */
    }

    public static InputStream toInputStream(String toConvert) {
        try {
            return new ByteArrayInputStream(toConvert.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 not supported");
        }
    }

    public static String quote(String toQuote) {
        return "\"" + toQuote + "\"";
    }

    public static String repeat(String text, int times) {
        StringBuilder builder = new StringBuilder(text.length() * times);
        for (int i = 0; i < times; i++) {
            builder.append(text);
        }
        return builder.toString();
    }

    public static String join(String delimiter, Iterable<? extends CharSequence> items) {
        StringBuilder result = new StringBuilder();
        for (CharSequence item : items) {
            if (result.length() != 0) {
                result.append(delimiter);
            }
            result.append(item);
        }

        return result.toString();
    }

    /** Make a string usable as html by escaping everything dangerous inside it */
    public static String htmlEscape(String in) {
        // https://www.owasp.org/index.php/XSS_(Cross_Site_Scripting)_Prevention_Cheat_Sheet#RULE_.231_-_HTML_Escape_Before_Inserting_Untrusted_Data_into_HTML_Element_Content
        in = in.replaceAll("&", "&amp;");
        in = in.replaceAll("<", "&lt;");
        in = in.replaceAll(">", "&gt;");
        in = in.replaceAll("\"", "&quot;");
        in = in.replaceAll("'", "&#x27;");
        in = in.replaceAll("/", "&#x2F;");
        return in;
    }

    /**
     * Compare nullable Strings.
     * The "null string" is considered to be "greater than", or "come after", any non-null String.
     * For two non-null values, this method is equal to s1.compareTo(s2).
     */
    public static int compare(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return 0;
        }
        if (s1 == null) {
            return 1;
        }
        if (s2 == null) {
            return -1;
        }
        return s1.compareTo(s2);
    }

}

