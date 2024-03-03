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
import java.util.logging.Logger;
import org.harctoolbox.cmdline.AbstractCommand;
import org.harctoolbox.cmdline.UsageException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.ir.IRemoteCommandIrSender;

@SuppressWarnings("FieldMayBeFinal")

@Parameters(commandNames = {"commands"}, commandDescription = "Get the commands contained in the selected remote.")
public class CommandGetCommands extends AbstractCommand {

    private static final Logger logger = Logger.getLogger(CommandGetCommands.class.getName());

    @Parameter(description = "Remote", required = true)
    private String remote;

    @Override
    public String description() {
        return "This command list the commands contained in the remote given as argument.";
    }

    public void getCommands(PrintStream out, CommandCommonOptions commandLineArgs, IHarcHardware hardware) throws UsageException, IOException {
        commandLineArgs.assertNonNullClass();
        String[] commands = ((IRemoteCommandIrSender) hardware).getCommands(remote);
        if (commands == null)
            throw new UsageException("No such remote \"" + remote + "\"");
        for (String command : commands)
            out.println(command);
    }
}
