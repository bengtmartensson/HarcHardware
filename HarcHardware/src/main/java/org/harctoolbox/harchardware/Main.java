/*
Copyright (C) 2012, 2013, 2014, 2019 Bengt Martensson.

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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.internal.DefaultConsole;
import java.io.PrintStream;
import org.harctoolbox.irp.IrpUtils;

/**
 * Gives possibilities to invoke many of the functions from the command line. Demonstrates the interfaces.
 */
public class Main {

    private final static int INVALID_PORT = -1;
    //private static IHarcHardware hardware = null;

    private static JCommander argumentParser;
    private static CommandLineArgs commandLineArgs;
    private static CommandHelp commandHelp;
    private static CommandVersion commandVersion;
    private static CommandTransmit commandTransmit;
    private static CommandReceive commandReceive;
    private static CommandCapture commandCapture;
    private static CommandGetRemotes commandGetRemotes;
    private static CommandGetCommands commandGetCommands;

//    private static final Thread closeOnShutdown = new Thread() {
//        @Override
//        public void run() {
//            try {
//                //System.err.println("Running shutdown");
//                if (harcHardware != null)
//                    harcHardware.close();
//            } catch (IOException e) {
//                System.err.println(e.getMessage());
//            }
//        }
//    };

//    private static void printTable(String title, String[] arr, PrintStream str) {
//        if (arr != null) {
//            str.println(title);
//            str.println();
//            for (String s : arr) {
//                str.println(s);
//            }
//        }
//    }

//    private static int noTrue(boolean... bool) {
//        int sum = 0;
//        for (boolean b : bool)
//            if (b)
//                sum++;
//
//        return sum;
//    }

    private static void usage(int exitcode) {
        PrintStream printStream = exitcode == IrpUtils.EXIT_SUCCESS ? System.out : System.err;
        argumentParser.setConsole(new DefaultConsole(printStream));
        argumentParser.usage();

        printStream.println("\n"
                + "parameters: <protocol> <deviceno> [<subdevice_no>] commandno [<toggle>]\n"
                + "   or       <Pronto code>");

        doExit(exitcode);
    }

    private static void doExit(int exitcode) {
        System.exit(exitcode);
    }

    public static void main(String[] args) {
        commandLineArgs = new CommandLineArgs();
        argumentParser = new JCommander(commandLineArgs);
        argumentParser.setProgramName("HarcHardware");
        argumentParser.setAllowAbbreviatedOptions(true);

        commandHelp = new CommandHelp();
        argumentParser.addCommand(commandHelp);

        commandVersion = new CommandVersion();
        argumentParser.addCommand(commandVersion);

        commandTransmit = new CommandTransmit();
        argumentParser.addCommand(commandTransmit);

        commandReceive = new CommandReceive();
        argumentParser.addCommand(commandReceive);

        commandCapture = new CommandCapture();
        argumentParser.addCommand(commandCapture);

        commandGetRemotes = new CommandGetRemotes();
        argumentParser.addCommand(commandGetRemotes);

        commandGetCommands = new CommandGetCommands();
        argumentParser.addCommand(commandGetCommands);

        try {
            argumentParser.parse(args);
        } catch (ParameterException | NumberFormatException ex) {
            System.err.println(ex.getMessage());
            usage(IrpUtils.EXIT_USAGE_ERROR);
        }

        String command = commandLineArgs.helpRequested ? "help"
                    : commandLineArgs.version ? "version"
                    : argumentParser.getParsedCommand();

        if (command == null)
            programExit(IrpUtils.EXIT_USAGE_ERROR, "No command given.");
        else // For findbugs...
            switch (command) {
                case "help":
                    usage(IrpUtils.EXIT_SUCCESS);
                    break;
                case "version":
                    System.out.println(Version.versionString);
                    System.out.println("JVM: " + System.getProperty("java.vendor") + " " + System.getProperty("java.version") + " " + System.getProperty("os.name") + "-" + System.getProperty("os.arch"));
                    System.out.println();
                    System.out.println(Version.licenseString);
                    doExit(IrpUtils.EXIT_SUCCESS);
                    break;
                case "transmit":
                case "send":
                    break;
                case "receive":
                    break;
                case "capture":
                    break;
                case "getremotes":
                    break;
                case "getcommands":
                    break;
                default:
                    programExit(IrpUtils.EXIT_USAGE_ERROR, "Unknown command: " + command);
            }
    }

    private static void programExit(int exitstatus, String message) {
        (exitstatus == IrpUtils.EXIT_SUCCESS ? System.out : System.err).println(message);
        System.exit(exitstatus);
    }

        //Runtime.getRuntime().addShutdownHook(closeOnShutdown);

//        int noHardware = noTrue(commandLineArgs.lirc, commandLineArgs.globalcache, commandLineArgs.irtrans, commandLineArgs.irtoy, commandLineArgs.irwidget, commandLineArgs.arduino);

//        if (noHardware == 0 && commandLineArgs.beacon) {
//            System.err.println("Listening for AMX Beacons for " + commandLineArgs.timeout/1000 + " seconds, be patient.");
//            Collection<AmxBeaconListener.Node> nodes = AmxBeaconListener.listen(commandLineArgs.timeout, commandLineArgs.verbose);
//            nodes.forEach((node) -> {
//                System.out.println(node);
//            });
//            doExit(IrpUtils.EXIT_SUCCESS);
//        }

//        if (noHardware != 1) {
//            System.err.println("Exactly one hardware device must be given.");
//            doExit(IrpUtils.EXIT_USAGE_ERROR);
//        }

        boolean didSomethingUseful = false;
//        GlobalCache globalCache;
//        IrTransIRDB irTrans;
//        IrToy irtoy;
//        LircCcfClient lircClient;
//        IrWidget irWidget;
//        IRemoteCommandIrSender remoteCommandIrSender = null;
//        IRawIrSender rawIrSender = null;
//        ICapture captureDevice = null;
//        Transmitter transmitter = null;

//        try {
//            if (commandLineArgs.globalcache) {
//                if (commandLineArgs.port != invalidPort)
//                    System.err.println("Port for GlobalCache not implemented, ignoring.");
//                String gcHostname = commandLineArgs.ip != null ? commandLineArgs.ip : GlobalCache.defaultGlobalCacheIP;
//                if (commandLineArgs.beacon && commandLineArgs.globalcache) {
//                    System.err.print("Invoking beacon listener, taking first response, be patient...");
//                    AmxBeaconListener.Node gcNode = GlobalCache.listenBeacon(commandLineArgs.timeout);
//                    if (gcNode != null) {
//                        gcHostname = gcNode.getInetAddress().getHostName();
//                        System.err.println("got " + gcHostname);
//                    } else
//                        System.err.println("failed.");
//                }
//                globalCache = new GlobalCache(gcHostname, commandLineArgs.verbose, commandLineArgs.timeout);
//                rawIrSender = globalCache;
//                harcHardware = globalCache;
//                transmitter = globalCache.getTransmitter(commandLineArgs.transmitter);
//                captureDevice = globalCache;
//            } else if (commandLineArgs.irtoy) {
//                if (commandLineArgs.port != invalidPort)
//                    System.err.println("Port for IrToy not sensible, ignored.");
//
//                irtoy = new IrToy(commandLineArgs.device != null ? commandLineArgs.device : IrToy.defaultPortName);
//                rawIrSender = irtoy;
//                harcHardware = irtoy;
//                captureDevice = irtoy;
//                transmitter = null;
//            } else if (commandLineArgs.irtrans) {
//                if (commandLineArgs.port != invalidPort)
//                    System.err.println("Port for IrTrans not implemented, using standard port.");
//                irTrans = new IrTransIRDB(commandLineArgs.ip, commandLineArgs.verbose, commandLineArgs.timeout);
//                rawIrSender = irTrans;
//                remoteCommandIrSender = irTrans;
//                harcHardware = irTrans;
//                transmitter = irTrans.getTransmitter(commandLineArgs.transmitter);
//            } else if (commandLineArgs.lirc) {
//                int port = commandLineArgs.port == invalidPort ? LircClient.lircDefaultPort : commandLineArgs.port;
//                lircClient = new LircCcfClient(commandLineArgs.ip, port, commandLineArgs.verbose, commandLineArgs.timeout);
//                rawIrSender = lircClient;
//                remoteCommandIrSender = lircClient;
//                harcHardware = lircClient;
//                transmitter = lircClient.getTransmitter(commandLineArgs.transmitter);
//            } else if (commandLineArgs.irwidget) {
//                String device = commandLineArgs.device == null ? IrWidget.defaultPortName : commandLineArgs.device;
//                irWidget = new IrWidget(device, commandLineArgs.timeout, ICapture.defaultCaptureMaxSize, IrWidget.defaultEndingTimeout, false);
//                captureDevice = irWidget;
//                harcHardware = irWidget;
//            }
//
//            if (commandLineArgs.capture) {
//                if (captureDevice == null) {
//                    System.err.println("Hardware does not support capturing");
//                    doExit(IrpUtils.EXIT_USAGE_ERROR);
//                } else {
//                    captureDevice.open();
//                    captureDevice.setBeginTimeout(commandLineArgs.timeout);
//                    ModulatedIrSequence seq = captureDevice.capture();
//                    if (seq != null) {
//                        System.out.println(seq);
//                        //System.out.println(DecodeIR.DecodedSignal.toPrintString(DecodeIR.decode(seq)));
//                        doExit(IrpUtils.EXIT_SUCCESS);
//                    } else {
//                        System.err.println("Nothing received");
//                        doExit(IrpUtils.EXIT_FATAL_PROGRAM_FAILURE);
//                    }
//                }
//            }

//            if (commandLineArgs.getversion) {
//                if (harcHardware != null) {
//                    harcHardware.open();
//                    System.out.println(harcHardware.getVersion());
//                }
//                didSomethingUseful = true;
//            }

//            if (commandLineArgs.getremotes) {
//                if (remoteCommandIrSender == null) {
//                    System.err.println("getRemotes not supported by selected hardware");
//                } else {
//                    printTable("Result of getRemotes:", remoteCommandIrSender.getRemotes(), System.out);
//                    didSomethingUseful = true;
//                }
//            }
//
//            if (commandLineArgs.getcommands != null) {
//                if (remoteCommandIrSender == null) {
//                    System.err.println("getCommands not supported by selected hardware");
//                } else {
//                    printTable("Result of getCommands " + commandLineArgs.getcommands + ": ",
//                            remoteCommandIrSender.getCommands(commandLineArgs.getcommands), System.out);
//                    didSomethingUseful = true;
//                }
//            }
//
//            if (commandLineArgs.remotecommand != null && !commandLineArgs.remotecommand.isEmpty()) {
//                if (remoteCommandIrSender == null) {
//                    System.err.println("sendCommand not supported by selected hardware");
//                } else {
//                    boolean success = remoteCommandIrSender.sendIrCommand(commandLineArgs.remotecommand.get(0),
//                            commandLineArgs.remotecommand.get(1),
//                            commandLineArgs.count, transmitter);
//                    if (success) {
//                        if (commandLineArgs.verbose)
//                            System.err.println("sendIrCommand succeeded");
//                    } else
//                        System.err.println("sendIrCommand failed");
//                    didSomethingUseful = true;
//                }
//            }

//            if (commandLineArgs.parameters.isEmpty()) {
//                if (didSomethingUseful)
//                    doExit(IrpUtils.EXIT_SUCCESS);
//                else {
//                    System.err.println("Nothing to do.");
//                    usage(IrpUtils.EXIT_USAGE_ERROR);
//                }
//            }

//            if (rawIrSender == null) {
//                System.err.println("Hardware does not support raw IR signals");
//                doExit(IrpUtils.EXIT_USAGE_ERROR);
//            } else {
// FIXME
//
//                IrSignal irSignal = new IrSignal(commandLineArgs.irprotocolsIniFilename, 0,
//                        commandLineArgs.parameters.toArray(new String[commandLineArgs.parameters.size()]));
//                for (int i = 0; i < commandLineArgs.loop; i++) {
//                    boolean success = rawIrSender.sendIr(irSignal, commandLineArgs.count, transmitter);
//                    if (success) {
//                        if (commandLineArgs.verbose)
//                            System.err.println("sendIr succeeded");
//                    } else
//                        System.err.println("sendIr failed");
//                    try {
//                        Thread.sleep(200);
//                    } catch (InterruptedException ex) {
//                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                }
//            }

//            doExit(IrpUtils.EXIT_SUCCESS);
//
//        } catch (NoSuchPortException ex) {
//            System.err.println("RXTX: No such port");
//            System.exit(IrpUtils.EXIT_FATAL_PROGRAM_FAILURE);
//        } catch (HarcHardwareException | InvalidArgumentException | IOException | PortInUseException | UnsupportedCommOperationException ex) {
//            System.err.println(ex.getMessage());
//            System.exit(IrpUtils.EXIT_FATAL_PROGRAM_FAILURE);
//        }
//    }

    private Main() {
    }

    private final static class CommandLineArgs {
        private final static int defaultTimeout = 2000;

        @Parameter(names = {"-c", "--config"}, description = "Path to IrpProtocols.xml")
        private String irprotocolsIniFilename = null;

        @Parameter(names = {"-d", "--device"}, description = "Device name, e.g. COM7: or /dev/ttyS0")
        private String device = null;

        @Parameter(names = {"-V", "--version"}, description = "Call the getVersion() function")
        private boolean version = false;

        @Parameter(names = {"-h", "--help", "-?"}, description = "Display help message")
        private boolean helpRequested = false;

        @Parameter(names = {"-i", "--ip"}, description = "IP address or name")
        private String ip = null;

        @Parameter(names = {"-p", "--port"}, description = "Port number")
        private int port = INVALID_PORT;

        @Parameter(names = {"-t", "--transmitter"}, description = "Transmitter, semantic device dependent")
        private String transmitter = null;

        @Parameter(names = {"-T", "--timeout"}, description = "Timeout in milliseconds")
        private int timeout = defaultTimeout;

        @Parameter(names = {"-v", "--version"}, description = "Display version information")
        private boolean versionRequested;

        @Parameter(names = {"-V", "--verbose"}, description = "Execute commands verbosely")
        private boolean verbose;

    }

    @Parameters(commandNames = {"transmit, send"}, commandDescription = "")
    private static class CommandTransmit {

        @Parameter(names = {"-#", "--count"}, description = "Number of times to send sequence")
        private int count = 1;

//        @Parameter(names = {"-n", "--nameengine"}, description = "Define a name engine for resolving the bitfield.", converter = NameEngineParser.class)
//        private NameEngine nameEngine = new NameEngine();
    }

    @Parameters(commandNames = {"receive"}, commandDescription = "Receive a IR signal using demodulating receiver")
    private static class CommandReceive {

    }

    @Parameters(commandNames = {"capture"}, commandDescription = "Receive a IR signal using non-demodulating receiver")
    private static class CommandCapture {

    }

    @Parameters(commandNames = {"getremotes"}, commandDescription = "")
    private static class CommandGetRemotes {

    }

    @Parameters(commandNames = {"getcommands"}, commandDescription = "")
    private static class CommandGetCommands {

    }

    @Parameters(commandNames = {"version"}, commandDescription = "")
    private static class CommandVersion {

    }

    @Parameters(commandNames = {"help"}, commandDescription = "")
    private static class CommandHelp {

    }
}
