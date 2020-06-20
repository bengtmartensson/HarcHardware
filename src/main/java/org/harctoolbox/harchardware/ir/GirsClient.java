/*
Copyright (C) 2015, 2016, 2018 Bengt Martensson.

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

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.ICommandLineDevice;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.comm.LocalSerialPort;
import org.harctoolbox.harchardware.comm.LocalSerialPortBuffered;
import org.harctoolbox.harchardware.comm.TcpSocketPort;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrSequence;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.OddSequenceLengthException;
import org.harctoolbox.ircore.Pronto;
import org.harctoolbox.ircore.ThisCannotHappenException;

/**
 *
 * @param <T>
 */
public class GirsClient<T extends ICommandLineDevice & IHarcHardware>  implements IHarcHardware, IReceive, IRawIrSender, IRawIrSenderRepeat, IRemoteCommandIrSender, IIrSenderStop, ITransmitter, ICapture, ICommandLineDevice, Closeable {

    private final static Logger logger = Logger.getLogger(GirsClient.class.getName());

    private final static String defaultLineEnding = "\r";
    private final static String sendCommand = "send";
    private final static String captureCommand = "analyze";
    private final static String receiveCommand = "receive";
    private final static String versionCommand = "version";
    private final static String modulesCommand = "modules";
    private final static String resetCommand = "reset";
    private final static String ledCommand = "led";
    private final static String lcdCommand = "lcd";
    private final static String captureModuleName = "capture";
    private final static String okString = "OK";
    private final static String errorString = "ERROR";
    private final static String timeoutString = ".";
    private final static String separator = " ";

    private final static int DEFAULT_BAUD = 115200;
    private final static int DEFAULT_PORT = 33333;

    /**
     * Just for testing.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            boolean verbose = true;
            GirsClient<?> gc;
            if (args[0].startsWith("/dev/") || args[0].toUpperCase(Locale.US).startsWith("COM")) {

                //int baud = args.length < 2 ? DEFAULT_BAUD : Integer.parseInt(args[1]);
                gc = newInstance(args[0], verbose, null);
            } else {
                int port = args.length < 2 ? DEFAULT_PORT : Integer.parseUnsignedInt(args[1]);
                gc = newInstance(InetAddress.getByName(args[0]), port, verbose, null);
            }
            gc.testGirs();
        } catch (NoSuchPortException | PortInUseException | UnsupportedCommOperationException | IOException | HarcHardwareException ex) {
            Logger.getLogger(GirsClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static GirsClient<TcpSocketPort> newInstance(InetAddress inetAddress, Integer portnumber, boolean verbose, Integer timeout) throws UnknownHostException, HarcHardwareException, IOException {
        TcpSocketPort tcp = new TcpSocketPort(inetAddress, portnumber != null ? portnumber : DEFAULT_PORT, timeout != null ? timeout : TcpSocketPort.defaultTimeout, verbose, TcpSocketPort.ConnectionMode.keepAlive);
        GirsClient<TcpSocketPort> gc = new GirsClient<>(tcp);
        return gc;
    }

    public static GirsClient<LocalSerialPortBuffered> newInstance(String portName, boolean verbose, Integer timeout) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException, HarcHardwareException {
        LocalSerialPortBuffered serial = new LocalSerialPortBuffered(portName, verbose, DEFAULT_BAUD, timeout);
        GirsClient<LocalSerialPortBuffered> gc = new GirsClient<>(serial);
        return gc;
    }

    private static String capitalize(String module) {
        return module.substring(0, 1).toUpperCase(Locale.US) + module.substring(1);
    }

    private String version;
    private List<String> modules;
    private final T hardware;
    private boolean verbose;
    private int debug;
    private boolean useReceiveForCapture;
    private String lineEnding;
    private int beginTimeout;
    private int maxCaptureLength;
    private int endingTimeout;
    private int fallbackFrequency = (int) ModulatedIrSequence.DEFAULT_FREQUENCY;
    private boolean stopRequested = false;
    private boolean pendingCapture = false;

    public GirsClient(T hardware) throws HarcHardwareException, IOException {
        this.lineEnding = defaultLineEnding;
        this.verbose = false;
        this.hardware = hardware;
        this.useReceiveForCapture = false;
    }

    private void testGirs() throws IOException, HarcHardwareException {
        open();
        getModules().forEach((module) -> this.testModule(module));
    }

    private void testModule(String module) {
        if (!hasModule(module)) {
            // should never happen really.
            logger.log(Level.SEVERE, "module {0} not implemented", module);
            return;
        }

        try {
            Thread.sleep(1000);
            String name = "test" + capitalize(module);
            Method method = GirsClient.class.getMethod(name);
            logger.log(Level.INFO, "Testing {0}", module);
            method.invoke(this);
        } catch (NoSuchMethodException ex) {
            logger.log(Level.WARNING, "No test for module {0} found.", module);
        } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | InterruptedException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    public void testLcd() throws IOException, HarcHardwareException {
        setLcd("LCD lcd");
    }

    public void testBase() throws IOException {
        logger.log(Level.INFO, "Version: {0}", getVersion());
    }

    public void testTransmit() throws IOException, HarcHardwareException {
        try {
            IrSignal rc5_0_0 = Pronto.parse("0000 0073 0000 000D 0020 0020 0040 0020 0020 0020 0020 0020 0020 0020 0020 0020 0020 0020 0020 0020 0020 0020 0020 0020 0020 0020 0020 0020 0020 0CC8");
            sendIr(rc5_0_0, 1);
        } catch (Pronto.NonProntoFormatException | InvalidArgumentException ex) {
            throw new ThisCannotHappenException();
        }
    }

    @SuppressWarnings("SleepWhileInLoop")
    public void testLed() throws IOException, InterruptedException {
        for (int i = 1; i <= 8; i++) {
            try {
                logger.log(Level.FINE, "Testing LED #{0}", i);
                setLed(i, true);
                Thread.sleep(1000);
                setLed(i, false);
            } catch (HarcHardwareException ex) {
                logger.log(Level.WARNING, "Failed for LED #{0}, not present?", i);
            }
        }
    }

    public void testReceive() throws HarcHardwareException, IOException {
        System.out.println("Now send an IR signal to the demodulating receiver");
        if (hasModule("lcd"))
            setLcd("Send signal to demod.");
        IrSequence irSequence = receive();
        if (irSequence == null) {
            logger.log(Level.WARNING, "No input");
        } else {
            ModulatedIrSequence seq = new ModulatedIrSequence(irSequence, ModulatedIrSequence.DEFAULT_FREQUENCY);
            logger.log(Level.INFO, seq.toString(true));
        }
    }

    public void testCapture() throws HarcHardwareException, IOException, OddSequenceLengthException {
        System.out.println("Now send an IR signal to the non-demodulating receiver");
        if (hasModule("lcd"))
            setLcd("Send signal to non-demod.");
        ModulatedIrSequence irSequence = capture();
        if (irSequence == null) {
            logger.warning("No input detected");
        } else {
            logger.log(Level.INFO, irSequence.toString(true));
        }
    }

    public void testParameters() throws IOException, HarcHardwareException {
        String parameterName = "capturesize";
        long newValue = 123L;
        getParameter(parameterName);
        //System.out.println(old);
        setParameter(parameterName, newValue);
        long newVal = getParameter(parameterName);
        if (newVal != newValue)
            logger.log(Level.SEVERE, "Parameters failed");
    }

    public void setUseReceiveForCapture(boolean val) throws HarcHardwareException {
        if (!val && !hasCaptureModule())
            throw new HarcHardwareException("Capture not supported");
        this.useReceiveForCapture = val;
    }

    public void setUseReceiveForCapture() {
        try {
            setUseReceiveForCapture(! hasCaptureModule());
        } catch (HarcHardwareException ex) {
        }
    }

    @Override
    public void close() throws IOException {
        hardware.close();
    }

    @Override
    public String getVersion() throws IOException {
        return version;
    }

    @Override
    public void setVerbose(boolean verbose) {
        hardware.setVerbose(verbose);
        this.verbose = verbose;
    }

    @Override
    public void setDebug(int debug) {
        this.debug = debug;
    }

    @Deprecated
    @Override
    public void setTimeout(int timeout) throws IOException {
        setBeginTimeout(timeout);
    }

    /**
     * @return the beginTimeout
     */
    public int getBeginTimeout() {
        return beginTimeout;
    }

    /**
     * @param beginTimeout the beginTimeout to set
     */
    @Override
    public void setBeginTimeout(int beginTimeout) {
        this.beginTimeout = beginTimeout;
    }

    /**
     * @return the maxCaptureLength
     */
    public int getMaxCaptureLength() {
        return maxCaptureLength;
    }

    /**
     * @param maxCaptureLength the maxCaptureLength to set
     */
    @Override
    public void setCaptureMaxSize(int maxCaptureLength) {
        this.maxCaptureLength = maxCaptureLength;
    }

    /**
     * @return the endingTimeout
     */
    public int getEndingTimeout() {
        return endingTimeout;
    }

    /**
     * @param endingTimeout the endingTimeout to set
     */
    @Override
    public void setEndingTimeout(int endingTimeout) {
        this.endingTimeout = endingTimeout;
    }

    /**
     * @param lineEnding the lineEnding to set
     */
    public void setLineEnding(String lineEnding) {
        this.lineEnding = lineEnding;
    }

    @Override
    public boolean isValid() {
        return hardware.isValid() && version != null && modules != null && modules.contains("base");
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<String> getModules() {
        return modules;
    }

    public boolean hasModule(String module) {
        return modules.contains(module.toLowerCase(Locale.US));
    }

    public boolean hasCaptureModule() {
        return modules.contains(captureModuleName);
    }

    /**
     * @param fallbackFrequency the fallbackFrequency to set
     */
    public void setFallbackFrequency(int fallbackFrequency) {
        this.fallbackFrequency = fallbackFrequency;
    }

    @Override
    public synchronized boolean sendIr(IrSignal irSignal, int count, Transmitter transmitter) throws IOException, HarcHardwareException {
       return sendIr(irSignal, count);
    }

    public synchronized boolean sendIr(IrSignal irSignal, int count) throws IOException, HarcHardwareException {
        String payload = formatSendString(irSignal, count);
        hardware.sendString(payload + lineEnding);
        if (verbose)
            System.err.println(payload);
        String response = hardware.readString(true);
        return response != null && response.trim().equals(okString);
    }

    @Override
    public void open() throws IOException, HarcHardwareException {
        hardware.open();
        waitFor(okString, lineEnding, /*delay*/ 100, /* tries = */ 3);
        hardware.sendString(versionCommand + lineEnding);
        version = hardware.readString(true).trim();
        if (verbose)
            System.err.println(versionCommand + " returned '" + version + "'.");
        hardware.sendString(modulesCommand + lineEnding);
        String line = hardware.readString(true);
        if (verbose)
            System.err.println(versionCommand + " returned '" + version + "'.");
        if (line != null)
            modules = Arrays.asList(line.toLowerCase(Locale.US).split("\\s+"));
        setUseReceiveForCapture();
    }

    @SuppressWarnings("SleepWhileInLoop")
    public void waitFor(String goal, String areUThere, int delay, int tries) throws IOException, HarcHardwareException {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            // nothing
        }
        flushIn();
        for (int i = 0; i < tries; i++) {
            sendString(areUThere);
            String answer = readString(true);
            if (answer == null)
                continue;
            answer = answer.trim();
            if (answer.startsWith(goal)) {// success!
                flushIn();
                return;
            }
            if (delay > 0)
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ex) {
                    break;
                }
        }
        // Failure if we get here.
        throw new HarcHardwareException("Hardware not responding");
    }

    private void flushIn() /*throws IOException*/ {
        try {
            while (true) {
                String junk = readString(false);
                if (junk == null)
                    break;
                if (verbose)
                    System.err.println("LocalSerialPortBuffered.flushIn: junked '" + junk + "'.");
            }
        } catch (IOException ex) {
            // This bizarre code actually both seems to work, and be needed (at least using my Mega2560),
            // the culprit is probably rxtx.
             if (verbose)
                    System.err.println("IOException in LocalSerialPortBuffered.flushIn ignored: " + ex.getMessage());
        }
    }

    private StringBuilder join(IrSequence irSequence, String separator) {
        if (irSequence == null || irSequence.isEmpty())
            return new StringBuilder(0);

        StringBuilder str = new StringBuilder(128);
        for (int i = 0; i < irSequence.getLength(); i++)
            str.append(separator).append(Integer.toString((int) irSequence.get(i)));
        return str;
    }

    private String formatSendString(IrSignal irSignal, int count) {
        if (irSignal == null)
            throw new IllegalArgumentException("irSignal cannot be null");
        StringBuilder str = new StringBuilder(sendCommand);
        str.append(separator).append(Integer.toString(count));
        str.append(separator).append(Integer.toString((int) ModulatedIrSequence.getFrequencyWithDefault(irSignal.getFrequency())));
        str.append(separator).append(Integer.toString(irSignal.getIntroLength()));
        str.append(separator).append(Integer.toString(irSignal.getRepeatLength()));
        str.append(separator).append(Integer.toString(irSignal.getEndingLength()));

        str.append(join(irSignal.getIntroSequence(), separator));
        str.append(join(irSignal.getRepeatSequence(), separator));
        str.append(join(irSignal.getEndingSequence(), separator));

        return str.toString();
    }

    @Override
    public ModulatedIrSequence capture() throws IOException, HarcHardwareException, OddSequenceLengthException {
        return useReceiveForCapture ? mockModulatedIrSequence() : realCapture();
    }

    private ModulatedIrSequence mockModulatedIrSequence() throws HarcHardwareException, IOException {
        IrSequence irSequence = receive();
        return irSequence == null ? null : new ModulatedIrSequence(irSequence, (double) fallbackFrequency, null);
    }

    private ModulatedIrSequence realCapture() throws HarcHardwareException, IOException, OddSequenceLengthException {
        if (stopRequested) // ???
            return null;
        if (!isValid())
            throw new HarcHardwareException("Port not initialized");
        if (!pendingCapture) {
            hardware.sendString(captureCommand + lineEnding);
            pendingCapture = true;
        }
        ModulatedIrSequence seq = null;
        try {
            //open();
            String str = hardware.readString(true);
            pendingCapture = false;
            if (str == null || str.length() == 0 || str.startsWith("null") || str.startsWith(timeoutString))
                return null;

            str = str.trim();
            if (str.toUpperCase(Locale.US).startsWith(errorString))
                throw new HarcHardwareException("Girs server does not support capture.");

            double frequency = fallbackFrequency;
            if (str.startsWith("f=")) {
                int indx = str.indexOf(' ');
                if (indx < 0)
                    return null;
                frequency = Integer.parseInt(str.substring(2, indx));
                str = str.substring(indx + 1);
            }
            seq = new ModulatedIrSequence(new IrSequence(str), frequency, -1.0);
        } catch (SocketTimeoutException ex) {
            return null;
        } catch (IOException ex) {
            //close();
            if (ex.getMessage().equals("Underlying input stream returned zero bytes")) //RXTX timeout
                return null;
            throw ex;
        } catch (OddSequenceLengthException ex) {
            //close();
            throw new HarcHardwareException(ex);
        }
        //close();
        return seq;
    }

    @Override
    public boolean stopCapture() {
        stopRequested = true;
        return true;
    }

    @Override
    public IrSequence receive() throws HarcHardwareException, IOException {
        if (stopRequested) // ???
            return null;
        if (!isValid())
            throw new HarcHardwareException("Port not initialized");
        if (!pendingCapture) {
            hardware.sendString(receiveCommand + lineEnding);
            pendingCapture = true;
        }

        IrSequence seq = null;
        try {
            //open();
            String str = hardware.readString(true);
            pendingCapture = false;
            if (str == null || str.length() == 0 || str.startsWith("null") || str.startsWith(timeoutString))
                return null;

            str = str.trim();

            //double frequency = fallbackFrequency;
            seq = new IrSequence(str);
        } catch (SocketTimeoutException ex) {
            return null;
        } catch (IOException ex) {
            //close();
            if (ex.getMessage().equals("Underlying input stream returned zero bytes")) //RXTX timeout
                return null;
            throw ex;
        } catch (OddSequenceLengthException ex) {
            //close();
            throw new HarcHardwareException(ex);
        }
        //close();
        return seq;
    }

    @Override
    public boolean stopReceive() {
        throw new UnsupportedOperationException("Not supported yet.");// TODO
    }

    public void reset() throws IOException, HarcHardwareException {
        hardware.sendString(resetCommand);
        // ???
        if (hardware instanceof LocalSerialPortBuffered)
            ((LocalSerialPort) hardware).dropDTR(100);
    }

    @Override
    public void sendString(String cmd) throws IOException, HarcHardwareException {
        hardware.sendString(cmd);
    }

    @Override
    public String readString() throws IOException {
        return hardware.readString();
    }

    @Override
    public String readString(boolean wait) throws IOException {
        return hardware.readString(wait);
    }

    @Override
    public boolean ready() throws IOException {
        return hardware.ready();
    }

    @Override
    public void flushInput() throws IOException {
        hardware.flushInput();
    }

    @Override
    public Transmitter getTransmitter() {
        return null; // TODO
    }

    @Override
    public boolean stopIr(Transmitter transmitter) throws NoSuchTransmitterException, IOException {
        throw new UnsupportedOperationException("Not supported yet."); // TODO
    }

    @Override
    public Transmitter getTransmitter(String connector) throws NoSuchTransmitterException {
        throw new UnsupportedOperationException("Not supported yet."); // TODO
    }

    @Override
    public String[] getTransmitterNames() {
        throw new UnsupportedOperationException("Not supported yet."); // TODO
    }

    @Override
    public boolean sendIrRepeat(IrSignal irSignal, Transmitter transmitter) throws NoSuchTransmitterException, IOException {
        throw new UnsupportedOperationException("Not supported yet."); // TODO
    }

    @Override
    public String[] getRemotes() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); // TODO (later)
    }

    @Override
    public String[] getCommands(String remote) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); // TODO (later)
    }

    @Override
    public boolean sendIrCommand(String remote, String command, int count, Transmitter transmitter) throws IOException, NoSuchTransmitterException {
        throw new UnsupportedOperationException("Not supported yet."); //TODO (later)
    }

    @Override
    public boolean sendIrCommandRepeat(String remote, String command, Transmitter transmitter) throws IOException, NoSuchTransmitterException {
        throw new UnsupportedOperationException("Not supported yet."); // TODO (later)
    }

    public void setLed(int led, boolean state) throws IOException, HarcHardwareException {
        sendStringWaitOk(ledCommand + separator + led + separator + (state ? "on" : "off"));
    }

    public void setLed(int led, int flashTime) {
        // TODO
    }

    public void setLcd(String message) throws IOException, HarcHardwareException {
        sendStringWaitOk(lcdCommand + " " + message);
    }

    public void setLcd(String message, int x, int y) {
        // TODO
    }

    public void setLcdBacklight(boolean state) {
        // TODO
    }

    public void setLcdBacklight(int flashTime) {
        // TODO
    }

    private void sendStringWaitOk(String line) throws IOException, HarcHardwareException {
        hardware.sendString(line + lineEnding);
        String answer = readString(true);
        if (answer == null)
            throw new HarcHardwareException("No \"" + okString + "\" received.");
        answer = answer.trim();
        if (!answer.startsWith(okString))
            throw new HarcHardwareException("No \"" + okString + "\" received, instead \"" + answer + "\".");
    }

    private long getParameter(String parameterName) throws IOException, HarcHardwareException {
        hardware.sendString("parameter " + parameterName + lineEnding);
        String answer = readString(true);
        if (answer == null)
            throw new HarcHardwareException("No answer received.");
        long value = Long.parseLong(answer.split("=")[1]);
        return value;
    }

    private void setParameter(String parameterName, long newValue) throws IOException, HarcHardwareException {
        hardware.sendString("parameter " + parameterName + " " + Long.toString(newValue) + lineEnding);
        String answer = readString(true);
        if (answer == null)
            throw new HarcHardwareException("No answer received.");
    }
}
