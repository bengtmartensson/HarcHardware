/*
Copyright (C) 2023, 2025 Bengt Martensson.

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

package org.harctoolbox.harchardware.ir;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.internal.DefaultConsole;
import com.github.mob41.blapi.BLDevice;
import com.github.mob41.blapi.RM2Device;
import com.github.mob41.blapi.RM4Device;
import com.github.mob41.blapi.mac.Mac;
import com.github.mob41.blapi.pkt.cmd.rm2.SendDataCmdPayload;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.Utils;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrSequence;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.OddSequenceLengthException;
import org.harctoolbox.irp.IrpUtils;

/**
 *
 */
public final class Broadlink implements IHarcHardware, IRawIrSender, IReceive /* NOT ICapture */ {

    private final static Logger logger = Logger.getLogger(Broadlink.class.getName());

    private static final int DEFAULT_TIMEOUT = 2000;
    public static final short DEV_RM_2_PRO = 0x272a; // FIXME
    public static final short DEV_RM_4_MINI = 0x520c; // FIXME
    public static final short DEFAULT_TYPE = DEV_RM_2_PRO; //BLDevice.DEV_RM_2_PRO_PLUS; // 0x272a; // 0x2712;
    public static final String DEFAULT_HOST = "broadlink";
    public final static String HEX_STRING_FORMAT = "%02X";

    public final static double TICK = 32.84d;
    public final static int IR_TOKEN = 0x26;
    public final static int RF_433_TOKEN = 0xB2; // Not yet used
    public final static int RF_315_TOKEN = 0xD7; // Not yet used
    public final static int IR_ENDING_TOKEN = 0x0d05;
    public final static int RF_433_ENDING_TOKEN = 0x0181; // Not yet used
    public final static int RF_315_ENDING_TOKEN = 0xFFFF; // FIXME
    public final static int TOKEN_POS = 0;
    public final static int REPEAT_POS = 1;
    public final static int LENGTH_LSB_POS = 2;
    public final static int LENGTH_MSB_POS = 3;
    public final static int DURATIONS_OFFSET = 4;
    public final static int MIN_ARCTECH_REPEATS = 6;
    public final static double A_PRIOR_MODULATION_FREQUENCY = 38000d;

    private static JCommander argumentParser;

    private static void usage(int exitcode) {
        @SuppressWarnings("UseOfSystemOutOrSystemErr")
        PrintStream printStream = exitcode == IrpUtils.EXIT_SUCCESS ? System.out : System.err;
        argumentParser.setConsole(new DefaultConsole(printStream));
        argumentParser.usage();

        System.exit(exitcode);
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] args) throws Exception {

        CommandLineArgs commandLineArgs = new CommandLineArgs();
        argumentParser = new JCommander(commandLineArgs);
        argumentParser.setCaseSensitiveOptions(true);
        argumentParser.setAllowAbbreviatedOptions(true);
        argumentParser.setProgramName("Broadlink");

        try {
            argumentParser.parse(args);
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            usage(IrpUtils.EXIT_USAGE_ERROR);
        }

        if (commandLineArgs.helpRequested)
            usage(IrpUtils.EXIT_SUCCESS);

        if (commandLineArgs.scanDevices) {
            try {
                Collection<Broadlink> devices = scanDevices(commandLineArgs.timeout).values();
                for (Broadlink dev : devices)
                    System.out.println(new Broadlink(dev));
                System.exit(IrpUtils.EXIT_SUCCESS);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }

        try (Broadlink broadlink = (commandLineArgs.ip != null && commandLineArgs.type != null)
                ? new Broadlink(commandLineArgs.type.shortValue(), commandLineArgs.ip, commandLineArgs.mac, commandLineArgs.verbose)
                : commandLineArgs.ip != null
                        ? newBroadlink(commandLineArgs.ip, commandLineArgs.timeout)
                        : newBroadlink()) {
            broadlink.setVerbose(commandLineArgs.verbose);

            broadlink.open();
            if (commandLineArgs.temperature) {
                System.out.println(broadlink.getTemperature());
            }

            if (commandLineArgs.receive) {
                IrSequence s = broadlink.receive();
                System.out.println(s);
            }

            if (commandLineArgs.transmit) {
                double[] data = new double[commandLineArgs.durations.size()];
                for (int i = 0; i < commandLineArgs.durations.size(); i++) {
                    data[i] = Double.parseDouble(commandLineArgs.durations.get(i));
                }

                ModulatedIrSequence irSequence = new ModulatedIrSequence(data, A_PRIOR_MODULATION_FREQUENCY);
                broadlink.sendIr(irSequence, commandLineArgs.count);
            }
            System.err.println("Nothing to do!!");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    public static Map<InetAddress, Broadlink> scanDevices() throws IOException {
        return scanDevices(null);
    }

    public static Map<InetAddress, Broadlink> scanDevices(Integer timeout) throws IOException {
        List<BLDevice> devs = BLDevice.discoverDevices(timeout == null ? DEFAULT_TIMEOUT : timeout);
        Map<InetAddress, Broadlink> map = new HashMap<>(8);
        for (BLDevice dev : devs) {
            map.put(InetAddress.getByName(dev.getHost()), new Broadlink(dev));
        }
        return map;
    }

//    public static Broadlink findFirstDevice() throws IOException {
//        return findFirstDevice(DEFAULT_TIMEOUT);
//    }
//
//    private static Broadlink findFirstDevice(int timeout) throws IOException {
//        BLDevice[] devs = BLDevice.discoverDevices(timeout);
//        return devs.length >  0 ? new Broadlink(devs[0]) : null;
//    }

    public static String expandIP(String IP) {
        return IP == null || IP.equals(Utils.DEFAULT) ? DEFAULT_HOST : IP;
    }

    public static String broadlinkHexString(byte[] data) {
        StringBuilder sb = new StringBuilder(4 * data.length);
        for (byte b : data)
            sb.append(String.format(HEX_STRING_FORMAT, Byte.toUnsignedInt(b)));
        return sb.toString();
    }

    public static String broadlinkHexString(ModulatedIrSequence irSequence, int count) {
        List<Integer> irData = broadlinkList(irSequence, count);
        StringBuilder sb = new StringBuilder(4 * irData.size());
        irData.forEach(chunk -> {
            sb.append(String.format(HEX_STRING_FORMAT, chunk));
        });
        return sb.toString();
    }

    public static String broadlinkBase64String(ModulatedIrSequence irSequence, int count) {
        byte[] bytearray = broadlinkData(irSequence, count);
        return Base64.getEncoder().encodeToString(bytearray);
    }

    private static List<Integer> broadlinkList(ModulatedIrSequence irSequence, int count) {
        boolean ir = irSequence.getFrequencyWithDefault() > 0;
        List<Integer> list = new ArrayList<>(2 * irSequence.getLength());
        list.add(ir ? IR_TOKEN : RF_433_TOKEN);
        list.add(ir ? count - 1 : Math.max(count, MIN_ARCTECH_REPEATS) - 1);
        list.add(0);
        list.add(0);
        for (int i = 0; i < irSequence.getLength() - 1; i++) { // ignoring final gap ...
            double duration = irSequence.get(i);
            int noTicks = (int) Math.round(duration / TICK);
            addEntry(list, noTicks);
        }
        addEntry(list, ir ? IR_ENDING_TOKEN : RF_433_ENDING_TOKEN); // ... and replacing it with the Broadlink ending token
        list.set(LENGTH_MSB_POS, list.size() / 256);
        list.set(LENGTH_LSB_POS, list.size() % 256);
        return list;
    }

    private static byte[] broadlinkData(ModulatedIrSequence irSequence, int count) {
        List<Integer> list = broadlinkList(irSequence, count);
        byte[] data = new byte[list.size()];
        for (int i = 0; i < data.length; i++)
            data[i] = list.get(i).byteValue();
        return data;
    }

    private static void addEntry(List<Integer> list, int noTicks) {
        if (noTicks > 255) {
            list.add(0);
            list.add(noTicks / 256);
        }
        list.add(noTicks % 256);
    }

    // Factory methods
    public static Broadlink newBroadlink(String hostname, Integer timeout) throws UnknownHostException, IOException  {
        Map<InetAddress, Broadlink> devs = scanDevices(timeout);
        if (devs.isEmpty())
            return null;
        if (hostname == null)
            return devs.values().iterator().next();
        InetAddress inetAddress = InetAddress.getByName(hostname);
        return devs.get(inetAddress);
    }

    public static Broadlink newBroadlink(String hostname) throws IOException {
        return newBroadlink(hostname, null);
    }

    /**
     * Returns "first" Broadlink device found
     * @return created Broadlink
     * @throws java.io.IOException
     */
    public static Broadlink newBroadlink() throws IOException {
        return newBroadlink(null);
    }

    private BLDevice dev;
    private boolean verbose;

    public Broadlink() throws IOException {
        this(false, null);
    }

    public Broadlink(BLDevice blDevice, boolean verbose) throws IOException {
        dev = blDevice;
        this.verbose = verbose;
    }

    public Broadlink(BLDevice blDevice) throws IOException {
        this(blDevice, false);
    }

    public Broadlink(Broadlink old) throws IOException {
        this(old.dev, old.verbose);
    }

    // Just for compatibility with org.harctoolbox.harchardware.Main
    public Broadlink(boolean verbose, Integer timeout) throws IOException {
        Broadlink bl = newBroadlink(null, timeout);
        this.dev = bl.dev;
        this.verbose = verbose;
    }

//    public Broadlink(String hostname, boolean verbose) throws IOException {
//        this(InetAddress.getByName(hostname), verbose, DEFAULT_TIMEOUT);
//    }

//    public Broadlink(InetAddress inetAddress, boolean verbose, Integer timeout) throws IOException {
//        Map<InetAddress, BLDevice> devs = scanDevices(timeout != null ? timeout : DEFAULT_TIMEOUT);
//        this.dev = null;
//        this.verbose = false;
//        for (BLDevice d : devs) {
//            if (inetAddress.equals(InetAddress.getByName(d.getHost()))) {
//                this.dev = d;
//                this.verbose = verbose;
//                return;
//            }
//        }
//        throw new IOException("No Broadlink with the address " + inetAddress.toString());
//    }

    // For compatibility with Main()
    public Broadlink(InetAddress inetAddress, Integer port, boolean verbose, Integer timeout) throws IOException {
        Broadlink bl = newBroadlink(inetAddress.getHostName(), timeout);
        this.dev = bl.dev;
        this.verbose = verbose;
    }

    public Broadlink(short deviceType, String host, String mac, boolean verbose) throws IOException {
        switch (deviceType) {

//            case BLDevice.DEV_RM_2:
//            case BLDevice.DEV_RM_MINI:
//            // ??? case BLDevice.DEV_RM_MINI_3:
//            case BLDevice.DEV_RM_PRO_PHICOMM:
//            case BLDevice.DEV_RM_2_HOME_PLUS:
//            case BLDevice.DEV_RM_2_2HOME_PLUS_GDT:
//            case BLDevice.DEV_RM_2_PRO_PLUS:
//            case BLDevice.DEV_RM_2_PRO_PLUS_2:
//            case BLDevice.DEV_RM_2_PRO_PLUS_2_BL:
//            case BLDevice.DEV_RM_MINI_SHATE:
            case 0x2712:
                dev = new RM2Device(deviceType, host, host, new Mac(mac));
                break;
            case 0x520c:
                dev = new RM4Device(deviceType, host, host, new Mac(mac));
                break;
            default:
                throw new UnsupportedOperationException("Currently not supported.");
        }
        this.verbose = verbose;
    }

    @Override
    public String toString() {
        return "Type: " + Integer.toHexString(dev.getDeviceType()) + " Host: " + dev.getHost() + " Mac: " + dev.getMac();
    }

    @Override
    public String getVersion() throws IOException {
        return null; // ???
    }

    @Override
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public void setDebug(int debug) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setTimeout(int timeout) throws IOException, HarcHardwareException {
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void open() throws HarcHardwareException, IOException {
        boolean success = dev.auth();
        if (!success)
            throw new HarcHardwareException("Authorization failed");
    }

    @Override
    public void close() throws IOException {
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private boolean sendIr(byte[] data) throws HarcHardwareException, NoSuchTransmitterException, IOException, InvalidArgumentException {
        if (!(dev instanceof RM2Device))
            throw new UnsupportedOperationException("Not yet supported.");

        if (verbose)
            System.err.println("Sending " + broadlinkHexString(data) + " to Broadlink: " + toString());
        SendDataCmdPayload cmdPayload = new SendDataCmdPayload(data);
        ((RM2Device) dev).sendCmdPkt(cmdPayload);
        return true;
    }

    public boolean sendIr(ModulatedIrSequence irSequence, int count) throws HarcHardwareException, NoSuchTransmitterException, IOException, InvalidArgumentException {
        byte[] data = broadlinkData(irSequence, count);
        return sendIr(data);
    }

    public boolean sendIr(IrSignal irSignal, int count) throws HarcHardwareException, NoSuchTransmitterException, IOException, InvalidArgumentException {
        int reps = irSignal.repeatsPerCountSemantic(count);
        ModulatedIrSequence irSequence = irSignal.toModulatedIrSequence(true, reps, true);
        return sendIr(irSequence, 1);
    }

    @Override
    public boolean sendIr(IrSignal irSignal, int count, Transmitter transmitter) throws HarcHardwareException, NoSuchTransmitterException, IOException, InvalidArgumentException {
        return sendIr(irSignal, count);
    }

    @Override
    public Transmitter getTransmitter() {
        return null;
    }

    @Override
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public IrSequence receive() throws HarcHardwareException, IOException, OddSequenceLengthException {
        if (!(dev instanceof RM2Device))
            throw new UnsupportedOperationException("Not yet supported.");

        RM2Device rm2 = (RM2Device) dev;
        boolean success = rm2.enterLearning();
        if (!success)
            throw new HarcHardwareException("Cannot enter learning mode");
        byte[] data = null;
        while (data == null) {
            try {
                data = ((RM2Device) this.dev).checkData();

            } catch (Exception ex) {
                //Logger.getLogger(Broadlink.class.getName()).log(Level.SEVERE, null, ex);
                throw new HarcHardwareException(ex);
            }
        }
        if (verbose)
            System.err.println("Received " + broadlinkHexString(data));

        try {
            return BroadlinkParser.parse(data);
        } catch (InvalidArgumentException ex) {
            throw new HarcHardwareException(ex);
        }
    }

    @Override
    public boolean stopReceive() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void setBeginTimeout(int integer) throws IOException, HarcHardwareException {
    }

    @Override
    public void setCaptureMaxSize(int integer) throws IOException, HarcHardwareException {
    }

    @Override
    public void setEndingTimeout(int integer) throws IOException, HarcHardwareException {
    }

    public double getTemperature() throws Exception { // FIXME
        if (!(dev instanceof RM2Device))
            throw new UnsupportedOperationException("Not yet supported.");

        return ((RM2Device) dev).getTemp();
    }

    private final static class CommandLineArgs {

        @Parameter(names = {"-a", "--address"}, description = "IP name or address of Broadlink")
        private String ip = null;

        @Parameter(names = {"-c", "-#", "--count"}, description = "Number of times to send the IR sequence")
        private int count = 1;

        @Parameter(names = {"-h", "--help", "-?"}, description = "Display help message")
        private boolean helpRequested = false;

        @Parameter(names = {"-r", "--receive", "-l", "--learn"}, description = "Receive (learn) IR signal")
        private boolean receive = false;

        @Parameter(names = {"-m", "--mac"}, description = "Mac address of Broadlink")
        private String mac = null;

        @Parameter(names = {"-S", "--scandevices"}, description = "Scan device on the LAN")
        private boolean scanDevices = false;

        @Parameter(names = {"-t", "--transmit", "-s", "--send"}, description = "Send a number  of duration as IR signal")
        private boolean transmit = false;

        @Parameter(names = {"--temperature"}, description = "Read temperature from the Broadlink")
        private boolean temperature = false;

        @Parameter(names = {"--timeout"}, description = "Timeout when discovering devices")
        private Integer timeout = null;

        @Parameter(names = {"-T", "--type"}, description = "Type of Broadlink")
        private Integer type = null;

        @Parameter(names = {"-v", "--verbose"}, description = "Have some commands executed verbosely")
        private boolean verbose;

        @Parameter(description = "Durations forming an IR signal to be sent")
        private List<String> durations = new ArrayList<>(132);
    }
}
