/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.geemodule.util;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.text.Transliterator;

public class Strings {
    private static final String DEFAULT_TRANSLITERATION_CODE = "Any-Latin; NFD; [:Nonspacing Mark:] remove; nfc";

    private static final Map<String, Transliterator> transliteratorCache = new HashMap<>();

    private static final Map<String, String> transliterateReplaceChars = new HashMap<>();

    private static final String ae = "ä";
    private static final String oe = "ö";
    private static final String ue = "ü";
    private static final String Ae = "Ä";
    private static final String Oe = "Ö";
    private static final String Ue = "Ü";
    private static final String ss = "ß";
    private static final String at = "@";
    private static final String ampersand = "&";

    static {
        transliterateReplaceChars.put(ae, "ae");
        transliterateReplaceChars.put(oe, "oe");
        transliterateReplaceChars.put(ue, "ue");
        transliterateReplaceChars.put(Ae, "Ae");
        transliterateReplaceChars.put(Oe, "Oe");
        transliterateReplaceChars.put(Ue, "Ue");
        transliterateReplaceChars.put(ss, "ss");
        transliterateReplaceChars.put(at, "at");
        transliterateReplaceChars.put(ampersand, "and");
    }

    private static final Pattern slugifyReplacePattern1 = Pattern.compile("[^\\p{ASCII}]");
    private static final Pattern slugifyReplacePattern2 = Pattern.compile("[^a-zA-Z0-9_\\-\\. ]");
    private static final Pattern slugifyReplacePattern3 = Pattern.compile("\\-+");
    private static final Pattern slugifyReplacePattern4 = Pattern.compile("\\s+");

    public static String slugify(String text) {
        return slugify(text, null);
    }

    public static String slugify(String text, String transliterationCode) {
        if (text == null || "".equals(text.trim()))
            return text;

        if (transliterationCode == null)
            transliterationCode = DEFAULT_TRANSLITERATION_CODE;

        String transliteratedText = text.replace(ae, transliterateReplaceChars.get(ae)).replace(oe, transliterateReplaceChars.get(oe)).replace(ue, transliterateReplaceChars.get(ue))
            .replace(Ae, transliterateReplaceChars.get(Ae))
            .replace(Oe, transliterateReplaceChars.get(Oe)).replace(Ue, transliterateReplaceChars.get(Ue)).replace(ss, transliterateReplaceChars.get(ss)).replace(at, transliterateReplaceChars.get(at))
            .replace(ampersand, transliterateReplaceChars.get(ampersand));

        Transliterator t = transliteratorCache.get(transliterationCode);

        if (t == null) {
            t = Transliterator.getInstance(transliterationCode);
            transliteratorCache.put(transliterationCode, t);
        }

        transliteratedText = t.transform(transliteratedText);

        transliteratedText = Normalizer.normalize(transliteratedText, Normalizer.Form.NFD).replace('_', '-').replace(' ', '-').replace(',', '-').replace('.', '-');

        Matcher m1 = slugifyReplacePattern1.matcher(transliteratedText);
        transliteratedText = m1.replaceAll("");

        Matcher m2 = slugifyReplacePattern2.matcher(transliteratedText);
        transliteratedText = m2.replaceAll("");

        Matcher m3 = slugifyReplacePattern3.matcher(transliteratedText);
        transliteratedText = m3.replaceAll("-");

        Matcher m4 = slugifyReplacePattern4.matcher(transliteratedText);
        transliteratedText = m4.replaceAll(" ");

        return transliteratedText.toLowerCase();
    }
}
