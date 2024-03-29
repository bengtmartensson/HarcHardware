/*
Copyright (C) 2009-2014 Bengt Martensson.

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

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.harchardware.ICommandLineDevice;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.Utils;
import org.harctoolbox.harchardware.beacon.AmxBeaconListener;
import org.harctoolbox.harchardware.comm.IBytesCommand;
import org.harctoolbox.harchardware.comm.IWeb;
import org.harctoolbox.harchardware.comm.TcpSocketChannel;
import org.harctoolbox.harchardware.comm.TcpSocketPort;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.Pronto;
import org.harctoolbox.irp.IrpUtils;

/**
 * This class implements the GlobalCache TCP Unified TCP API (Version 1.1),
 * according to <a href="https://www.globalcache.com/files/docs/API-GC-UnifiedTCPv1.1.pdf">the official API documentation</a>.
 * This covers the models GC-100, iTach, Flex and Global Connect.
 */
public class GlobalCache implements IHarcHardware, IRawIrSender, IIrSenderStop, ITransmitter, ICapture, IWeb {

    private static final Logger logger = Logger.getLogger(GlobalCache.class.getName());

    private final static int smallDelay = 10; // ms
    private final static int gcPort = 4998;
    private final static int gcFirstSerialPort = 4999;
    public  final static String DEFAULT_IP = "192.168.1.70";
    public  final static int defaultGlobalCachePort = 1;
    public  final static String sendIrPrefix = "sendir";
    private final static int defaultSocketTimeout = 2000;
    private final static int beaconTimeout = 60000; // Selected for GC-100, somewhat long for iTach[Flex]
    private final static int invalidDevice = -1;
    public  final static int connectorMin = 1;
    public  final static int connectorsPerModule = 3;
    public  final static int maxCompressedLetters = 15;

    /**
     * GlobalCache default connector
     */
    private final static int defaultConnector = 1;

    public static String expandIP(String IP) {
        return IP == null || IP.equals(Utils.DEFAULT) ? DEFAULT_IP : IP;
    }

    private static String camelCase2uppercase(String cc) {
        StringBuilder result = new StringBuilder(32);
        for (int i = 0; i < cc.length(); i++) {
            char ch = cc.charAt(i);
            if (Character.isUpperCase(ch))
                result.append('_');
            result.append(Character.toUpperCase(ch));
        }
        return result.toString();
    }

    private static boolean validConnector(int c) {
        return (c >= connectorMin) && (c <= connectorsPerModule + connectorMin - 1);
    }

    private static String gcJoiner(int[] array) {
        StringBuilder result = new StringBuilder(32);
        for (int i = 0; i < array.length; i++) {
            result.append(",").append(array[i]);
        }
        return result.toString();
    }

    private static String gcCompressedJoiner(int[]intro, int[]repeat) {
        Map<String, Character> index = new LinkedHashMap<>(32);
        return gcCompressedJoiner(intro, index) + gcCompressedJoiner(repeat, index);
    }

    private static String gcCompressedJoiner(int seq[], Map<String, Character> index) {
        StringBuilder result = new StringBuilder(32);
        for (int i = 0; i < seq.length / 2; i++) {
            String key = "," + seq[2 * i] + "," + seq[2 * i + 1];
            if (index.containsKey(key))
                result.append(index.get(key));
            else {
                if (index.size() < maxCompressedLetters) {
                    Character letter = (char) ('A' + (char) index.size());
                    index.put(key, letter);
                }
                result.append(key);
            }
        }
        return result.toString();
    }

    private static String globalCacheString(IrSignal code, int count, boolean compressed) {
        int[] intro = code.getIntroPulses();
        int[] repeat = code.getRepeatPulses();
        return (Long.toString(Math.round(code.getFrequency()))) + "," + count + "," + (1 + intro.length)
                + (compressed ? gcCompressedJoiner(intro, repeat) : (gcJoiner(intro) + gcJoiner(repeat)));
    }

    private static String transmitterAddress(int module, int connector) throws NoSuchTransmitterException {
        if (connector < connectorMin || connector > connectorsPerModule + connectorMin - 1)
            throw new NoSuchTransmitterException("" + module + ":" + connector);
        return "" + module + ":" + connector;
    }

    private static String transmitterAddress(int module) {
        return "" + module + ":1";
    }

    /**
     * Formats a string suitable for sending to a GlobalCache.
     *
     * @param code
     * @param count
     * @param module
     * @param connector
     * @param sendIndex
     * @param compressed
     * @return
     * @throws NoSuchTransmitterException
     */
    public static String sendIrString(IrSignal code, int count, int module, int connector, int sendIndex, boolean compressed) throws NoSuchTransmitterException {
        return sendIrPrefix + "," + transmitterAddress(module, connector)
                + "," + (sendIndex % 65536) + "," + globalCacheString(code, count, compressed);
    }

    public static AmxBeaconListener.Node listenBeacon(int timeout) {
        return AmxBeaconListener.listenFor("-Make", "GlobalCache", timeout);
    }

    public static AmxBeaconListener.Node listenBeacon() {
        return listenBeacon(beaconTimeout);
    }

    public static AmxBeaconListener newListener(AmxBeaconListener.Callback callback) {
        AmxBeaconListener abl = new AmxBeaconListener(callback,  "-Make", "GlobalCache");
        abl.start();
        return abl;
    }

    /**
     * Parses a string intended as command to a GlobalCache and return it as a IrSignal.
     * Presently only the uncompressed form is parsed.
     *
     * @param gcString
     * @return IrSignal representing the signal, with begin and repeat part.
     * @throws org.harctoolbox.ircore.InvalidArgumentException
     */
    public static IrSignal parse(String gcString) throws InvalidArgumentException {
        String[] chunks = gcString.trim().split(",");
        if (chunks.length < 6)
            return null;
        int index = 0;
        if (!chunks[index].trim().equals(sendIrPrefix))
            return null;
        index++;

        index++; // module, discard
        index++; // send index, discard
        int frequency = Integer.parseInt(chunks[index].trim());
        index++;
        index++; // number repetitions, discard
        int repIndex = Integer.parseInt(chunks[index].trim());
        index++;
        int[] durations = new int[chunks.length - index];
        double T = 1000000f / (double) frequency; // period time in micro seconds
        for (int i = 0; i < chunks.length - index; i++)
            durations[i] = (int) Math.round(Integer.parseInt(chunks[i + index]) * T);

        return new IrSignal(durations, repIndex - 1, durations.length - repIndex + 1, frequency);
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void usage() {
        System.err.println("Usagex:");
        System.err.println("GlobalCache [options] <command> [<argument>]");
        System.err.println("where options=-# <count>,-h <hostname>,-c <connector>,-m <module>,-b <baudrate>,-t <timeout>,-p <sendirstring>,-v,-B,-j");
        System.err.println("and command=send_ir" /*,send_serial,listen_serial,*/
                + ",set_relay,get_devices,get_version,set_blink,[set|get]_serial,[set|get]_ir,[set|get]_net,[get|set]_state,get_learn,ccf");
        doExit(IrpUtils.EXIT_USAGE_ERROR);
    }

    private static void doExit(int exitcode) {
        System.exit(exitcode);
    }

    // Probably not entirely working...
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] args) throws InvalidArgumentException {
        //String str = "sendir,4:1,0,38380,1,69,347,173,22,22,22,22,22,22,22,22,22,22,22,22,22,22,22,65,22,65,22,65,22,65,22,65,22,65,22,65,22,65,22,22,22,22,22,65,22,22,22,22,22,22,22,22,22,22,22,22,22,65,22,22,22,65,22,65,22,65,22,65,22,65,22,65,22,1527,347,87,22,3692";
        String hostname = DEFAULT_IP;
        int connector = 1;
        int module = 2;
        int count = 1;
        boolean verbose = false;
        boolean beacon = false;
        int baudrate = 0; // invalid value
        GlobalCache gc;
        String cmd;
        String arg;
        int arg_i = 0;
        int timeout = defaultSocketTimeout;
        String parserFood = null;

        if (args.length == 0)
            usage();

        try {
            while (arg_i < args.length && args[arg_i].charAt(0) == '-') {
                switch (args[arg_i]) {
                    case "-h":
                        hostname = args[arg_i + 1];
                        arg_i += 2;
                        break;
                    case "-m":
                        module = Integer.parseInt(args[arg_i + 1]);
                        arg_i += 2;
                        break;
                    case "-c":
                        connector = Integer.parseInt(args[arg_i + 1]);
                        arg_i += 2;
                        break;
                    case "-#":
                        count = Integer.parseInt(args[arg_i + 1]);
                        arg_i += 2;
                        break;
                    case "-b":
                        baudrate = Integer.parseInt(args[arg_i + 1]);
                        arg_i += 2;
                        break;
                    case "-v":
                        verbose = true;
                        arg_i++;
                        break;
                    case "-t":
                        timeout = Integer.parseInt(args[arg_i + 1]);
                        arg_i += 2;
                        break;
                    case "-B":
                        beacon = true;
                        arg_i++;
                        break;
                    case "-p":
                        parserFood = args[arg_i + 1];
                        arg_i += 2;
                        break;
                    default:
                        usage();
                        break;
                }
            }

        } catch (ArrayIndexOutOfBoundsException ex) {
            usage();
        } catch (NumberFormatException ex) {
            System.err.println(ex.getMessage());
        }

        if (parserFood != null) {
            IrSignal sig = parse(parserFood);
            System.out.println(sig);
            System.exit(IrpUtils.EXIT_SUCCESS);
        }

        String output = "";

        if (beacon) {
            AmxBeaconListener.Node r = listenBeacon();
            if (r != null) {
                String model = r.get("-Model");
                //GlobalCacheModel gcModel = GlobalCacheModel.newGlobalCacheModel(model);

                //System.err.println(r + gcModel.getName());
                hostname = r.getInetAddress().getHostName();
            } else {
                System.err.println("none found");
                System.exit(1);
            }
        }


        try {
            gc = new GlobalCache(hostname, verbose, timeout);

            if (args.length - 1 < arg_i)
                System.exit(IrpUtils.EXIT_SUCCESS);

            cmd = args[arg_i];
            arg = (args.length > arg_i + 1) ? args[arg_i + 1] : null;


            switch (cmd) {
                case "set_blink":
                    if (arg == null || !arg.equals("1")) {
                        gc.setBlink(0);
                    } else {
                        gc.setBlink(1);
                    }   break;
                case "get_devices":
                    output = String.join(System.getProperty("line.separator"), gc.getDevices());
                    break;
                case "get_version":
                    output = gc.getVersion(module);
                    break;
                case "get_net":
                    // Only v3
                    output = gc.getNet();
                    break;
                case "set_net":
                    // Only v3
                    // Syntax: see API-document
                    output = gc.setNet(arg);
                    break;
                case "get_ir":
                    //Only v3
                    output = gc.getIr(module, connector);
                    break;
                case "set_ir":
                    output = gc.setIr(module, connector, arg);
                    break;
                case "get_serial":
                    output = gc.getSerial(module);
                    break;
                case "set_serial":
                    if (baudrate > 0) {
                        output = gc.setSerial(module, baudrate);
                    } else {
                        output = gc.setSerial(module, arg);
                    }   break;
                case "get_state":
                    output = gc.getState(module, connector) == 1 ? "on" : "off";
                    break;
                case "toggle_state":
                    output = gc.toggleState(connector) ? "ok" : "not ok";
                    break;
                case "set_state":
                case "set_relay":
                    // Just a convenience version of the above
                    output = gc.setState(connector, (arg == null || arg.equals("0") ? 0 : 1)) ? "on" : "off";
                    break;
                case "send_ir":
                    if (arg_i + 2 == args.length) {
                        output = gc.sendIr(args[arg_i + 1]) ? "ok" : "error";
                    } else {
                        StringBuilder ccf = new StringBuilder(6 * args.length);
                        for (int i = arg_i + 1; i < args.length; i++)
                            ccf.append(' ').append(args[i]);

                        IrSignal irSignal = Pronto.parse(ccf.toString());
                        output = gc.sendIr(irSignal, count, module, connector) ? "ok" : "error";
                    }   break;
                // Currently broken
                //case "send_serial":
                //    StringBuilder transmit = new StringBuilder(2 * args.length);
                //    for (int i = arg_i + 1; i < args.length; i++) {
                //        transmit.append(' ').append(args[i]);
                //    }

                //      output = gc.sendStringCommand(module, transmit, 0);
                //    break;
                case "stop_ir":
                    gc.stopIr(module, connector);
                    break;
                case "get_learn":
                    ModulatedIrSequence seq = gc.capture();
                    System.out.println(seq);
                    break;
                    // Currently broken
                    //case "listen_serial":
                    // System.err.println("Press Ctrl-C to interrupt.");
                    // Never returns
                    //gc.listenStringCommands(module);
                    //} else if (cmd.equals("ccf")) {
                    //    String s = "";
                    //    for (int i = arg_i + 1; i < args.length; i++)
                    //        s = s + args[i];
                    //    System.out.println(gc2Ccf(s));
                    //break;
                default:
                    usage();
                    break;
            }
            gc.close();
        } catch (NoSuchTransmitterException e) {
            System.err.println(e.getMessage());
        } catch (UnknownHostException e) {
            System.err.println("Host \"" + hostname + "\" does not resolve.");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("IOException occured.");
            System.exit(1);
        } catch (Pronto.NonProntoFormatException ex) {
            Logger.getLogger(GlobalCache.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.err.println(output);
    }

    private String hostIp;
    private InetAddress inetAddress;
    private int timeout = defaultSocketTimeout;
    private boolean verbose = true;
    private String[] getdevicesResult = null;

    private TcpSocketChannel tcpSocketChannel;
    private SerialPort[] serialPorts;

    private List<Integer> irModules;
    private List<Integer> serialModules;
    private List<Integer> relayModules;

    private boolean compressed = false;

   /**
     * Global Cache default module for ir communication
     */
    private int firstIrModule = 2; // right for gc_100_06, wrong for others

    /**
     * GlobalCache default relay module
     */
    private int defaultRelayModule = 3; // right for gc_100_12, wrong for others

    /**
     * GlobalCache default serial module
     */
    private int defaultSerialModule = 1; // mostly right

    /**
     * The Global Cache IR index.
     * Should turn around at 65536, see GC API docs.
     */
    private int sendIndex;

    public GlobalCache(InetAddress inetAddress, boolean verbose, Integer timeout, boolean compressed) throws IOException {
        this.timeout = timeout != null ? timeout : defaultSocketTimeout;
        this.hostIp = inetAddress.getHostAddress();
        this.inetAddress = inetAddress;
        this.verbose = verbose;
        this.compressed = compressed;
        open();
    }

    public GlobalCache(String hostIp, boolean verbose, Integer timeout, boolean compressed) throws UnknownHostException, IOException {
        this(InetAddress.getByName(expandIP(hostIp)), verbose, timeout, compressed);
    }

    public GlobalCache(InetAddress inetAddress, Integer portIsNotUsed, boolean verbose, Integer timeout) throws IOException {
        this(inetAddress, verbose, timeout, false);
    }

    public GlobalCache(InetAddress inetAddress, boolean verbose, Integer portNumber, Integer timeout) throws IOException {
        // ignores portNumber
        this(inetAddress, verbose, timeout, false);
        if (portNumber != null)
            logger.log(Level.WARNING, "Explicit port (=({0}) ignored.", portNumber);
    }

    public GlobalCache(String hostIp, boolean verbose, Integer timeout) throws UnknownHostException, IOException {
        this(hostIp, verbose, timeout, false);
    }

    public GlobalCache(String hostname) throws UnknownHostException, IOException {
        this(hostname, false);
    }

    public GlobalCache(String hostname, boolean verbose, boolean compressed) throws UnknownHostException, IOException {
        this(hostname, verbose, null, compressed);
    }

    public GlobalCache(String hostname, boolean verbose) throws UnknownHostException, IOException {
        this(hostname, verbose, null);
    }

    @Override
    public URI getUri(String user, String password) {
        try {
            return new URI("http", hostIp, null, null);
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    /**
     * Returns a serial port from the GlobalCache.
     * @param portNumber 1-based port  number, i.e. use 1 for first, not 0.
     * @return serial port implementing ICommandLineDevice
     * @throws NoSuchTransmitterException
     */
    public synchronized SerialPort getSerialPort(int portNumber) throws NoSuchTransmitterException {
        int portIndex = portNumber - 1;
        if (portIndex < 0 || portIndex > serialPorts.length - 1)
            throw new NoSuchTransmitterException(Integer.toString(portNumber));

        if (serialPorts[portIndex] == null) {
            serialPorts[portIndex] = new SerialPort(portNumber);
        } else {
            throw new NoSuchTransmitterException("Requested port " + portNumber
                    + " in GlobalCache " + hostIp + " is already in use.");
        }
        return serialPorts[portIndex];
    }

    @Override
    public GlobalCacheIrTransmitter getTransmitter(String str) throws NoSuchTransmitterException {
        if (str == null || str.isEmpty())
            return new GlobalCacheIrTransmitter();

        String[] s = str.trim().split(":");
        return s.length == 1
                ? new GlobalCacheIrTransmitter(Integer.parseInt(s[0]))
                : new GlobalCacheIrTransmitter(Integer.parseInt(s[0]), Integer.parseInt(s[1]));
    }

    @Override
    public GlobalCacheIrTransmitter getTransmitter() {
        return new GlobalCacheIrTransmitter();
    }

    public GlobalCacheIrTransmitter newTransmitter(int module, int port) throws NoSuchTransmitterException {
        return new GlobalCacheIrTransmitter(module, port);
    }

    public GlobalCacheIrTransmitter newTransmitter(int cardinal) throws NoSuchTransmitterException {
        int module = (cardinal - 1)/ connectorsPerModule + this.firstIrModule;
        int port = (cardinal - 1) % connectorsPerModule + 1;
        return new GlobalCacheIrTransmitter(module, port);
    }

    @Override
    public String[] getTransmitterNames() {
        String[] result = new String[irModules.size() * connectorsPerModule];
        int index = 0;
        for (Integer module : irModules) {
            for (int trans = 0; trans < connectorsPerModule; trans++) {
                result[index] = module.toString() + ":" + (trans + connectorMin);
                index++;
            }
        }
        return result;
    }

    private GlobalCacheIrTransmitter newGlobalCacheIrTransmitter(Transmitter trans) {
         GlobalCacheIrTransmitter tr = trans == null
                 ? new GlobalCacheIrTransmitter()
                 : (GlobalCacheIrTransmitter) trans;
         tr.normalize(firstIrModule);
         return tr;
    }

    @Override
    public void setTimeout(int timeout) {
        this.timeout = timeout;
        if (tcpSocketChannel != null && tcpSocketChannel.isValid())
            try {
                tcpSocketChannel.setTimeout(timeout);
            } catch (SocketException ex) {
            }
    }

    @Override
    @SuppressWarnings("null")
    public final void open() throws UnknownHostException, IOException {
        if (tcpSocketChannel != null)
            return; // already open

        tcpSocketChannel = new TcpSocketChannel(this.hostIp, gcPort, timeout,
                verbose, TcpSocketPort.ConnectionMode.keepAlive);
        getdevicesResult = sendCommand("getdevices", -1);

        irModules = getIrModules();
        firstIrModule = irModules.isEmpty() ? invalidDevice : irModules.get(0);
        serialModules = getSerialModules();
        defaultSerialModule = serialModules.isEmpty() ? invalidDevice : serialModules.get(0);
        serialPorts = new SerialPort[serialModules.size()];
        relayModules = getRelayModules();
        defaultRelayModule = relayModules.isEmpty() ? invalidDevice : relayModules.get(0);
    }

    public String getIp() {
        return hostIp;
    }

    @Override
    public synchronized void close() throws IOException {
        if (tcpSocketChannel != null) {
            tcpSocketChannel.close(true);
            tcpSocketChannel = null;
        }
        getdevicesResult = null;
    }

    @Override
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public void setDebug(int debug) {
    }

    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    @SuppressWarnings({"SleepWhileHoldingLock", "UseOfSystemOutOrSystemErr"})
    private synchronized String[] sendCommand(String cmd, int noLines, int delay, String expectedFirstLine) throws IOException {
        if (verbose && cmd != null)
            System.err.println("Sending command \"" + cmd + "\" to GlobalCache (" + hostIp + ")");

        tcpSocketChannel.connect();
        if (noLines != 0)
            while (tcpSocketChannel.getBufferedIn().ready()) {
                tcpSocketChannel.readString();
            }

        if (cmd != null) {
            tcpSocketChannel.sendString(cmd + '\r');

            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ex) {
                }
            }
        }
        String[] result;
        if (noLines >= 0) {
            result = new String[noLines];
            for (int i = 0; i < noLines; i++) {
                result[i] = tcpSocketChannel.readString();// throws SocketTimeoutException after so_timeout
               if (i == 0 && expectedFirstLine != null)
                    if (!result[0].startsWith(expectedFirstLine)) {
                        logger.log(Level.WARNING, "Expected \"{0}\", returning immediately.", expectedFirstLine);
                        break;
                    }
            }
        } else {
            List<String> array = new ArrayList<>(8);
            String lineRead = tcpSocketChannel.readString(); // throws SocketTimeoutException after so_timeout
            if (lineRead != null) {
                array.add(lineRead);
                while (tcpSocketChannel.getBufferedIn().ready()) {
                    String resp = tcpSocketChannel.readString();
                    array.add(resp);
                }
            }
            result = array.toArray(new String[0]);
        }
        tcpSocketChannel.close(false);
        return result;
    }

    private String sendCommand(String cmd) throws IOException {
        return sendCommand(cmd, 1, smallDelay, null)[0];
    }

    private String[] sendCommand(String cmd, int noLines) throws IOException {
        return sendCommand(cmd, noLines, smallDelay, null);
    }

    public boolean stopIr(int module, int connector) throws IOException, NoSuchTransmitterException {
        sendCommand("stopir," + transmitterAddress(module, connector), 1);
        return true;
    }

    @Override
    public boolean stopIr(Transmitter transmitter) throws IOException, NoSuchTransmitterException {
        GlobalCacheIrTransmitter gctransmitter = newGlobalCacheIrTransmitter(transmitter);
        return stopIr(gctransmitter.module, gctransmitter.port);
    }

    public boolean sendIr(String cmd) throws IOException {
        String result = sendCommand(cmd);
        return result.startsWith("completeir");
    }

    public String sendIrString(IrSignal code, int count, int module, int connector) throws NoSuchTransmitterException {
        String cmd = sendIrString(code, count, module, connector, sendIndex, compressed);
        sendIndex = (sendIndex + 1) % 65536;
        return cmd;
    }

    public boolean sendIr(IrSignal code, int count, int module, int connector) throws NoSuchTransmitterException, IOException {
        if (!validConnector(connector))
            throw new NoSuchTransmitterException(Integer.toString(connector));
        String cmd = sendIrString(code, count, module, connector);
        return sendIr(cmd);
    }

    public boolean sendIr(IrSignal code, int count, int connector) throws IOException, NoSuchTransmitterException {
        return sendIr(code, count, new GlobalCacheIrTransmitter(connector));
    }

    public boolean sendIr(IrSignal code, int connector) throws IOException, NoSuchTransmitterException {
        return sendIr(code, 1, new GlobalCacheIrTransmitter(connector));
    }

    public boolean sendIr(IrSignal code) throws IOException, NoSuchTransmitterException {
        return sendIr(code, 1, null);
    }

    @Override
    public boolean sendIr(IrSignal code, int count, Transmitter transmitter) throws NoSuchTransmitterException, IOException {
        GlobalCacheIrTransmitter gct = newGlobalCacheIrTransmitter(transmitter);
        return sendIr(code, count, gct.module, gct.port);
    }

    public String[] getDevices() {
        return getdevicesResult.clone();
    }

    // Meaningful only on GC-100.
    public String getVersion(int module) throws IOException {
        return sendCommand("getversion," + module);
    }

    @Override
    public String getVersion() throws IOException {
        String answer = sendCommand("getversion");
        return answer.replaceFirst("version,", "");
    }

    public String getNet() throws IOException {
        return sendCommand("get_NET,0:1");
    }

    public String setNet(String arg) throws IOException {
        return sendCommand("set_NET,0:1," + arg);
    }

    public String getIr(int module, int connector) throws IOException, NoSuchTransmitterException {
        return sendCommand("get_IR," + transmitterAddress(module, connector));
    }

    private List<Integer> getModules(String moduleType) {
        List<Integer> modules = new ArrayList<>(5);
        for (String devicesResult : getdevicesResult) {
            String[] s = devicesResult.split(" ");
            if (s.length > 1 && s[1].startsWith(moduleType))
                modules.add(Integer.valueOf(devicesResult.substring(7, 8)));
        }
        return modules;
    }

    public int getModuleSecondNumber(int moduleNumber) {
        for (String deviceResult : getdevicesResult) {
            String[] str = deviceResult.split(",");
            int actual = Integer.parseInt(str[1]);
            if (actual == moduleNumber)
                return Integer.parseInt(str[2].substring(0, 1));
        }
        throw new IllegalArgumentException("Non-existing module: " + moduleNumber);
    }

    public final List<Integer> getIrModules() {
        return getModules("IR");
    }

    public final List<Integer> getSerialModules() {
        return getModules("SERIAL");
    }

    public final List<Integer> getRelayModules() {
        return getModules("RELAY");
    }

    // Appears not to be working on my iTachFlex, just returns ERR 005.
    public String setIr(int module, int connector, String modestr) throws IOException, NoSuchTransmitterException {
        return sendCommand("set_IR," + transmitterAddress(module, connector) + "," + modestr.toUpperCase(Locale.US));
    }

    public String setIr(int module, int connector, IrConfiguration mode) throws IOException, NoSuchTransmitterException {
        return setIr(module, connector, mode.toString());
    }

    public String getSerial(int module) throws IOException {
        return sendCommand("get_SERIAL," + transmitterAddress(module));
    }

    public String getSerial() throws IOException {
        return getSerial(defaultSerialModule);
    }

    public String setSerial(int module, String arg) throws IOException {
        return sendCommand("set_SERIAL," + transmitterAddress(module) + "," + arg);
    }

    public String setSerial(String arg) throws IOException {
        return setSerial(defaultSerialModule, arg);
    }

    public String setSerial(int module, int baudrate) throws IOException {
        return sendCommand("set_SERIAL," + transmitterAddress(module) + "," + baudrate);
    }

    public String setSerial(int baudrate) throws IOException {
        return setSerial(defaultSerialModule, baudrate);
    }

    public int getState(int module, int connector) throws IOException, NoSuchTransmitterException {
        return Integer.parseInt(sendCommand("getstate," + transmitterAddress(module,
                    connector)).substring(10, 11));
    }

    public int getState(int connector) throws IOException, NoSuchTransmitterException {
        return getState(firstIrModule + ((connector - connectorMin) / connectorsPerModule), (connector - connectorMin) % connectorsPerModule + connectorMin);
    }

    // Seems quite inefficient
    public boolean toggleState(int module, int connector) throws IOException, NoSuchTransmitterException {
        return setState(module, connector, 1 - getState(module, connector));
    }

    public boolean toggleState(int connector) throws IOException, NoSuchTransmitterException {
        return toggleState(defaultRelayModule, connector);
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public boolean setState(int module, int connector, int state) throws IOException, NoSuchTransmitterException {
        String result = sendCommand("setstate," + transmitterAddress(module, connector) + "," + (state == 0 ? 0 : 1));
        if (verbose) {
            System.err.println(result.substring(0, 5));
        }
        return result.substring(0, 5).equals("state");
    }

    public boolean setState(int connector, int state) throws IOException, NoSuchTransmitterException {
        return setState(defaultRelayModule, connector, state);
    }

    public boolean setState(int connector, boolean onOff) throws IOException, NoSuchTransmitterException {
        return setState(connector, onOff ? 1 : 0);
    }

    /**
     *
     * @param module
     * @param connector
     * @return
     * @throws IOException
     * @throws NoSuchTransmitterException
     */
    public boolean pulseState(int module, int connector) throws NoSuchTransmitterException, IOException {
        sendCommand("setstate," + transmitterAddress(module, connector) + ",1", 0, 300, null);
        sendCommand("setstate," + transmitterAddress(module, connector) + ",0", 0, 0, null);
        return true;
    }

    /**
     *
     * @param connector
     * @return
     * @throws IOException
     * @throws NoSuchTransmitterException
     */
    public boolean pulseState(int connector) throws NoSuchTransmitterException, IOException {
        return pulseState(defaultRelayModule, connector);
    }

    // GC-100 only
    public void setBlink(int arg) throws IOException {
        sendCommand("blink," + arg, 0);
    }

    @Override
    public boolean isValid() {
        return this.getdevicesResult != null;
    }

    @Override
    // Only iTach, Flex GlobalConnect
    public synchronized ModulatedIrSequence capture() throws InvalidArgumentException {
        try {
            String[] result = sendCommand("get_IRL", 1, smallDelay, null);
            if (result[0].equals("IR Learner Enabled"))
                result = sendCommand(null, 1, 0, null);

            IrSignal signal = parse(result[0]);
            return signal != null ? signal.toModulatedIrSequence(1) : null;
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public boolean stopCapture() {
        String response;
        try {
            response = sendCommand("stop_IRL");
        } catch (IOException ex) {
            return false;
        }
        return response.startsWith("IR Learner Disabled");
    }

    @Override
    public void setBeginTimeout(int timeout) throws IOException {
        tcpSocketChannel.setTimeout(timeout);
    }

    @Override
    public void setCaptureMaxSize(int integer) {
    }

    @Override
    public void setEndingTimeout(int integer) {
    }

    public static class GlobalCacheIrTransmitter extends Transmitter {
        private int module; // 1,2,4,5 (on present models)
        private int port; // 1,2,3

        private GlobalCacheIrTransmitter(int module, int port) throws NoSuchTransmitterException {
            if (!GlobalCache.validConnector(port))
                throw new NoSuchTransmitterException(Integer.toString(port));

            this.module = module;
            this.port = port;
        }

        private GlobalCacheIrTransmitter(int port) throws NoSuchTransmitterException {
            this(invalidDevice, port);
        }

        private GlobalCacheIrTransmitter() {
            this.module = invalidDevice;
            this.port = defaultConnector;
        }

        private void normalize(int defaultIrModule) {
            if (module == invalidDevice) {
                module = (port - connectorMin)/connectorsPerModule + defaultIrModule;
                port = (port - connectorMin) % connectorsPerModule + connectorMin;
            }
        }

        @Override
        public String toString() {
            return Integer.toString(module) + ":" + Integer.toString(port);
        }
    }

    /**
     * Possible arguments to the set_IR command.
     */
    public enum IrConfiguration {
        ir, // all
        sensor, // GC-100, iTach
        sensorNotify, // GC-100, iTach
        irNocarrier, // GC-100
        serial, // iTachFlex
        irBlaster, // iTach, iTachFlex
        irtriport, // iTachFlex
        irtriportBlaster, // iTachFlex
        ledLighting;   //iTach

        @Override
        public String toString() {
            return camelCase2uppercase(this.name());
        }
    }

    public enum ModuleType {
        unknown,
        ir,         // all
        //three_ir,   // iTach
        //irBlaster, // only iTachFlex
        //irtriport,  // only iTachFlex
        //irtriportBlaster, // only iTachFlex
        serial,     // all
        //one_serial, // iTach
        relay,      // GC-100
        //three_relay,// iTach
        wifi,       // iTach, iTachFlex
        ethernet,   // iTach, ITachFlex
        net;

        @Override
        public String toString() {
            return camelCase2uppercase(this.name());
        }
    }

    public class SerialPort implements ICommandLineDevice, IBytesCommand {
        private final int portIndex; // 0 or 1, i.e. zero based.
        private TcpSocketChannel tcpSocketChannel;

        /** Do not use, use getSerialPort instead. */
        private SerialPort(int portNumber) throws NoSuchTransmitterException {
            portIndex = portNumber - 1;
            if (portIndex < 0 || portIndex > serialPorts.length - 1)
                throw new NoSuchTransmitterException(Integer.toString(portNumber));

            tcpSocketChannel = new TcpSocketChannel(inetAddress, gcFirstSerialPort + portIndex,
                    timeout, verbose, TcpSocketPort.ConnectionMode.keepAlive);
        }

        @Override
        public void sendString(String cmd) throws IOException {
            sendBytes(cmd.getBytes(Charset.forName("US-ASCII")));
        }

        @Override
        public synchronized String readString(boolean wait) throws IOException {
            tcpSocketChannel.connect();
            String result = tcpSocketChannel.readString(wait);
            tcpSocketChannel.close(false);
            return result;
        }

        @Override
        public synchronized String readString() throws IOException {
            return readString(false);
        }

        @Override
        public synchronized void close() throws IOException {
            tcpSocketChannel.close(true);
            serialPorts[portIndex] = null;
        }

        @Override
        public synchronized void sendBytes(byte[] cmd) throws IOException {
            tcpSocketChannel.connect();
            tcpSocketChannel.getOut().write(cmd);
            tcpSocketChannel.close(false);
        }

        @Override
        public synchronized byte[] readBytes(int length) throws IOException {
            tcpSocketChannel.connect();
            byte[] result = Utils.readBytes(tcpSocketChannel.getIn(), length);
            tcpSocketChannel.close(false);
            return result;
        }

        @Override
        public String getVersion() {
            return tcpSocketChannel.getVersion();
        }

        @Override
        public void setVerbose(boolean verbose) {
            tcpSocketChannel.setVerbose(verbose);
        }

        @Override
        public void setDebug(int debug) {
            tcpSocketChannel.setDebug(debug);
        }

        @Override
        public void setTimeout(int timeout) throws SocketException {
            tcpSocketChannel.setTimeout(timeout);
        }

        @Override
        public boolean isValid() {
            return tcpSocketChannel.isValid();
        }

        @Override
        public void open() {
            tcpSocketChannel.open();
        }

        @Override
        public boolean ready() throws IOException {
            return tcpSocketChannel.ready();
        }

        @Override
        public void flushInput() throws IOException {
            tcpSocketChannel.flushInput();
        }
    }
}
