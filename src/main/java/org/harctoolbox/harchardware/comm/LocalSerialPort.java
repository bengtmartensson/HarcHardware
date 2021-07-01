/*
Copyright (C) 2012,2013,2014 Bengt Martensson.

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

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.RXTXPort;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;

// NOTE: public functions must not expose the underlying implementation
// (gnu.io.*), in particular not throw any gnu.io.*Exceptions!!

public abstract class LocalSerialPort implements IHarcHardware {

    private static final Logger logger = Logger.getLogger(LocalSerialPort.class.getName());

    public static final String DEFAULT_PORT = "/dev/ttyS0";
    public static final int DEFAULT_BAUD = 9600;
    public static final int DEFAULT_DATABITS = 8;
    public static final Parity DEFAULT_PARITY = Parity.NONE;
    public static final StopBits DEFAULT_STOPBITS = StopBits.ONE;
    public static final FlowControl DEFAULT_FLOWCONTROL = FlowControl.NONE;
    private static final int MS_TO_WAIT_FOR_PORT = 100;
    public static final int DEFAULT_TIMEOUT = 0;
    private static final String SLASH_DEV = "/dev";

    private static List<String> cachedPortNames = null;
    private static final File LOCKDEV_DIR = new File("/var/lock/lockdev");
    private static final String NRLIB_PROPERTY_NAME = "libNRJavaSerial.userlib";
    private static final String NRLIB_NAME = "NRJavaSerial";
    public static final String DEFAULT = "default";

//    private static File libraryDir = null;

//    static {
//        loadLibrary();
//    }
    public static synchronized void setLibraryDir(File libraryDir) throws IOException {
//        libraryDir = newLibraryDir;
//    }
//
//    private static synchronized void loadLibrary() {
//        try {
        if (LOCKDEV_DIR.isDirectory() && LOCKDEV_DIR.canWrite() && libraryDir != null) {
            File absFile = new File(libraryDir, System.mapLibraryName(NRLIB_NAME));
            // Load it here, because NRJavaSerial does not properly throw UnsatisfiedLinkError
            //System.load(absFile.getCanonicalPath());
            System.setProperty(NRLIB_PROPERTY_NAME, absFile.getCanonicalPath());
//        } catch (IOException ex) {
//            throw new UnsatisfiedLinkError(ex.getLocalizedMessage());
        }
    }

    public static String getSerialPortName(Integer portNumber) throws NonExistingPortException {
        @SuppressWarnings("unchecked")
        Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier.getPortIdentifiers();
        int nr = 0;
        while (portEnum.hasMoreElements()) {
            CommPortIdentifier portIdentifier = portEnum.nextElement();
            if (portIdentifier.getPortType() == CommPortIdentifier.PORT_SERIAL)
                nr++;

            if (portNumber == null || nr == portNumber)
                return portIdentifier.getName();
        }
        throw new NonExistingPortException(Integer.toString(portNumber));
    }

    /**
     * Returns all serial port names found in the system.
     * @param useCached If true, use previously acquired list, if available
     * @return ArrayList&lt;String&gt;
     * @throws IOException
     */
    @SuppressWarnings({"unchecked", "ReturnOfCollectionOrArrayField"})
    public static List<String> getSerialPortNames(boolean useCached) throws IOException {
        if (useCached && cachedPortNames != null)
            return cachedPortNames;

        Enumeration<CommPortIdentifier> portEnum = null;
        try {
            portEnum = CommPortIdentifier.getPortIdentifiers();
        } catch (UnsatisfiedLinkError ex) {
            throw new IOException(ex.getMessage());
        }
        List<String> names = new ArrayList<>(8);
        while (portEnum.hasMoreElements()) {
            CommPortIdentifier portIdentifier = portEnum.nextElement();
            if (portIdentifier.getPortType() == CommPortIdentifier.PORT_SERIAL)
                names.add(portIdentifier.getName());
        }
        cachedPortNames = names;
        return names;
    }

    // On systems with /dev device names, expand symbolic links
    // (like the one udev creates, /dev/arduino -> /dev/ttyACM0)
    // Otherwise, just return the argument.
    public static String canonicalizePortName(String name, String dflt) throws IOException {
        String portName = (name == null || name.equalsIgnoreCase(DEFAULT)) ? dflt : name;
        if (File.separatorChar == '\\')
            return portName;
        File absFile = new File(portName).isAbsolute()  ? new File(portName) : new File(SLASH_DEV, portName);
        String path = absFile.getCanonicalPath();
        if (name == null || !name.equals(path))
            logger.log(Level.INFO, "Device named \"{0}\" expanded to \"{1}\"", new String[]{name, path});
        return path;
    }

    public static String canonicalizePortName(String portName) throws IOException {
        return canonicalizePortName(portName, DEFAULT_PORT);
    }

    public static String getSoftwareVersion() {
        // FIXME return RXTXVersion.getVersion() + "/" + RXTXVersion.nativeGetVersion();
        return "NRJavaSerial-5.2.1";
    }

    public static StopBits mkStopBits(int stopBits) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    protected InputStream inStream;
    protected OutputStream outStream;
    private CommPort commPort;
    private final String portName;
    private final String realPath;
    private final int baud;
    private final int dataLength;
    private final StopBits stopBits;
    private final Parity parity;
    private final FlowControl flowControl;
    private int timeout;
    protected boolean verbose;

    public LocalSerialPort(String portName, boolean verbose, Integer timeout, Integer baud, Integer dataLength, StopBits stopBits, Parity parity, FlowControl flowControl) throws IOException {
        this.verbose = verbose;
        this.portName = portName;
        realPath = canonicalizePortName(portName);
        this.baud = baud != null ? baud : DEFAULT_BAUD;
        this.dataLength = dataLength != null ? dataLength : DEFAULT_DATABITS;
        this.stopBits = stopBits != null ? stopBits : DEFAULT_STOPBITS;
        this.parity = parity != null ? parity : DEFAULT_PARITY;
        this.flowControl = flowControl != null ? flowControl : DEFAULT_FLOWCONTROL;
        this.timeout = timeout != null ? timeout : DEFAULT_TIMEOUT;
    }


    public LocalSerialPort(String portName, boolean verbose, Integer timeout, Integer baud) throws IOException {
        this(portName, verbose, timeout, baud, null, null, null, null);
    }

    public LocalSerialPort(String portName, boolean verbose, Integer timeout) throws IOException {
        this(portName, verbose, timeout, null);
    }

    public LocalSerialPort(String portName, boolean verbose) throws IOException {
        this(portName, verbose, null);
    }

    public LocalSerialPort(String portName) throws IOException {
        this(portName, false);
    }

    public LocalSerialPort(Integer portNumber) throws NonExistingPortException, IOException {
        this(getSerialPortName(portNumber));
    }

    private void lowLevelOpen() throws NoSuchPortException, PortInUseException, IOException {
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(realPath);
        commPort = portIdentifier.open(this.getClass().getName(), MS_TO_WAIT_FOR_PORT);
    }

    @Override
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    /**
     * Opens the device.
     *
     * @throws org.harctoolbox.harchardware.comm.NonExistingPortException
     * @throws HarcHardwareException Bundles RXTX exceptions together.
     * @throws IOException
     */
    @Override
    public void open() throws NonExistingPortException, HarcHardwareException, IOException {
        boolean success = false;
        try {
            lowLevelOpen();
            success = true;
        } catch (NoSuchPortException ex) {
            commPort = null;
            throw new NonExistingPortException(portName);
        } catch (PortInUseException ex) {
            commPort = null;
            throw new HarcHardwareException("Port in use: " + portName);
        }

        if (!success)
            throw new HarcHardwareException("Could not open LocalSerialPort " + portName);

        //if (commPort instanceof gnu.io.SerialPort) {
        RXTXPort serialPort = (RXTXPort) commPort;
        try {
            serialPort.setSerialPortParams(baud, dataLength, stopBits.ordinal(), parity.ordinal());
        } catch (UnsupportedCommOperationException ex) {
            throw new HarcHardwareException(ex);
        }

        inStream = serialPort.getInputStream();
        outStream = serialPort.getOutputStream();
        //}
        //if (commPort instanceof gnu.io.RXTXPort) {
        serialPort.setFlowControlMode(flowControl.ordinal());
        //}
        setTimeout();
    }

    public void flushInput() throws IOException {
        while (inStream.available() > 0)
            inStream.read();
    }

    @Override
    public boolean isValid() {
        return commPort != null;
    }

    @Override
    public void setTimeout(int timeout) throws IOException {
        this.timeout = timeout;
        setTimeout();
    }

    private void setTimeout() throws IOException {
        if (timeout > 0)
            try {
                commPort.enableReceiveTimeout(timeout);
            } catch (UnsupportedCommOperationException ex) {
                throw new IOException("timeout not supported");
            }
        else
            commPort.disableReceiveTimeout();
    }

    @Override
    public String getVersion() {
        return getSoftwareVersion();
    }

    /**
     * Returns the nominal port name being used. May differ from one requested in
     * the constructor if the device was opened with open(true), or if it is symlink
     * (for example created by udev).
     * @return port name used.
     */
    public String getPortName() {
        return portName;
    }

    @Override
    public void close() {
        if (!isValid())
            return;
        try {
            inStream.close();
            outStream.close();
            commPort.close();
        } catch (IOException ex) {
            //throw new HarcHardwareException(ex);
        } finally {
            commPort = null;
        }
    }

    public void flush() throws IOException {
        outStream.flush();
    }

    public void setDTR(boolean state) {
        //if (commPort instanceof gnu.io.RXTXPort) {
            ((SerialPort) commPort).setDTR(state);
        //}
    }

    public void dropDTR(int duration) {
        setDTR(false);
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ex) {
        }
        setDTR(true);
    }

    @Override
    public void setDebug(int dbg) {
    }

    public enum StopBits {
        NONE, // 0
        ONE, // 1
        TWO, // 2
        ONE_AND_A_HALF; // 3

        public static StopBits mkStopBits(int stopBits) {
            return stopBits == 2 ? TWO : ONE;
        }
    }

    public enum Parity {
        NONE,  // 0
        ODD,   // 1
        EVEN,  // 2
        MARK,  // 3
        SPACE; // 4

        public static Parity parse(String str) {
            return Parity.valueOf(str.toUpperCase(Locale.US));
        }
    }

    public enum FlowControl {
        NONE,        // 0
        RTSCTS_IN,   // 1
        RTSCTS_OUT,  // 2
        RTSCTS,      // 3
        XONXOFF_IN,  // 4
        dummy2,
        dummy3,
        dummy4,
        XONXOFF_OUT, // 8
        dummy5,
        dummy6,
        dummy7,
        XONXOFF;     // 12

        public static FlowControl parse(String str) {
            return FlowControl.valueOf(str.toUpperCase(Locale.US).replaceAll("/", ""));
        }
    }
}
