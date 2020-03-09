/*
 Copyright (C) 2014, 2015 Bengt Martensson.

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

package org.harctoolbox.harchardware.comm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.harchardware.misc.Ethers;
import org.harctoolbox.ircore.ThisCannotHappenException;

/**
 * This class sends a wakeup package to a host, given by its MAC address or hostname (provided that it is present in the ethers data base).
 */
public class Wol {

    private final static Logger logger = Logger.getLogger(Wol.class.getName());
    public final static String defaultIP = "255.255.255.255";
    public final static int defaultPort = 9;

    public static void wol(String wolee) throws IOException, Ethers.MacAddressNotFound {
        (new Wol(wolee)).wol();
    }

    public static void main(String[] args) {
        try {
            File ethersPath = null;
            int arg_i = 0;
            if (args[arg_i].equals("-f")) {
                arg_i++;
                ethersPath = new File(args[arg_i]);
                arg_i++;
            }
            String wolee = args[arg_i];
            Wol wol = new Wol(wolee, ethersPath);
            wol.wol();
            System.err.println("Sent WOL to " + wol.ethernetAddress.toString());
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        } catch (Ethers.MacAddressNotFound ex) {
            Logger.getLogger(Wol.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private EthernetAddress ethernetAddress;
    private InetAddress ip;
    private final int port;

    public Wol(String str) throws IOException, Ethers.MacAddressNotFound {
        this(str, null);
    }

    public Wol(String str, File path) throws IOException, Ethers.MacAddressNotFound {
        this(str, null, defaultPort, path);
    }

    /**
     * Constructor for Wol.
     * @param str Either ethernet address or a host name found in the ethers data base
     * @param ip IP address to send to, should be a broadcast address (255.255.255.255)
     * @param port port to send to, normally 9
     * @param pathname
     * @throws FileNotFoundException ethers data base file was not found.
     * @throws org.harctoolbox.harchardware.misc.Ethers.MacAddressNotFound
     * @throws java.net.UnknownHostException
     */
    public Wol(String str, InetAddress ip, int port, File pathname) throws IOException, Ethers.MacAddressNotFound {
        this.ip = ip;
        this.port = port;
        try {
            if (ip == null)
                this.ip = InetAddress.getByName(defaultIP);
        } catch (UnknownHostException ex) {
            throw new ThisCannotHappenException(ex);
        }
        try {
            this.ethernetAddress = new EthernetAddress(str);
        } catch (InvalidEthernetAddressException ex) {
            // the argument did not parse as Ethernet address, try as hostname
            Ethers ethers = pathname == null ? new Ethers() : new Ethers(pathname);
            String mac = ethers.getMac(str);
            try {
                this.ethernetAddress = new EthernetAddress(mac);
            } catch (InvalidEthernetAddressException ex1) {
                throw new ThisCannotHappenException(ex);
            }
        }
    }

    @Override
    public String toString() {
        return "wol " + ethernetAddress.toString();
    }

    private byte[] createWakeupFrame() {
        byte[] ethernetAddressBytes = ethernetAddress.toBytes();
        byte[] wakeupFrame = new byte[6 + 16 * ethernetAddressBytes.length];
        Arrays.fill(wakeupFrame, 0, 6, (byte) 0xFF);
        for (int j = 6; j < wakeupFrame.length; j += ethernetAddressBytes.length)
            System.arraycopy(ethernetAddressBytes, 0, wakeupFrame, j, ethernetAddressBytes.length);

        return wakeupFrame;
    }

    /**
     * Wakes up the machines with provided Ethernet addresses. The magic
     * sequences are sent to the given host and port.
     *
     * @throws java.net.SocketException
     * @throws IOException if an I/O error occurs
     */
    public void wol() throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] wakeupFrame = createWakeupFrame();
            DatagramPacket packet = new DatagramPacket(wakeupFrame, wakeupFrame.length, ip, port);
            socket.send(packet);
        }
    }
}
