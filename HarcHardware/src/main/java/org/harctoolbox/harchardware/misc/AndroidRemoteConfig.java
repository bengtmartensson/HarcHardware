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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

// See http://www.hifi-remote.com/forums/viewtopic.php?t=15987&start=14 and
// https://github.com/bengtmartensson/harctoolboxbundle/issues/234
// Too soft to present as a solution to a real problem.
public class AndroidRemoteConfig {
    private static String separator = "\t";

    private static String parseCommand(String string) {
        return LinuxInputEventCodes.getKeyName(parseInt(string));
   }

    private static int parseInt(String string) {
        return string.startsWith("0x") ? Integer.parseInt(string.substring(2), 16) : Integer.parseInt(string);
    }

    public static void main(String[] args) {
        try {
            System.out.println(new AndroidRemoteConfig(new File(args[0])));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private Map<String, Integer> commands;
    private int D = -1;
    private int S = -1;

    public AndroidRemoteConfig(File configFileName) throws FileNotFoundException, IOException {
        this(new InputStreamReader(new FileInputStream(configFileName), Charset.forName("US-ASCII")));
    }

    public AndroidRemoteConfig(Reader reader) throws IOException {
        commands = new LinkedHashMap<>(32);
        boolean inCommandSet = false;
        try (BufferedReader r = new BufferedReader(reader)) {
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                String l = line.trim();
                if (l.startsWith("factory_code")) {
                    String[] arr = l.split("=");
                    long code = Long.parseLong(arr[1].trim().replace("0x", ""), 16);
                    int n = (int) (code >> 16L);
                    D = n & 0xFF;
                    S = n >> 8;
                } else if (inCommandSet) {
                    if (l.equals("key_end"))
                        break;
                    String[] arr = line.split("\\s+");
                    String command = parseCommand(arr[1]);
                    int number = parseInt(arr[0]);
                    commands.put(command, number);
                } else
                    inCommandSet = l.equals("key_begin");
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(20*commands.size());

        str.append("NEC1 ").append(D);
        if (D + S != 255)
            str.append("/").append(S);
        str.append("\n");
        commands.entrySet().forEach((kvp) -> {
            if (kvp.getKey() != null)
                str.append(kvp.getKey()).append(separator).append(kvp.getValue()).append("\n");
        });
        return str.toString();
    }
}
