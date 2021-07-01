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

    public static String getEtherAddress(String hostname, File ethersPathname) throws MacAddressNotFound, IOException {
        return (new Ethers(ethersPathname)).getMac(hostname);
    }

    public static String getEtherAddress(String hostname) throws IOException, MacAddressNotFound {
        return getEtherAddress(hostname, new File(defaultPathname));
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

    private final HashMap<String, String> table;

    public Ethers(File file) throws IOException {
        table = new HashMap<>(64);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.defaultCharset()))) {
            while (true) {
                String line = reader.readLine();
                if (line == null)
                    break;

                if (line.startsWith("#"))
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

    public Ethers() throws IOException {
        this(new File(defaultPathname));
    }

    /**
     * Returns MAC address.
     *
     * @param hostname
     * @return Mac-address belonging to the host in the argument, or null if not found.
     */
    public String getMacOrNull(String hostname) {
        String mac = table.get(hostname);
        if (mac != null)
            return mac;

        InetAddress addr;
        try {
            addr = InetAddress.getByName(hostname);
        } catch (UnknownHostException ex) {
            return null;
        }
        String hostnameCanonical = addr.getCanonicalHostName();
        mac = table.get(hostnameCanonical);
        if (mac != null)
            return mac;

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
    public String getMac(String hostname) throws MacAddressNotFound {
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
