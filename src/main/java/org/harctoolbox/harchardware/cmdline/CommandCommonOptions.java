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

    private final static String IR_MODULE_NAME = "org.harctoolbox.harchardware.ir";
    private final static String DEV_PREFIX = "/dev/";

    @Parameter(names = {"-C", "--class"}, description = "Class to be instantiated; must implement IHarcHardware and possibly more (dependent on requested function).")

    public String className = null;

    @Parameter(names = {"-d", "--device"}, description = "Device name, e.g. COM7: or /dev/ttyACM0,")
    private String device = null;

    // NOTE: There is no baud! Instead (possibly device dependant) defaults are used. Ditto for wordlength, parity, stopbits.

    @Parameter(names = {"-I", "--ip"}, description = "IP address or name.")
    private String ip = null;

    @Parameter(names = {"-p", "--port"}, description = "Port number.")
    private Integer port = null;

    @Parameter(names = {"-T", "--timeout"}, description = "Timeout in milliseconds.")
    private Integer timeout = null;//defaultTimeout;

    @Parameter(names = {"-V", "--verbose"}, description = "Execute commands verbosely.")
    private boolean verbose;

    public IHarcHardware hardware() throws UsageException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
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
                Method factory = clazz.getMethod("newInstance", String.class, boolean.class, Integer.class);
                hardware = (IHarcHardware) factory.invoke(null, qualifiedDevName, verbose, timeout);
            }
        } else if (ip != null) {
            InetAddress inetAddress = InetAddress.getByName(ip);
            try {
                Constructor<?> constructor = clazz.getConstructor(InetAddress.class, Integer.class, boolean.class, Integer.class);
                hardware = (IHarcHardware) constructor.newInstance(inetAddress, port, verbose, timeout);
            } catch (NoSuchMethodException ex) {
                Method factory = clazz.getMethod("newInstance", InetAddress.class, Integer.class, boolean.class, Integer.class);
                hardware = (IHarcHardware) factory.invoke(null, inetAddress, port, verbose, timeout);
                //LocalSerialPortBuffered serial = new LocalSerialPortBuffered(device, verbose, 115200, timeout);
            }
        } else
            throw new UsageException("Either --device or --ip must be given.");

        return hardware;
    }

    public boolean hasClass() {
        return className != null;
    }

    void assertClass() throws UsageException {
        if (className == null)
            throw new UsageException(("--class must be given."));
    }
}
