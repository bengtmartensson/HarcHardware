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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.cmdline.AbstractCommand;
import org.harctoolbox.cmdline.NameEngineParser;
import org.harctoolbox.cmdline.UsageException;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.ir.IRawIrSender;
import org.harctoolbox.harchardware.ir.IRemoteCommandIrSender;
import org.harctoolbox.harchardware.ir.ITransmitter;
import org.harctoolbox.harchardware.ir.NoSuchTransmitterException;
import org.harctoolbox.harchardware.ir.Transmitter;
import org.harctoolbox.ircore.InvalidArgumentException;
import static org.harctoolbox.ircore.IrCoreUtils.WHITESPACE;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.MultiParser;
import org.harctoolbox.irp.IrpDatabase;
import org.harctoolbox.irp.IrpException;
import org.harctoolbox.irp.IrpParseException;
import org.harctoolbox.irp.NameEngine;
import org.harctoolbox.irp.UnknownProtocolException;
import org.xml.sax.SAXException;

@SuppressWarnings("FieldMayBeFinal")

@Parameters(commandNames = {"transmit", "send"}, commandDescription = "Transmit an IrSignal using selected hardware.")
public class CommandTransmit extends AbstractCommand {

    private static final Logger logger = Logger.getLogger(CommandTransmit.class.getName());

    private static final Double trailingGap = 10000.0;

    // This is static for performance reasons only; pretty expensive to set up.
    private static IrpDatabase irpDatabase = null;
    private static void setupIrpDatabaseUnlessDone(CommandCommonOptions commandLineArgs) throws UsageException, IrpParseException, IOException, UnknownProtocolException, SAXException {
        if (irpDatabase == null)
            irpDatabase = commandLineArgs.setupDatabase();
    }

    @Parameter(names = {"-#", "--count"}, description = "Number of times to send sequence")
    private int count = 1;

    @Parameter(names = {"-c", "--command"}, description = "Name of command (residing in the named remote) to send.")
    private String command = null;

    @Parameter(names = {"-F", "--file"}, description = "File name of file containing arguments; one send per line")
    private String file = null;

    @Parameter(names = {"-f", "--frequency"}, description = "Frequency in Hz, for use with raw signals")
    private Double frequency = null;

    @Parameter(names = {"-n", "--names"}, converter = NameEngineParser.class,
            description = "Parameter values for the protocol to be rendered, in the syntax of a name engine.")
    private NameEngine nameEngine = null;

    @Parameter(names = {"-p", "--protocol"}, description = "protocol name to be rendered")
    private String protocol = null;

    @Parameter(names = {"-r", "--remote"}, description = "Name of remote for command")
    private String remote = null;

    @Parameter(names = {"-t", "--transmitter"}, description = "Transmitter, semantic device dependent")
    private String transmitter = null;

    @Parameter(description = "remaining arguments")
    private List<String> args = new ArrayList<>(8);

    private Transmitter trnsmttr;

    @Override
    public String description() {
        return "This command sends an IR signal to the selected hardware. The IR signal can be either a named command "
                + "(using the --remote and --command options -- assuming it is known by the hardware), "
                + "a rendered signal (using the --nameengine and --protocol options), "
                + "or a raw signal in Pronto Hex- or raw format.";
    }

    public boolean transmit(PrintStream out, CommandCommonOptions commandLineArgs, IHarcHardware hardware)
            throws IOException, NoSuchTransmitterException, InvalidArgumentException, UsageException, HarcHardwareException, IrpParseException, UnknownProtocolException, IrpException, SAXException {
        commandLineArgs.assertNonNullClass();

        if (file != null) {
            if (remote != null || command != null || protocol != null || nameEngine != null || args.size() > 0)
                throw new UsageException("--file cannot be combined with any other option");
            return parseFile(out, commandLineArgs, hardware);
        }

        if (transmitter != null && transmitter.equals("?")) {
            ITransmitter hw = (ITransmitter) hardware;
            String[] transmitters = hw.getTransmitterNames();
            for (String trnsmit : transmitters)
                out.println(trnsmit);
            return true;
        }
        trnsmttr = transmitter != null ? ((ITransmitter) hardware).getTransmitter(transmitter) : null;

        boolean status;
        if ((remote != null) != (command != null))
            throw new UsageException("--remote and --command must be given together");
        else if (remote != null)
            status = transmitNamedCommand(hardware);
        else if (protocol != null != (nameEngine != null))
            throw new UsageException("--protocol and --names must be given together");
        else if (protocol != null)
            status = transmitRender(commandLineArgs, hardware);
        else
            status = transmitRaw(hardware);
        logger.log(Level.INFO, "Sending {0}.", status ? "succeded" : "failed");
        return status;
    }

    @SuppressWarnings("SleepWhileInLoop")
    private boolean parseFile(PrintStream out, CommandCommonOptions commandLineArgs, IHarcHardware hardware)
            throws IOException, InvalidArgumentException, UsageException, HarcHardwareException, SAXException, IrpException, IrpParseException {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null)
                    return true;
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty())
                    continue;
                if (line.startsWith("delay")) {
                    String[] arr = line.split(WHITESPACE);
                    long millis = Long.parseLong(arr[1]);
                    try {
                        Thread.sleep(millis);
                    } catch (InterruptedException ex) {
                    }
                    continue;
                }
                CommandTransmit commandTransmit = new CommandTransmit();
                JCommander jCommander = new JCommander(commandTransmit);
                jCommander.parse(line.split(WHITESPACE));
                boolean success = commandTransmit.transmit(out, commandLineArgs, hardware);
                if (!success)
                    return false;
            }
        }
        // Never gets here
    }

    private boolean transmitNamedCommand(IHarcHardware hardware) throws IOException, NoSuchTransmitterException {
        IRemoteCommandIrSender namedCommandSender = (IRemoteCommandIrSender) hardware;
        return namedCommandSender.sendIrCommand(remote, command, count, trnsmttr);
    }

    private boolean transmitRaw(IHarcHardware hardware) throws InvalidArgumentException, UsageException, HarcHardwareException, NoSuchTransmitterException, IOException {
        MultiParser prontoRawParser = MultiParser.newIrCoreParser(args);
        IrSignal irSignal = prontoRawParser.toIrSignal(frequency, trailingGap);
        if (irSignal == null) {
            logger.log(Level.WARNING, "Could not parse as IrSignal: {0}", String.join(" ", args));
            return false;
        }
        if (irSignal.isEmpty()) {
            logger.log(Level.WARNING, "No signal given");
            return false;
        }
        return sendRaw(hardware, irSignal);
    }

    private boolean transmitRender(CommandCommonOptions commandLineArgs, IHarcHardware hardware) throws UsageException, IrpParseException, IOException, UnknownProtocolException, IrpException, HarcHardwareException, NoSuchTransmitterException, InvalidArgumentException, SAXException {
        setupIrpDatabaseUnlessDone(commandLineArgs);
        IrSignal irSignal = irpDatabase.render(protocol, nameEngine.toMap());
        return sendRaw(hardware, irSignal);
    }

    private boolean sendRaw(IHarcHardware hardware, IrSignal irSignal) throws HarcHardwareException, NoSuchTransmitterException, IOException, InvalidArgumentException {
        IRawIrSender hw = (IRawIrSender) hardware;
        return hw.sendIr(irSignal, count, trnsmttr);
    }
}
