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

package org.harctoolbox.harchardware.ir;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.comm.LocalSerialPort;
import org.harctoolbox.harchardware.comm.LocalSerialPortRaw;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.OddSequenceLengthException;
import org.harctoolbox.ircore.Pronto;

/**
 * This class implements capturing and sending support for the CommandFusion Learner.
 * see <a href="http://www.commandfusion.com/wiki2/hardware/cflink/ir-learner">IR learner</a>,
 * <a href="http://www.commandfusion.com/wiki2/hardware/cflink/ir-module">IR Module</a>, and
 * <a href="https://docs.google.com/document/d/1BMRwD9RlUYtf4VeJNXgRwo6-lkkSAIVo8tczrynJ7CU/preview?pli=1">USB Communication Protocol</a>.
 *
 * This device does not support settable timeouts, due to limitations in the hardware.
 * beginTimeout is effectively 20 seconds,
 * captureLength 2 seconds,
 * endingTimeout: not applicable.
 */
// It would probably be possible to get the serial timeout to work as beginTimeout, but my tries
// rendered a very unreliably working device.
public class CommandFusion extends IrSerial<LocalSerialPortRaw> implements IRawIrSender, ICapture {

    private static final Logger logger = Logger.getLogger(CommandFusion.class.getName());

    // USB parameters:
    //    VID = 0403
    //    PID = 6001

    private final static int CAPTUREWINDOW = 2000;
    private static final int PORTID = 1;
    private static final byte[] INTROBYTES = { (byte) 0xF2, (byte) PORTID, (byte) 0xF3 };

    private static final byte MIDDLETOKEN = (byte) 0xF4;
    private static final byte ENDINGTOKEN = (byte) 0xF5;
    private static final byte TRANSMITTOKEN = (byte) 'T';
    private static final byte RECEIVETOKEN = (byte) 'R';
    private static final byte QUERYTOKEN = (byte) 'Q';

    private static final String LEARNERNAME = "IRL";
    private static final String SENDCOMMAND = "SND";
    private static final String CAPTURECOMMAND = "LIR";
    private static final String READCOMMAND = "RIR";
    private static final String VERSIONCOMMAND = "WHO";
    private static final int COMMANDLENGTH = LEARNERNAME.length();

    private static final String TIMEOUT = "TIMEOUT";
    private static final String START = "START";
    private static final String SIGNAL = "SIGNAL";
    private static final String IRCODE = "IRCODE";
    private static final String END = "END";

    private static final int TICK = 25; // micro seconds

    public static final String DEFAULTPORTNAME = "ftdi";///dev/ttyUSB0";
    public static final int DEFAULTBAUDRATE = 115200;
    public static final int DEFAULT_TIMEOUT = 10000;
    public static final String COMMAND_FUSION = "Command Fusion";
    private static final int DATASIZE = 8;
    private static final LocalSerialPort.StopBits STOPBITS = LocalSerialPort.StopBits.ONE;
    private static final LocalSerialPort.Parity PARITY = LocalSerialPort.Parity.NONE;
    private static final LocalSerialPort.FlowControl DEFAULTFLOWCONTROL = LocalSerialPort.FlowControl.NONE;

    private static byte[] encode(IrSignal irSignal, int count) {
        Objects.requireNonNull(irSignal, "irSignal cannot be null");
        String data = "P0" + Integer.toString(PORTID) + ":RAW:" + Pronto.toString(irSignal.toOneShot(count));
        return encode(SENDCOMMAND, data);
    }

    // "All communications to and from the IR Learner’s serial port use the below format:
    //  \xF2\x01\xF3<COMMAND>\xF4<DATA>\xF5\xF5"

    // <COMMAND> is always made up of 7 characters:
    // 1 = T (transmitted TO IR learner) or R (reply FROM IR learner)
    // 2-4 = IRL (signifying we are communicating with an IR Learner)
    // 5-7 = The command name. See below for available commands.
    private static Payload decode(byte[] data, Byte token) {
        if (data == null)
            return null;
        int index = 0;
        for (int i = 0; i < INTROBYTES.length; i++) {
            if (data[i] != INTROBYTES[i])
                return null;
            index++;
        }

        if (token != null && data[index] != token)
            return null;
        index++;
        for (int i = 0; i < LEARNERNAME.length(); i++) {
            if (data[index] != LEARNERNAME.charAt(i))
                return null;
            index++;
        }

        Payload payload = new Payload();
        payload.command = new String(data, index, COMMANDLENGTH, Charset.forName("US-ASCII"));
        index += COMMANDLENGTH;
        if (data[index] != MIDDLETOKEN)
            return null;
        index++;
        for (int i = data.length-2; i < data.length; i++)
            if (data[i] != ENDINGTOKEN)
                return null;
        payload.data = new String(data, index, data.length - index - 2, Charset.forName("US-ASCII")); // possibly empty
        return payload;
    }

    private static byte[] encode(String cmd, String data) {
        return encode(cmd, data, TRANSMITTOKEN);
    }

    @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
    private static byte[] encode(String cmd, String data, byte token) {
        byte[] result = new byte[7 + LEARNERNAME.length() + cmd.length() + data.length()];
        int index = 0;
        for (int i = 0; i < INTROBYTES.length; i++)
            result[index++] = INTROBYTES[i];
        result[index++] = token;
        for (int i = 0; i < LEARNERNAME.length(); i++)
            result[index++] = (byte) LEARNERNAME.charAt(i);
        for (int i = 0; i < cmd.length(); i++)
            result[index++] = (byte) cmd.charAt(i);
        result[index++] = (byte) 0xF4;
        for (int i = 0; i < data.length(); i++)
            result[index++] = (byte) data.charAt(i);
        result[index++] = (byte) 0xF5;
        result[index++] = (byte) 0xF5;
        return result;
    }

    private static byte[] encode(String cmd) {
        return encode(cmd, "");
    }

    private boolean stopRequested = false;
    private String versionString = null;

    public CommandFusion() throws IOException {
        this(DEFAULTPORTNAME);
    }

    public CommandFusion(String portName) throws IOException {
        this(portName, false);
    }

    public CommandFusion(String portName, boolean verbose) throws IOException {
        this(portName, verbose, null);
    }

    public CommandFusion(String portName, boolean verbose, Integer timeout) throws IOException {
        super(LocalSerialPortRaw.class, LocalSerialPort.canonicalizePortName(portName, DEFAULTPORTNAME), verbose, DEFAULT_TIMEOUT, DEFAULTBAUDRATE, DATASIZE, STOPBITS, PARITY, DEFAULTFLOWCONTROL);
    }

    // Necessary for the HardwareManager. Do not "clean up".
    public CommandFusion(String portName, boolean verbose, Integer timeoutNotUsed, Integer baudRate) throws IOException {
        this(portName, verbose, baudRate);
    }

    /**
     * Dummy without function.
     * @param debug
     */
    @Override
    public void setDebug(int debug) {
    }

    @Override
    public void open() throws HarcHardwareException, IOException {
        try {
            super.open();
            fetchVersion();
        } catch (IOException | HarcHardwareException ex) {
            close();
            throw ex;
        }
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private void fetchVersion() throws IOException, HarcHardwareException {
        send(encode(VERSIONCOMMAND, "", QUERYTOKEN));
        byte[] response = readUntilTwoEndTokens();
        Payload payload = decode(response, RECEIVETOKEN);
        if (verbose)
            System.err.println("<Received " + payload);
        if (payload == null)
            throw new HarcHardwareException("Cannot open CommandFusion.");

        if (payload.command.equals(VERSIONCOMMAND)) {
            String s[] = payload.data.split(":");
            versionString = s[2];
        }
    }

    /**
     * Sends an IR signal from the <a href="http://www.commandfusion.com/irdatabase">built-in, proprietary data base</a>.
     * Not tested.
     *
     * @param deviceType
     * @param codeset
     * @param key function code
     * @return success of operation
     * @throws IOException
     */
    public boolean sendIr(int deviceType, int codeset, int key) throws IOException {
        return sendIr(encode(SENDCOMMAND,
                             String.format("P%02d:DBA:%02d:%04d:%02d", PORTID, deviceType, codeset, key)));
    }

    /**
     *
     * @param irSignal
     * @param count
     * @param transmitter Not used
     * @return Success of operation.
     * @throws IOException
     */
    @Override
    public boolean sendIr(IrSignal irSignal, int count, Transmitter transmitter) throws IOException {
        return sendIr(encode(irSignal, count));
    }

    private boolean sendIr(byte[] data) throws IOException {
        send(data);
        return expect(SENDCOMMAND, "") == Status.OK;
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private Status expect(String command, String data) throws IOException {
        byte[] response = readUntilTwoEndTokens();// serialPort.readBytes(13 + data.length());
        if (response == null)
            return Status.TIMEOUT;
        Payload payload = decode(response, RECEIVETOKEN);
        if (verbose)
            System.err.println("<Received " + payload);

        return payload == null ? Status.ERROR
                : payload.data.equals(TIMEOUT) ? Status.TIMEOUT
                : (payload.command.equals(command) && payload.data.equals(data)) ? Status.OK
                : Status.ERROR;
    }

    private byte[] readUntilTwoEndTokens() throws IOException {
        List<Byte> data = new ArrayList<>(200);
        int noEndingTokensFound = 0;
        while (noEndingTokensFound < 2) {
            if (stopRequested)
                return null;
            int x = serialPort.readByte();
            if (x == -1)
                return null;
                //throw new IOException("EOF from CommandFusion");
            data.add((byte) x);
            if ((byte) x == ENDINGTOKEN)
                noEndingTokensFound++;
        }
        byte[] result = new byte[data.size()];
        int i = 0;
        for (Byte b : data) {
            result[i] = b;
            i++;
        }
        return result;
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private void send(byte[] buf) throws IOException {
        if (verbose) {
            Payload payload = decode(buf, null);
            if (payload != null)
                System.err.println(">Sending " + payload);
        }
        serialPort.sendBytes(buf);
    }

    @Override
    public ModulatedIrSequence capture() throws IOException, InvalidArgumentException {
        stopRequested = false;
        Status status;
        do {
            if (stopRequested)
                return null;

            //Send LIR command (Learn IR) to tell the learner we are ready to learn.
            send(encode(CAPTURECOMMAND));

            //Learner will reply with an LIR command with data value START
            status = expect(CAPTURECOMMAND, START);

            if (stopRequested)
                return null;
        } while (status != Status.OK);

        //The learner is now waiting for you to press a button on your remote and send an IR signal to the ‘LEARN’ window of the IR Learner.
        //When a signal is first detected, IR Learner will send back an LIR command with data value SIGNAL. If no signal was detected for 20 seconds after the START command was issued, the IR learner will send back an LIR command with data value TIMEOUT
        status = expect(CAPTURECOMMAND, SIGNAL); // Blocks until signal starts, or timeout from CF in 20 seconds
        if (status != Status.OK || stopRequested)
            return null;

        try {
            //Wait for 2 seconds for the IR buffer to fill whilst holding the remote button.
            Thread.sleep(CAPTUREWINDOW);
        } catch (InterruptedException ex) {
        }

        if (stopRequested)
                return null;

        //Send a RIR command (Read IR) with no data.
        send(encode(READCOMMAND));

        //IR Learner will send back a RIR reply with data in the format of IRCODE:<irdata>
        //Process the IR Data using the CFIRProcessor.dll or your own code. The IR data format is documented below.
        ModulatedIrSequence modulatedIrSequence = null;
        try {
            modulatedIrSequence = readCapture();
        } catch (InvalidArgumentException ex) {
            throw ex;
        } finally {
            // Finally, the IR Learner will send back an LIR command with a data value END to signify the end of the IR
            // Do this also in the case of an exception, to not lose sync.
            status = expect(CAPTURECOMMAND, END);
        }
        if (status != Status.OK)
            return null;

        return modulatedIrSequence;
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private ModulatedIrSequence readCapture() throws IOException, OddSequenceLengthException, InvalidArgumentException {
        //IR Learner will send back a RIR reply with data in the format of IRCODE:<irdata>
        byte[] response = readUntilTwoEndTokens();
        if (response == null)
            return null;
        Payload payload = decode(response, RECEIVETOKEN);
        if (verbose) {
            System.err.println("<Received " + payload);
        }
        if (payload == null || !payload.command.equals(CAPTURECOMMAND))
            return null;

        if (!payload.data.startsWith(IRCODE + ":"))
            return null;
        int index = IRCODE.length() + 1;
        double frequency = Pronto.frequency(Integer.parseInt(payload.data.substring(index, index+4), 16));
        index += 4;
        if ((payload.data.length() - index) % 4 != 0)
            throw new InvalidArgumentException("Receive length erroneous");

        ArrayList<Integer> durations = new ArrayList<>(payload.data.length() - index);
        boolean lastState = false;
        int accumulated = 0;
        for (int i = index; i < payload.data.length(); i += 4) {
            boolean state = payload.data.substring(i, i + 2).equals("01");
            int duration = Integer.parseInt(payload.data.substring(i + 2, i + 4), 16);
            if (lastState != state) {
                if (accumulated > 0)
                    durations.add(TICK * accumulated);
                accumulated = duration;
            } else {
                accumulated += duration;
            }
            lastState = state;
        }
        durations.add(TICK * accumulated);
        int[] data = new int[durations.size()];
        for (int i = 0; i < data.length; i++)
            data[i] = durations.get(i);

        return new ModulatedIrSequence(data, frequency, -1.0);
    }

    @Override
    public boolean stopCapture() {
        stopRequested = true;
        return true;
    }

    @Override
    public String getVersion() {
        return versionString;
    }

    /**
     * Dummy without function.
     * @param integer
     */
    @Override
    public void setBeginTimeout(int integer) {
    }

    /**
     * Dummy without function.
     * @param integer
     */
    @Override
    public void setCaptureMaxSize(int integer) {
    }

    /**
     * Dummy without function.
     * @param integer
     */
    @Override
    public void setEndingTimeout(int integer) {
    }

    /*
    From http://www.commandfusion.com/wiki2/hardware/cflink/ir-module
    Error numbers:
    003 = Invalid Port Number
    004 = Invalid Module Number
    450 = Invalid IR Format Type
    451 = Invalid IR Database Parameters
    452 = Invalid IR Memory Parameters
    453 = Invalid IR Raw Hex Code
    454 = Invalid CF IR format
    */

    private static enum Status {
        OK,
        TIMEOUT,
        ERROR
    }

    private static class Payload {
        public String command;
        public String data;

        @Override
        public String toString() {
            return "command \"" + command + "\", data \"" + data + "\"";
        }
    }
}
