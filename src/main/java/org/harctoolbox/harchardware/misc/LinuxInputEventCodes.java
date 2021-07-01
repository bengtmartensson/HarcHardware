/*
Copyright (C) 2018 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
*/

package org.harctoolbox.harchardware.misc;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class contains some static stuff allowing access to the Linux keycodes in input-event-codes.h.
 * Presently, it runs on Linux only. We leave it as an exercise to the reader to make it portable.
 */
public class LinuxInputEventCodes {

    private static final String linuxFilename = "/usr/include/linux/input-event-codes.h";

    private static Map<String, Integer> keys2numbers;
    private static Map<Integer, String> numbers2keys;

    static {
        try {
            parse(linuxFilename);
        } catch (IOException ex) {
            ex.printStackTrace();
            //keys2numbers = LinuxInputEventCodesFallback.keys2number;
        }
        invert();
    }

    private static void parse(String filename) throws FileNotFoundException, IOException {
        keys2numbers = new LinkedHashMap<>(1024);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(filename), Charset.forName("US-ASCII")))) {
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                String[] str = line.split("[\\s]+");
                if (str.length < 3 || !str[0].equals("#define"))
                    continue;
                String name = str[1];
                String value = str[2];
                if (!(name.startsWith("KEY_") /*|| name.startsWith("BTN_")|| name.startsWith("EV_")*/))
                    continue;
                Integer number = parseValue(value);
                //if (keys.containsValue(number))
                //    System.err.println("Conflict: " + name + " " + value);
                keys2numbers.put(name, number);
            }
        }
    }

    private static void invert() {
        numbers2keys = new LinkedHashMap<>(keys2numbers.size());
        keys2numbers.entrySet().forEach((kvp) -> {
            numbers2keys.put(kvp.getValue(), kvp.getKey());
        });
    }

    private static Integer parseValue(String s) {
        if (s.startsWith("0x"))
            return Integer.parseInt(s.substring(2), 16);
        if (s.matches("\\d+"))
            return Integer.parseInt(s);
        if (s.matches("\\(.*\\)"))
            return parseValue(s.substring(1,s.length()-1));
        if (s.contains("+")) {
            String[] arr = s.split("\\+");
            return parseValue(arr[0]) + parseValue(arr[1]);
        }
        return keys2numbers.get(s);
    }

    private static String generateFallback() {
        StringBuilder str = new StringBuilder(10000);
        str.append("// This file was automatically generated, do not edit.\n\n");
        str.append("package org.harctoolbox.harchardware.misc;\n\n");
        str.append("import java.util.LinkedHashMap;\n");
        str.append("import java.util.Map;\n\n");
        str.append("class LinuxInputEventCodesFallback {\n");
        str.append("    public static Map<String, Integer> keys2numbers;\n\n");
        str.append("    static {\n");
        str.append("        keys2numbers = new LinkedHashMap<>(").append(keys2numbers.size()).append(");\n");

        keys2numbers.entrySet().forEach((kvp) -> {
            str.append("        keys2numbers.put(\"").append(kvp.getKey()).append("\", ").append(kvp.getValue()).append(");\n");
        });
        str.append("    }\n");
        str.append("    private LinuxInputEventCodesFallback() {\n");
        str.append("    }\n");
        str.append("}\n");

        return str.toString();
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] args) {
        System.out.println(generateFallback());
    }

    public static Set<String> getKeyNames() {
        return keys2numbers.keySet();
    }

    public static boolean hasKeyName(String key) {
        return keys2numbers.containsKey(key);
    }

    public static Integer getKeyCode(String key) {
        return keys2numbers.get(key);
    }

    public static String getKeyName(int code) {
        return numbers2keys.get(code);
    }

    private LinuxInputEventCodes() {
    }
}
