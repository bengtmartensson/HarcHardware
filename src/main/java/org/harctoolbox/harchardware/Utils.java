/*
Copyright (C) 2011, 2019 Bengt Martensson.

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

package org.harctoolbox.harchardware;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;
import org.harctoolbox.ircore.ThisCannotHappenException;

/**
 * This class contains a few static support function.
 */
public class Utils {

    private static final String NATIVE = "native";
    public static final String DEFAULT = "default";

    public static String getHostname() {
        String hostname = System.getenv("HOSTNAME");
        if (hostname == null)
            hostname = System.getenv("COMPUTERNAME");

        return hostname;
    }

    /**
     * Returns the MAC address for the argument IP.
     * @param address IP address.
     * @return MAC address as string.
     */
    public static String getMacAddress(InetAddress address) {
        try {
            NetworkInterface ni = NetworkInterface.getByInetAddress(address);
            StringBuilder macAddress = new StringBuilder(32);
            if (ni != null) {
                byte[] mac = ni.getHardwareAddress();
                if (mac != null) {
                    for (int i = 0; i < mac.length; i++) {
                        macAddress.append(String.format("%02X%s", mac[i], ((i == mac.length - 1) ? "" : "-")));
                    }
                }
            }
            return macAddress.toString();
        } catch (SocketException ex) {
            return null;
        }
    }

    public static byte[] readBytes(InputStream inStream, int length) throws TimeoutException, IOException  {
        byte[] result = new byte[length];
        int index = 0;
        while (index < length) {
            int bytesRead = inStream.read(result, index, length - index);
            if (bytesRead == 0)
                throw new TimeoutException("Timeout in readBytes");
            index += bytesRead;
        }
        return result;
    }

    public static File libraryDir(String applicationHome) {
        File appHome = new File(applicationHome);
        File stem = appHome.getName().equals("target") ? appHome.getParentFile() : appHome;
        File nativeDir = new File(stem, NATIVE);
        if (nativeDir.exists())
            stem = nativeDir;
        String subFolderName = (System.getProperty("os.name").startsWith("Windows")
                ? "Windows"
                : System.getProperty("os.name"))
                + '-' + System.getProperty("os.arch").toLowerCase(Locale.US);
        return new File(stem, subFolderName);
    }

    public static String findApplicationHome(String appHome, Class<?> mainClass, String appName) {
        String enviromentVarName = appName.toUpperCase(Locale.US) + "HOME";
        String applicationHome = appHome != null ? appHome : System.getenv(enviromentVarName);
        if (applicationHome == null) {
            URL url = mainClass.getProtectionDomain().getCodeSource().getLocation();
            File dir;
            try {
                dir = new File(url.toURI()).getParentFile();
            } catch (URISyntaxException ex) {
                throw new ThisCannotHappenException(ex);
            }
            applicationHome = (dir.getName().equals("build") || dir.getName().equals("dist"))
                    ? dir.getParent() : dir.getAbsolutePath();
        }
        return applicationHome;
    }

    private Utils() {
    }
}
