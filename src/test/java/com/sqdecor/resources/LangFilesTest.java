package com.sqdecor.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;

class LangFilesTest {
    private static final Path LANG = Path.of("src/generated/resources/assets/sqdecor/lang");
    private static final Pattern ARG = Pattern.compile("%(?:(\\d+)\\$)?[sd]");
    private static final List<String> LOCALES = List.of("en_us", "de_de", "ru_ru");

    private static Map<String, String> load(String locale) {
        Map<String, String> out = new TreeMap<>();
        for (Map.Entry<String, JsonElement> e : Res.object(LANG.resolve(locale + ".json")).entrySet()) {
            out.put(e.getKey(), e.getValue().getAsString());
        }
        return out;
    }

    static int args(String text) {
        Matcher m = ARG.matcher(text.replace("%%", ""));
        int plain = 0;
        int positional = 0;
        while (m.find()) {
            if (m.group(1) == null) plain++;
            else positional = Math.max(positional, Integer.parseInt(m.group(1)));
        }
        return Math.max(plain, positional);
    }

    @Test
    void sameKeysInAllLanguages() {
        Map<String, String> en = load("en_us");
        assertFalse(en.isEmpty());
        for (String locale : LOCALES) {
            Map<String, String> other = load(locale);
            TreeSet<String> missing = new TreeSet<>(en.keySet());
            missing.removeAll(other.keySet());
            TreeSet<String> extra = new TreeSet<>(other.keySet());
            extra.removeAll(en.keySet());
            assertTrue(missing.isEmpty(), locale + " fehlt: " + missing);
            assertTrue(extra.isEmpty(), locale + " zuviel: " + extra);
        }
    }

    @Test
    void samePlaceholders() {
        Map<String, String> en = load("en_us");
        List<String> wrong = new ArrayList<>();
        for (String locale : LOCALES) {
            Map<String, String> other = load(locale);
            for (Map.Entry<String, String> e : en.entrySet()) {
                String text = other.get(e.getKey());
                if (text != null && args(text) != args(e.getValue())) wrong.add(locale + ": " + e.getKey());
            }
        }
        assertTrue(wrong.isEmpty(), "Platzhalter: " + wrong);
    }

    @Test
    void noEmptyTexts() {
        List<String> empty = new ArrayList<>();
        for (String locale : LOCALES) {
            load(locale).forEach((k, v) -> {
                if (v.isBlank() && !k.equals("item.sqdecor.tab_spacer")) empty.add(locale + ": " + k);
            });
        }
        assertTrue(empty.isEmpty(), "leer: " + empty);
    }

    @Test
    void argumentCounting() {
        assertEquals(0, args("100%% sicher"));
        assertEquals(2, args("%s LEDs an Kanal %s"));
        assertEquals(2, args("Kanal %2$s: %1$s"));
    }
}
