/*
Copyright (C) 2009-2012 Bengt Martensson.

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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is a read interface to /etc/ethers, or any file with that format.
 *
 * @see <a href="http://linux.die.net/man/5/ethers">man /etc/ethers</a>
 */
public class Ethers {

    private final static Logger logger = Logger.getLogger(Ethers.class.getName());
    public final static String defaultPathname = System.getProperty("os.name").startsWith("Windows")
            ? System.getenv("WINDIR") + File.separator + "ethers"
            : "/etc/ethers";
    private static Ethers instance = null;

    /**
     * Returns the Ethernet address (MAC) of the host given as argument, using the filename as ethere data base.
     * @param hostname
     * @param ethersPathname Path to ethers data base. If null, use system default.
     * @return Ethernet address as (un-interpreted) string.
     * @throws org.harctoolbox.harchardware.misc.Ethers.MacAddressNotFound If not found.
     * @throws IOException Problems with the ethers data base.
     */
    public static String getEtherAddress(String hostname, File ethersPathname) throws MacAddressNotFound, IOException {
        File ethers = ethersPathname == null ? new File(defaultPathname) : ethersPathname;
        if (instance == null || ! instance.filename.equals(ethers))
            instance = new Ethers(ethersPathname);
        return instance.getMac(hostname);
    }

    /**
     * Same as two parameter version, but uses the system default.
     * @param hostname
     * @return
     * @throws IOException
     * @throws org.harctoolbox.harchardware.misc.Ethers.MacAddressNotFound 
     */
    public static String getEtherAddress(String hostname) throws IOException, MacAddressNotFound {
        return getEtherAddress(hostname, null);
    }

    /**
     * Command line interface to getMac.
     * @param args hostname to be resolved
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage:\n\tethers host");
            System.exit(1);
        }

        try {
            String result = getEtherAddress(args[0]);
            System.out.println(result);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (MacAddressNotFound ex) {
            System.out.println(ex.getLocalizedMessage());
        }
    }

    private final Map<String, String> table;
    private final File filename;

    private Ethers(File file) throws IOException {
        this.filename = file;
        table = new HashMap<>(64);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.defaultCharset()))) {
            while (true) {
                String line = reader.readLine();
                if (line == null)
                    break;

                if (line.startsWith("#") || line.isBlank())
                    continue;

                String[] str = line.split("[\\s]+");
                if (str.length == 2) {
                    String mac = str[0];
                    String hostname = str[1];
                    table.put(hostname, mac);
                } else
                    logger.log(Level.WARNING, "Malformed line: {0}", line);
            }
        }
    }

    /**
     * Returns MAC address.
     *
     * @param hostname
     * @return Mac-address belonging to the host in the argument, or null if not found.
     */
    private String getMacOrNull(String hostname) {
        if (hostname == null || hostname.isEmpty())
            return null;
        if (table.containsKey(hostname))
            return table.get(hostname); // may be null

        InetAddress addr;
        try {
            addr = InetAddress.getByName(hostname);
        } catch (UnknownHostException ex) {
            table.put(hostname, null);
            return null;
        }
        String hostnameCanonical = addr.getCanonicalHostName();
        String mac = table.get(hostnameCanonical);
        if (mac != null) {
            table.put(hostname, mac);
            return mac;
        }

        return table.get(hostnameCanonical.split("\\.")[0]);
    }

    /**
     * Returns MAC address.
     *
     * @param hostname
     * @return Mac-address belonging to the host in the argument, or null if not
     * found.
     * @throws org.harctoolbox.harchardware.misc.Ethers.MacAddressNotFound
     */
    private String getMac(String hostname) throws MacAddressNotFound {
        String mac = getMacOrNull(hostname);
        if (mac == null)
            throw new MacAddressNotFound(hostname);

        return mac;
    }

    public static class MacAddressNotFound extends Exception {

        public MacAddressNotFound(String hostname) {
            super("MAC address for " + hostname + " not found.");
        }
    }
}
