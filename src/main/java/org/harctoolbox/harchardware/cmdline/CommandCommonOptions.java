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
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.cmdline.CommandDecodeParameterOptions;
import org.harctoolbox.cmdline.UsageException;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.Utils;
import org.harctoolbox.harchardware.Version;
import org.harctoolbox.harchardware.comm.LocalSerialPort;
import org.harctoolbox.harchardware.comm.NonExistingPortException;
import org.harctoolbox.harchardware.ir.IIrReader;

@SuppressWarnings({"FieldMayBeFinal", "PublicField"})
public class CommandCommonOptions extends CommandDecodeParameterOptions {

    private static final Logger logger = Logger.getLogger(CommandCommonOptions.class.getName());

    private static final String IR_MODULE_NAME = "org.harctoolbox.harchardware.ir";
    private static final String QUERY = "?";

    // NOTE: There is no baud! Instead (device dependant) defaults are used. Ditto for wordlength, parity, stopbits.

    @Parameter(names = {"--arduino", "--girs"}, description = "Implies --class GirsClient. If --device is not given, the device default will be selected.")
    private boolean girs = false;

    @Parameter(names = {"--audio"}, description = "Implies --class IrAudioDevice.")
    private boolean audio = false;

    @Parameter(names = {"-B", "--begintimeout"}, description = "Set begin timeout (in ms).")
    private Integer beginTimeout = null;

    @Parameter(names = {"--capturemaxlength"}, description = "Set max capturelength.")
    private Integer capturemaxlength = null;

    @Parameter(names = {"--cf", "--commandfusion"}, description = "Implies --class CommandFusion. If --device is not given, the device default will be selected.")
    private boolean commandFusion = false;

    @Parameter(names = {"-C", "--class"}, description = "Class to be instantiated; must implement IHarcHardware and possibly more (dependent on requested function).")
    public String className = null;

    @Parameter(names = {"-d", "--device"}, description = "Device name, e.g. COM7: or /dev/ttyACM0,")
    private String device = null;

    @Parameter(names = {"--devslashlirc"}, description = "Implies --class DevSlashLirc. If --device is not given, the device default will be selected.")
    private boolean devSlashLirc = false;

    @Parameter(names = {"-E", "--endingtimeout"}, description = "Set ending timeout (in ms).")
    private Integer endingTimeout = null;

    @Parameter(names = {"--globalcache"}, description = "Implies --class GlobalCache. If --device is not given, the device default will be selected.")
    private boolean globalCache = false;

    @Parameter(names = {"-H", "--home", "--applicationhome", "--apphome"}, description = "Set application home (where files are located)")
    private String applicationHome = null;

    @Parameter(names = {      "--irremote"}, description = "Implies --class IRrecvDumpV2. If --device is not given, the device default will be selected.")
    private boolean irremote = false;

    @Parameter(names = {"--irtoy", "--IrToy"}, description = "Implies --class IrToy. If --device is not given, the device default will be selected.")
    private boolean irToy = false;

    @Parameter(names = {"--irtrans", "--IrTrans"}, description = "Implies --class IrTrans. If --ip is not given, the device default will be selected.")
    private boolean irTrans = false;

    @Parameter(names = {"--irwidget", "--IrWidget"}, description = "Implies --class IrWidget. If --device is not given, the device default will be selected.")
    private boolean irWidget = false;

    @Parameter(names = {"-I", "--ip"}, description = "IP address or name.")
    private String ip = null;

    @Parameter(names = {"-p", "--port"}, description = "Port number (TCP or UDP).")
    private Integer port = null;

    @Parameter(names = {"-T", "--timeout"}, description = "Timeout in milliseconds.") // TODO: replace by beginTimeout, endingTimeout, maxCaptureSize
    private Integer timeout = null;

    @Parameter(names = {"-v", "--verbose"}, description = "Execute commands verbosely.")
    private boolean verbose;

    public IHarcHardware setupHardware() throws UsageException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, IOException, HarcHardwareException {
        if (className == null || className.isEmpty())
            return null;

        IHarcHardware hardware = null;
        String qualifiedClassName = (className.contains(".") ? "" : IR_MODULE_NAME + ".") + className;
        Class<?> clazz = Class.forName(qualifiedClassName);

        try {
            if (device != null) {
                if (ip != null || port != null)
                    throw new UsageException("Cannot give both --ip and --device");
                try {
                    Constructor<?> constructor = clazz.getConstructor(String.class, boolean.class, Integer.class);
                    hardware = (IHarcHardware) constructor.newInstance(device, verbose, timeout);
                } catch (NoSuchMethodException ex) {
                    Method factory = clazz.getMethod("newInstance", String.class, boolean.class, Integer.class);
                    hardware = (IHarcHardware) factory.invoke(null, device, verbose, timeout);
                }
            } else if (ip != null) {
                Method expander = clazz.getMethod("expandIP", String.class);
                ip = (String) expander.invoke(null, ip);
                InetAddress inetAddress = InetAddress.getByName(ip);
                try {
                    Constructor<?> constructor = clazz.getConstructor(InetAddress.class, Integer.class, boolean.class, Integer.class);
                    hardware = (IHarcHardware) constructor.newInstance(inetAddress, port, verbose, timeout);
                } catch (NoSuchMethodException ex) {
                    Method factory = clazz.getMethod("newInstance", InetAddress.class, Integer.class, boolean.class, Integer.class);
                    hardware = (IHarcHardware) factory.invoke(null, inetAddress, port, verbose, timeout);
                }
            } else {
                Constructor<?> constructor = clazz.getConstructor(boolean.class, Integer.class);
                hardware = (IHarcHardware) constructor.newInstance(verbose, timeout);
            }
        } catch (InvocationTargetException ex) {
            if (ex.getTargetException() instanceof NonExistingPortException)
                throw new HarcHardwareException("Port " + ex.getTargetException().getMessage() + " does not exist or could not be opened.");
            else
                throw new HarcHardwareException(ex);
        }

        return hardware;
    }

    public boolean listSerialDevices(PrintStream out) throws IOException {
        if (device == null || !device.equals(QUERY))
            return false;

        if (!quiet)
            out.println("Serial ports:");
        List<String> ports = LocalSerialPort.getSerialPortNames(false);
        ports.stream().forEachOrdered((port) -> {
            out.println(port);
        });
        return true;
    }

    public String getAppHome(Class<?> mainClass) {
        return Utils.findApplicationHome(applicationHome, mainClass, Version.appName);
    }

    public boolean hasClass() {
        return className != null;
    }

    void assertNonNullClass() throws UsageException {
        if (className == null)
            throw new UsageException(("--class must be given."));
    }

    public void initialize() throws UsageException {
        if (girs)
            initializeSerial("--girs", "GirsClient");
        else if (irToy)
            initializeSerial("--irtoy", "IrToy");
        else if (commandFusion)
            initializeSerial("--commandfusion", "CommandFusion");
        else if (irWidget)
            initializeSerial("--irwidget", "IrWidget");
        else if (irremote)
            initializeSerial("--irremote", "IRrecvDumpV2");
        else if (devSlashLirc)
            initializeSerial("--devslashlirc", "DevLirc");
        else if (audio)
            initialize("--audio", "IrAudioDevice");
        else if (globalCache)
            initializeIP("--globalcache", "GlobalCache");
        else if (irTrans)
            initializeIP("--irtrans", "IrTrans");
    }

    public void setupTimeouts(IIrReader hardware) throws IOException, HarcHardwareException {
        if (beginTimeout != null)
            hardware.setBeginTimeout(beginTimeout);
        if (capturemaxlength != null)
            hardware.setCaptureMaxSize(capturemaxlength);
        if (endingTimeout != null)
            hardware.setEndingTimeout(endingTimeout);
    }

    private void initialize(String optionName, String clazz) throws UsageException {
        if (className != null)
            throw new UsageException("Cannot use " + optionName + " together with --class");
        className = clazz;

        logger.log(Level.FINE, "{0} set className = {1}", new Object[]{optionName, clazz});
    }

    private void initializeIP(String optionName, String clazz) throws UsageException {
        initialize(optionName, clazz);
        if (ip == null) {
            ip = Utils.DEFAULT;
            logger.log(Level.FINE, "{0} set ip = {1}", new Object[]{optionName, ip});
        }
    }

    private void initializeSerial(String optionName, String clazz) throws UsageException {
        initialize(optionName, clazz);
        if (device == null) {
            device = Utils.DEFAULT;
            logger.log(Level.FINE, "{0} set device = {1}", new Object[]{optionName, device});
        }
    }
}
