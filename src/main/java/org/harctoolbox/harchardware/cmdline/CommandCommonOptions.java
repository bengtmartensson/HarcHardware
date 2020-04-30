/*
Copyright (C) 2020 Bengt Martensson.

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
package org.harctoolbox.harchardware.cmdline;

import com.beust.jcommander.Parameter;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import org.harctoolbox.cmdline.CommandDecodeParameterOptions;
import org.harctoolbox.cmdline.UsageException;
import org.harctoolbox.harchardware.IHarcHardware;

@SuppressWarnings({"FieldMayBeFinal", "PublicField"})
public class CommandCommonOptions extends CommandDecodeParameterOptions {
    //static final String[] loggingOptions = new String[] { "logclasses",  "logfile", "logformat", "logLevel", "xmlLog" };

    //private final static int defaultTimeout = 2000;
    private final static String IR_MODULE_NAME = "org.harctoolbox.harchardware.ir";
    private final static String DEV_PREFIX = "/dev/";

    @Parameter(names = {"-C", "--class"}, description = "Class to be instantiated; must implement IHarcHardware and possibly more (dependent on requested function).")

    public String className = null;

//    @Parameter(names = {"--configfiles"}, listConverter = FileListParser.class,
//            description = "Pathname(s) of IRP database file(s) in XML format. Default is the one in the jar file.")
//    public List<File> configFiles = null;

    @Parameter(names = {"-d", "--device"}, description = "Device name, e.g. COM7: or /dev/ttyACM0,")
    private String device = null;

    // NOTE: There is no baud! Instead (possibly device dependant) defaults are used. Ditto for wordlength, parity, stopbits.

    @Parameter(names = {"-I", "--ip"}, description = "IP address or name.")
    private String ip = null;

    @Parameter(names = {"-p", "--port"}, description = "Port number.")
    private Integer port = null;

//    @Parameter(names = {"-t", "--transmitter"}, description = "Transmitter, semantic device dependent")
//    private String transmitter = null;

    @Parameter(names = {"-T", "--timeout"}, description = "Timeout in milliseconds.")
    private Integer timeout = null;//defaultTimeout;

    @Parameter(names = {"-V", "--verbose"}, description = "Execute commands verbosely.")
    private boolean verbose;

//    // JCommander does not know about our defaults being null, so handle this explicitly-
//    @Parameter(names = {"-a", "--absolutetolerance"},
//            description = "Absolute tolerance in microseconds, used when comparing durations. Default: " + IrCoreUtils.DEFAULT_ABSOLUTE_TOLERANCE + ".")
//    public Double absoluteTolerance = null;

//    @Parameter(names = {"-b", "--blacklist"}, description = "List of protocols to be removed from the data base")
//    public List<String> blackList = null;

////    @Parameter(names = {"-c", "--configfile"}, listConverter = FileListParser.class,
////            description = "Pathname of IRP database file in XML format. Default is the one in the jar file.")
////    public List<File> configFiles = null;
//
//    @Parameter(names = { "-C", "--commentStart"}, description = "Character(s) to be considered starting a line comment in input and namedInput files.")
//    public String commentStart = null;
//
//    // Some day there will possibly be a commentEnd?

//    @Parameter(names = {"-e", "--encoding"}, description = "Encoding used in generated output.")
//    public String encoding = "UTF-8";

////    @Parameter(names = {"-f", "--frequencytolerance"}, converter = FrequencyParser.class,
////            description = "Frequency tolerance in Hz. Negative disables frequency check. Default: " + IrCoreUtils.DEFAULT_FREQUENCY_TOLERANCE + ".")
////    public Double frequencyTolerance = null;
//
//    @Parameter(names = {"-g", "--minrepeatgap"}, description = "Minimum gap required to end a repetition.")
//    public double minRepeatGap = IrCoreUtils.DEFAULT_MIN_REPEAT_LAST_GAP;

//    @Parameter(names = {"-h", "--help", "-?"}, help = true, description = "Display help message. Deprecated; use the command \"help\" instead.")
//    public boolean helpRequested = false;

//    @Parameter(names = {"-i", "--irp"}, description = "Explicit IRP string to use as protocol definition.")
//    public String irp = null;

//    @Parameter(names = {"--logclasses"}, description = "List of (fully qualified) classes and their log levels, in the form class1:level1|class2:level2|...")
//    public String logclasses = "";
//
//    @Parameter(names = {"-L", "--logfile"}, description = "Log file. If empty, log to stderr.")
//    public String logfile = null;
//
//    @Parameter(names = {"-F", "--logformat"}, description = "Log format, as in class java.util.logging.SimpleFormatter.")
//    public String logformat = "[%2$s] %4$s: %5$s%n";
//
//    @Parameter(names = {"-l", "--loglevel"}, converter = LevelParser.class,
//            description = "Log level { OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL }")
//    public Level logLevel = Level.WARNING;

//    @Parameter(names = {"--min-leadout"},
//            description = "Threshold for leadout when decoding. Default: " + IrCoreUtils.DEFAULT_MINIMUM_LEADOUT + ".")
//    public Double minLeadout = null;

//    @Parameter(names = {"-o", "--output"}, description = "Name of output file. Default: stdout.")
//    public String output = null;

//    @Parameter(names = {"-O", "--override"}, description = "Let given command line parameters override the protocol parameters in IrpProtoocols.xml")
//    public boolean override = false;

//    @Parameter(names = {"-q", "--quiet"}, description = "Quitest possible operation, typically to be used from scripts.")
//    public boolean quiet = false;

////    @Parameter(names = {"-r", "--relativetolerance"}, validateWith = LessThanOne.class,
////            description = "Relative tolerance as a number < 1. Default: " + IrCoreUtils.DEFAULT_RELATIVE_TOLERANCE + ".")
////    public Double relativeTolerance = null;
//
//    @Parameter(names = {"--regexp"}, description = "Interpret protocol/decoder argument as regular expressions.")
//    public boolean regexp = false;
//
//    @Parameter(names = {"-s", "--sort"}, description = "Sort the protocols alphabetically on output.")
//    public boolean sort = false;
//
//    @Parameter(names = {"--seed"},
//            description = "Set seed for the pseudo random number generation. If not specified, will be random, different between program invocations.")
//    public Long seed = null;
//
////    @Parameter(names = {"-t", "--tsv", "--csv"}, description = "Use tabs in output to optimize for the import in spreadsheet programs as cvs.")
////    public boolean tsvOptimize = false;
//
//    @Parameter(names = {"-u", "--url-decode"}, description = "URL-decode protocol names, (understanding %20 for example).")
//    public boolean urlDecode = false;

//    @Parameter(names = {"-v", "--version"}, description = "Report version. Deprecated; use the command \"version\" instead.")
//    public boolean versionRequested = false;
//
//    @Parameter(names = {"-x", "--xmllog"}, description = "Write the log in XML format.")
//    public boolean xmlLog = false;

    public IHarcHardware hardware() throws UsageException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, NoSuchPortException, PortInUseException, UnsupportedCommOperationException {
        if (className == null || className.isEmpty())
            return null;

        IHarcHardware hardware = null;
        String qualifiedClassName = (className.contains(".") ? "" : IR_MODULE_NAME + ".") + className;
        Class<?> clazz = Class.forName(qualifiedClassName);

        if (device != null) {
            if (ip != null || port != null)
                throw new UsageException("Cannot give both --ip and --device");
            String qualifiedDevName = ((File.separatorChar == '\\' || new File(device).isAbsolute()) ? "" : DEV_PREFIX) + device;
            try {
                Constructor<?> constructor = clazz.getConstructor(String.class, boolean.class, Integer.class);
                hardware = (IHarcHardware) constructor.newInstance(qualifiedDevName, verbose, timeout);
            } catch (NoSuchMethodException ex) {
//                Constructor<?> constructor = clazz.getConstructor(String.class);
//                hardware = (IHarcHardware) constructor.newInstance(qualifiedDevName);
//            }
                Method factory = clazz.getMethod("newInstance", String.class, boolean.class, Integer.class);
                hardware = (IHarcHardware) factory.invoke(null, qualifiedDevName, verbose, timeout);
            //LocalSerialPortBuffered serial = new LocalSerialPortBuffered(device, verbose, 115200, timeout);
            }
        } else if (ip != null) {
            InetAddress inetAddress = InetAddress.getByName(ip);
            try {
                Constructor<?> constructor = clazz.getConstructor(InetAddress.class, Integer.class, boolean.class, Integer.class);
                hardware = (IHarcHardware) constructor.newInstance(inetAddress, port, verbose, timeout);
            } catch (NoSuchMethodException ex) {
//                Constructor<?> constructor = clazz.getConstructor(String.class);
//                hardware = (IHarcHardware) constructor.newInstance(qualifiedDevName);
//            }
                Method factory = clazz.getMethod("newInstance", InetAddress.class, Integer.class, boolean.class, Integer.class);
                hardware = (IHarcHardware) factory.invoke(null, inetAddress, port, verbose, timeout);
                //LocalSerialPortBuffered serial = new LocalSerialPortBuffered(device, verbose, 115200, timeout);
            }
        } else
            throw new UsageException("Either --device or --ip must be given.");

        hardware.setVerbose(verbose);
        if (timeout != null)
            hardware.setTimeout(timeout);

        return hardware;
    }

    public boolean hasClass() {
        return className != null;
    }

    void assertClass() throws UsageException {
        if (className == null)
            throw new UsageException(("--class must be given."));
    }

//    public static class NullHarcHardware implements IHarcHardware {
//
//        @Override
//        public String getVersion() throws IOException {
//            return null;
//        }
//
//        @Override
//        public void setVerbose(boolean verbose) {
//        }
//
//        @Override
//        public void setDebug(int debug) {
//        }
//
//        @Override
//        public void setTimeout(int timeout) throws IOException {
//        }
//
//        @Override
//        public boolean isValid() {
//            return false;
//        }
//
//        @Override
//        public void open() throws HarcHardwareException, IOException {
//        }
//
//        @Override
//        public void close() throws IOException {
//        }
//
//    }
}
