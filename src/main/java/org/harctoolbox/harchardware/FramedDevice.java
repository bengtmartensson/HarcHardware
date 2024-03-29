/*
Copyright (C) 2015 Bengt Martensson.

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

import java.io.IOException;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.harctoolbox.harchardware.comm.TcpSocketPort;

/**
 * This class is basically the encapsulation of a ICommandLineDevice and a Framer (a formatter for the lines).
 */

public class FramedDevice {

    @SuppressWarnings({"UseOfSystemOutOrSystemErr", "CallToPrintStackTrace"})
    public static void main(String[] args) {
        try (ICommandLineDevice denon = new TcpSocketPort("denon", 23, 2000, true, TcpSocketPort.ConnectionMode.keepAlive)) {
            FramedDevice commandLineDevice = new FramedDevice(denon, "{0}\r", true);
            String[] result = commandLineDevice.sendString("mvdown", 1, 0);
            System.out.println(result[0]);
        } catch (IOException | HarcHardwareException ex) {
            ex.printStackTrace();
        }
    }

    private ICommandLineDevice hardware;
    private IFramer framer;

    public FramedDevice(ICommandLineDevice hardware, IFramer framer) {
        this.hardware = hardware;
        this.framer = framer;
    }

    public FramedDevice(ICommandLineDevice hardware, String format, boolean touppercase) {
        this(hardware, new Framer(format, touppercase));
    }

    public FramedDevice(ICommandLineDevice hardware, String format) {
        this(hardware, new Framer(format, false));
    }

    public FramedDevice(ICommandLineDevice hardware) {
        this(hardware, new Framer());
    }

    @SuppressWarnings("SleepWhileInLoop")
    public String[] sendString(String[] cmds, int count, int returnLines, int delay, int waitForAnswer) throws IOException, HarcHardwareException {
        if (count < 1)
            throw new IllegalArgumentException("Count = " + count + " < 1; this is meaningless.");

        boolean sentStuff = false;
        try {
            for (String cmd : cmds) {
                sentStuff = true;
                String command = framer.frame(cmd);
                for (int c = 0; c < count; c++) {
                    if (delay > 0 && c > 0)
                        Thread.sleep(delay);
                    hardware.sendString(command);
                }
            }
            if (returnLines == 0)
                return new String[0];
            else if (returnLines > 0) {
                String[] result = new String[returnLines];
                for (int i = 0; i < returnLines; i++)
                    result[i] = hardware.readString(sentStuff); // wait only if we have sent something
                return result;
            } else {
                if (!hardware.ready() && waitForAnswer > 0)
                    Thread.sleep(waitForAnswer);
                List<String> answer = new ArrayList<>(16);
                while (hardware.ready()) {
                    String ans = hardware.readString(false);
                    answer.add(ans);
                }
                return answer.toArray(new String[0]);
            }
        } catch (InterruptedException ex) {
        }
        return null;
    }

    public String[] sendString(String cmd, int returnLines, int waitForAnswer) throws IOException, HarcHardwareException {
        return sendString(new String[]{ cmd }, 1, returnLines, 0, waitForAnswer);
    }

    public void sendString(String cmd) throws IOException, HarcHardwareException {
        sendString(cmd, 0, 0);
    }

    public boolean ready() throws IOException {
        return hardware.ready();
    }

    public String readString() throws IOException {
        return hardware.readString();
    }

    public String readString(boolean wait) throws IOException {
        return hardware.readString(wait);
    }

    public String getVersion() throws IOException {
        return hardware.getVersion();
    }

    public void setVerbose(boolean verbose) {
        hardware.setVerbose(verbose);
    }

    public void setDebug(int debug) {
        hardware.setDebug(debug);
    }

    public void setTimeout(int timeout) throws IOException, HarcHardwareException {
        hardware.setTimeout(timeout);
    }

    public boolean isValid() {
        return hardware.isValid();
    }

    public void open() throws HarcHardwareException, IOException {
        hardware.open();
    }

    public void close() throws IOException {
        hardware.close();
    }

    public static interface IFramer {

        public String frame(String arg);

        public String frame(Object[] args);
    }

    public static class Framer implements IFramer {
        private MessageFormat format;
        private final boolean toUpper;

        public Framer(String format, boolean toUpper) {
            this.format = new MessageFormat(format, Locale.US);
            this.toUpper = toUpper;
        }

        public Framer() {
            this("{0}", false);
        }

        @Override
        public String frame(String arg) {
            return frame(new Object[]{ toUpper ? arg.toUpperCase(Locale.US) : arg });
        }

        @Override
        public String frame(Object[] args) {
            return format.format(args, new StringBuffer(256), new FieldPosition(0)).toString();
        }
    }
}
