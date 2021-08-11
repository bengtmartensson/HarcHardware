/*
Copyright (C) 2015, 2016, 2018, 2020 Bengt Martensson.

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

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.ICommandLineDevice;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.comm.LocalSerialPort;
import org.harctoolbox.harchardware.comm.LocalSerialPortBuffered;
import org.harctoolbox.harchardware.comm.TcpSocketPort;
import org.harctoolbox.ircore.IrSequence;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.OddSequenceLengthException;

/**
 *
 * @param <T>
 */
public class GirsClient<T extends ICommandLineDevice & IHarcHardware>  implements IHarcHardware, IReceive, IRawIrSender, IRawIrSenderRepeat, IRemoteCommandIrSender, IIrSenderStop, ITransmitter, ICapture, ICommandLineDevice, Closeable {

    private static final Logger logger = Logger.getLogger(GirsClient.class.getName());

    private static final String DEFAULT_LINEENDING = "\r";
    private static final String SEND_COMMAND = "send";
    private static final String CAPTURE_COMMAND = "analyze";
    private static final String RECEIVE_COMMAND = "receive";
    private static final String VERSION_COMMAND = "version";
    private static final String MODULES_COMMAND = "modules";
    private static final String RESET_COMMAND = "reset";
    private static final String LED_COMMAND = "led";
    private static final String LCD_COMMAND = "lcd";
    private static final String SET_PARAMETER_COMMAND = "parameter";
    private static final String CAPTURE_MODULENAME = "capture";
    private static final String PARAMETERS_MODULENAME = "parameters";
    private static final String TRANSMIT_MODULENAME = "transmit";
    private static final String BEGIN_TIMEOUT_PARAMETER_NAME = "beginTimeout";
    private static final String ENDING_CAPTURE_TIMEOUT_PARAMETER_NAME = "captureendingTimeout";
    private static final String ENDING_RECEIVE_TIMEOUT_PARAMETER_NAME = "receiveendingTimeout";
    private static final String MAX_CAPTURE_LENGTH_PARAMETER_NAME = "capturesize";

    private static final String OK_STRING = "OK";
    private static final String ERROR_STRING = "ERROR";
    private static final String TIMEOUT_STRING = ".";
    private static final String SEPARATOR = " ";

    public static final int DEFAULT_BAUD = 115200;
    public static final int DEFAULT_PORT = 33333;

    private static final String DEFAULT_PORTNAME = "arduino";

    public static GirsClient<TcpSocketPort> newInstance(InetAddress inetAddress, Integer portnumber, boolean verbose, Integer timeout) throws UnknownHostException, HarcHardwareException, IOException {
        TcpSocketPort tcp = new TcpSocketPort(inetAddress, portnumber != null ? portnumber : DEFAULT_PORT, timeout != null ? timeout : TcpSocketPort.defaultTimeout, verbose, TcpSocketPort.ConnectionMode.keepAlive);
        GirsClient<TcpSocketPort> gc = new GirsClient<>(tcp);
        return gc;
    }

    public static GirsClient<LocalSerialPortBuffered> newInstance(String portName, boolean verbose, Integer timeout) throws IOException, HarcHardwareException {
        String realPort = LocalSerialPort.canonicalizePortName(portName, DEFAULT_PORTNAME);
        LocalSerialPortBuffered serial = new LocalSerialPortBuffered(realPort, verbose, timeout, DEFAULT_BAUD);
        GirsClient<LocalSerialPortBuffered> gc = new GirsClient<>(serial);
        return gc;
    }

    public static String expandIP(String IP) {
        return IP;
    }

    private String version;
    private List<String> modules;
    private final T hardware;
    private boolean verbose;
    private int debug;
    private boolean useReceiveForCapture;
    private String lineEnding;
    private Integer beginTimeout = null;
    private Integer maxCaptureLength = null;
    private Integer endingTimeout = null;
    private int fallbackFrequency = (int) ModulatedIrSequence.DEFAULT_FREQUENCY;
    private boolean stopRequested = false;
    private boolean pendingCapture = false;
    private boolean hasParameters = false;

    public GirsClient(T hardware) throws HarcHardwareException, IOException {
        this.lineEnding = DEFAULT_LINEENDING;
        this.verbose = false;
        this.hardware = hardware;
        this.useReceiveForCapture = false;
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
    public void setTimeout(int timeout) throws IOException, HarcHardwareException {
        setBeginTimeout(timeout);
    }

    /**
     * @return the beginTimeout
     */
    public Integer getBeginTimeout() {
        return beginTimeout;
    }

    /**
     * @param beginTimeout the beginTimeout to set
     * @throws java.io.IOException
     * @throws org.harctoolbox.harchardware.HarcHardwareException
     */
    @Override
    public void setBeginTimeout(int beginTimeout) throws IOException, HarcHardwareException {
        this.beginTimeout = beginTimeout;
        setParameter(BEGIN_TIMEOUT_PARAMETER_NAME, beginTimeout);
    }

    /**
     * @return the maxCaptureLength
     */
    public int getMaxCaptureLength() {
        return maxCaptureLength;
    }

    /**
     * @param maxCaptureLength the maxCaptureLength to set
     * @throws java.io.IOException
     * @throws org.harctoolbox.harchardware.HarcHardwareException
     */
    @Override
    public void setCaptureMaxSize(int maxCaptureLength) throws IOException, HarcHardwareException {
        this.maxCaptureLength = maxCaptureLength;
        setParameter(MAX_CAPTURE_LENGTH_PARAMETER_NAME, maxCaptureLength);
    }

    /**
     * @return the endingTimeout
     */
    public int getEndingTimeout() {
        return endingTimeout;
    }

    /**
     * @param endingTimeout the endingTimeout to set
     * @throws java.io.IOException
     * @throws org.harctoolbox.harchardware.HarcHardwareException
     */
    @Override
    public void setEndingTimeout(int endingTimeout) throws IOException, HarcHardwareException {
        this.endingTimeout = endingTimeout;
        setParameter(ENDING_CAPTURE_TIMEOUT_PARAMETER_NAME, endingTimeout);
        setParameter(ENDING_RECEIVE_TIMEOUT_PARAMETER_NAME, endingTimeout);
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
        return modules.contains(CAPTURE_MODULENAME);
    }

    public boolean hasTransmitModule() {
        return modules.contains(TRANSMIT_MODULENAME);
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
        return response != null && response.trim().equals(OK_STRING);
    }

    @Override
    public void open() throws IOException, HarcHardwareException {
        hardware.open();
        waitFor(OK_STRING, lineEnding, /*delay*/ 100, /* tries = */ 3);
        hardware.sendString(VERSION_COMMAND + lineEnding);
        version = hardware.readString(true).trim();
        if (verbose)
            System.err.println(VERSION_COMMAND + " returned '" + version + "'.");
        hardware.sendString(MODULES_COMMAND + lineEnding);
        String line = hardware.readString(true);
        if (verbose)
            System.err.println(VERSION_COMMAND + " returned '" + version + "'.");
        if (line != null)
            modules = Arrays.asList(line.toLowerCase(Locale.US).split("\\s+"));
        setUseReceiveForCapture();
        hasParameters =  modules.contains(PARAMETERS_MODULENAME);
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
        StringBuilder str = new StringBuilder(SEND_COMMAND);
        str.append(SEPARATOR).append(Integer.toString(count));
        str.append(SEPARATOR).append(Integer.toString((int) ModulatedIrSequence.getFrequencyWithDefault(irSignal.getFrequency())));
        str.append(SEPARATOR).append(Integer.toString(irSignal.getIntroLength()));
        str.append(SEPARATOR).append(Integer.toString(irSignal.getRepeatLength()));
        str.append(SEPARATOR).append(Integer.toString(irSignal.getEndingLength()));

        str.append(join(irSignal.getIntroSequence(), SEPARATOR));
        str.append(join(irSignal.getRepeatSequence(), SEPARATOR));
        str.append(join(irSignal.getEndingSequence(), SEPARATOR));

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
            hardware.sendString(CAPTURE_COMMAND + lineEnding);
            pendingCapture = true;
        }
        ModulatedIrSequence seq = null;
        try {
            String str = hardware.readString(true);
            pendingCapture = false;
            if (str == null || str.length() == 0 || str.startsWith("null") || str.startsWith(TIMEOUT_STRING))
                return null;

            str = str.trim();
            if (str.toUpperCase(Locale.US).startsWith(ERROR_STRING))
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
            if (ex.getMessage().equals("Underlying input stream returned zero bytes")) //RXTX timeout
                return null;
            throw ex;
        } catch (OddSequenceLengthException ex) {
            throw new HarcHardwareException(ex);
        }
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
            hardware.sendString(RECEIVE_COMMAND + lineEnding);
            pendingCapture = true;
        }

        IrSequence seq = null;
        try {
            //open();
            String str = hardware.readString(true);
            pendingCapture = false;
            if (str == null || str.length() == 0 || str.startsWith("null") || str.startsWith(TIMEOUT_STRING))
                return null;

            str = str.trim();

            seq = new IrSequence(str);
        } catch (SocketTimeoutException ex) {
            return null;
        } catch (IOException ex) {
            if (ex.getMessage().equals("Underlying input stream returned zero bytes")) //RXTX timeout
                return null;
            throw ex;
        } catch (OddSequenceLengthException ex) {
            throw new HarcHardwareException(ex);
        }
        return seq;
    }

    @Override
    public boolean stopReceive() {
        throw new UnsupportedOperationException("Not supported yet.");// TODO
    }

    public void reset() throws IOException, HarcHardwareException {
        hardware.sendString(RESET_COMMAND);
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
        sendStringWaitOk(LED_COMMAND + SEPARATOR + led + SEPARATOR + (state ? "on" : "off"));
    }

    public void setLcd(String message) throws IOException, HarcHardwareException {
        sendStringWaitOk(LCD_COMMAND + " " + message);
    }

    private void sendStringWaitOk(String line) throws IOException, HarcHardwareException {
        hardware.sendString(line + lineEnding);
        String answer = readString(true);
        if (answer == null)
            throw new HarcHardwareException("No \"" + OK_STRING + "\" received.");
        answer = answer.trim();
        if (!answer.startsWith(OK_STRING))
            throw new HarcHardwareException("No \"" + OK_STRING + "\" received, instead \"" + answer + "\".");
    }

    public long getParameter(String parameterName) throws IOException, HarcHardwareException {
        if (hasParameters) {
            hardware.sendString("parameter " + parameterName + lineEnding);
            String answer = readString(true);
            if (answer == null)
                throw new HarcHardwareException("No answer received.");
            long value = Long.parseLong(answer.split("=")[1]);
            return value;
        } else
            throw new HarcHardwareException("parameters not implemented.");
    }

    public void setParameter(String parameterName, int newValue) throws IOException, HarcHardwareException {
        if (hasParameters) {
            hardware.sendString(SET_PARAMETER_COMMAND + SEPARATOR + parameterName + SEPARATOR + Integer.toString(newValue) + lineEnding);
            String answer = readString(true);
            if (answer == null)
                throw new HarcHardwareException("No answer received.");
            if (!answer.equals(parameterName + "=" + Integer.toString(newValue)))
                throw new HarcHardwareException("Wrong answer received.");
        } else
            throw new HarcHardwareException("parameters not implemented.");
    }
}
