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
import com.beust.jcommander.Parameters;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.logging.Logger;
import org.harctoolbox.analyze.Analyzer;
import org.harctoolbox.analyze.Cleaner;
import org.harctoolbox.analyze.NoDecoderMatchException;
import org.harctoolbox.analyze.RepeatFinder;
import org.harctoolbox.cmdline.AbstractCommand;
import org.harctoolbox.cmdline.UsageException;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.ir.ICapture;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.Pronto;
import org.harctoolbox.ircore.ThisCannotHappenException;
import org.harctoolbox.irp.Decoder;
import org.harctoolbox.irp.IrpDatabase;
import org.harctoolbox.irp.IrpParseException;
import org.harctoolbox.irp.Protocol;
import org.harctoolbox.irp.UnknownProtocolException;
import org.xml.sax.SAXException;

@SuppressWarnings("FieldMayBeFinal")

@Parameters(commandNames = {"capture"}, commandDescription = "Receive a IR signal using demodulating receiver.")
public class CommandCapture extends AbstractCommand {

    private static final Logger logger = Logger.getLogger(CommandCapture.class.getName());
    private static final int radix = 10;

    @Parameter(names = {"-a", "--analyze"}, description = "Send the received signal into the analyzer.")
    private boolean analyze = false;

    @Parameter(names = {"-#", "--count"}, description = "Capture this many signals before exiting.")
    private Integer count = null;

    @Parameter(names = {"-c", "--cleaner"}, description = "Clean received signals")
    private boolean clean = false;

    @Parameter(names = {"-d", "--decode"}, description = "Attempt to decode the signal received.")
    private boolean decode = false;

    @Parameter(names = {"-p", "--pronto"}, description = "Output signal as Pronto Hex.")
    private boolean pronto = false;

    @Parameter(names = {"-r", "--raw"}, description = "Output signal in raw form.")
    private boolean raw = false;

    @Parameter(names = {"-R", "--repeatfinder"}, description = "Invoke the repeatfinder on the captured signal.")
    private boolean repeatFinder = false;
    private Decoder decoder = null;

    @Override
    public String description() {
        return "This command captures a command from the selected hardware, using a non-demodulating sensor.";
    }

    public boolean collect(PrintStream out, CommandCommonOptions commandLineArgs, IHarcHardware hardware) throws UsageException, HarcHardwareException, InvalidArgumentException, IOException, IrpParseException, UnknownProtocolException, SAXException {
        commandLineArgs.assertNonNullClass();
        if (! (pronto || raw || decode || analyze)) {
            logger.warning("No output format selected, forcing Pronto Hex");
            pronto = true;
        }

        boolean success = true;
        for (int i = 0; i < (count == null ? 1 : count); i++) {
            boolean succ = collectOne(out, commandLineArgs, hardware);
            success = success && succ;
        }
        return success;
    }

    private boolean collectOne(PrintStream out, CommandCommonOptions commandLineArgs, IHarcHardware hardware) throws HarcHardwareException, IOException, UsageException, SAXException, InvalidArgumentException, IrpParseException, UnknownProtocolException {
        if (!commandLineArgs.quiet)
            out.println("Now send a signal to the selected capturing device.");

        ModulatedIrSequence irSequence = collect(hardware, commandLineArgs);
        if (irSequence == null || irSequence.isEmpty()) {
            out.println("No signal received.");
            return false;
        }

        if (clean)
            irSequence = Cleaner.clean(irSequence);

        IrSignal irSignal = repeatFinder ? RepeatFinder.findRepeat(irSequence) : new IrSignal(irSequence);

        if (pronto)
            out.println(Pronto.toString(irSignal));

        if (raw)
            out.println(irSignal.toString(true));

        if (decode) {
            if (decoder == null) {
                IrpDatabase irpDatabase = commandLineArgs.setupDatabase();
                decoder = new Decoder(irpDatabase);
            }
            Decoder.DecoderParameters parameters = commandLineArgs.decoderParameters();
            if (repeatFinder) {
                Decoder.SimpleDecodesSet decodes = decoder.decodeIrSignal(irSignal, parameters);
                for (Decoder.Decode dec : decodes)
                    out.println(dec);
                if (decodes.isEmpty() && !commandLineArgs.quiet)
                    out.println("No decodes.");
            } else {
                Decoder.DecodeTree decodes = decoder.decode(irSequence, parameters);
                for (Decoder.TrunkDecodeTree dec : decodes)
                    out.println(dec);
                if (decodes.isEmpty() && !commandLineArgs.quiet)
                    out.println("No decodes.");
            }
        }

        if (analyze) {
            Analyzer analyzer = new Analyzer(irSequence, irSequence.getFrequencyWithDefault(),
                    repeatFinder, commandLineArgs.absoluteTolerance, commandLineArgs.relativeTolerance);
            try {
                List<Protocol> protocols = analyzer.searchBestProtocol(new Analyzer.AnalyzerParams());
                protocols.forEach((protocol) -> {
                    out.println(protocol.toIrpString(radix));
                });
            } catch (NoDecoderMatchException ex) {
                throw new ThisCannotHappenException();
            }
         }
        return true;
    }

    public ModulatedIrSequence collect(IHarcHardware hardware, CommandCommonOptions commandLineArgs) throws HarcHardwareException, IOException, InvalidArgumentException {
        ICapture hw = (ICapture) hardware;
        commandLineArgs.setupTimeouts(hw);
        return hw.capture();
    }
}
