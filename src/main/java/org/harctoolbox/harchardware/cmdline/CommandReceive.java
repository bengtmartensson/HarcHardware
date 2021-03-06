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
import java.util.logging.Logger;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.ir.IReceive;
import org.harctoolbox.ircore.IrSequence;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.OddSequenceLengthException;

@SuppressWarnings("FieldMayBeFinal")

@Parameters(commandNames = {"receive"}, commandDescription = "Receive a IR signal using demodulating receiver.")
public class CommandReceive extends CommandCapture {

    private static final Logger logger = Logger.getLogger(CommandReceive.class.getName());

    // Should there be a --count?

    @Parameter(names = {"-f", "--frequency"}, description = "Overriding frequency for received signal.")
    private Double frequency = ModulatedIrSequence.DEFAULT_FREQUENCY;

    @Override
    public String description() {
        return "This command captures a command from the selected hardware, using a demodulating sensor.";
    }

    @Override
    public ModulatedIrSequence collect(IHarcHardware hardware) throws IOException, HarcHardwareException, OddSequenceLengthException {
        IReceive hw = (IReceive) hardware;
        IrSequence irSequence = hw.receive();
        return irSequence != null ? new ModulatedIrSequence(irSequence, frequency) : null;
    }
}
