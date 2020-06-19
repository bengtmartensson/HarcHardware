/*
Copyright (C) 2019 Bengt Martensson.

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
import org.harctoolbox.cmdline.CommandIrpDatabaseOptions;
import org.harctoolbox.cmdline.UsageException;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.harchardware.Version;
import org.harctoolbox.irp.IrpDatabase;
import org.harctoolbox.irp.IrpParseException;
import org.harctoolbox.irp.UnknownProtocolException;
import org.xml.sax.SAXException;

@SuppressWarnings("PublicField")

@Parameters(commandNames = {"version"}, commandDescription = "Report version and license, or, if the --class argument is given, the version of the hardware.")
public class CommandVersion extends AbstractCommand {

    private static final Logger logger = Logger.getLogger(CommandVersion.class.getName());

    @Parameter(names = {"-s", "--short"}, description = "Issue only the version number of the program proper.")
    @SuppressWarnings("FieldMayBeFinal")
    private boolean shortForm = false;

    @Override
    public String description() {
        return "This command returns the version. and licensing information for the program. If the --class option is given, instead the version of the hardware is reported.";
    }

    public void version(PrintStream out, CommandIrpDatabaseOptions commandLineArgs) throws UsageException, IrpParseException, IOException, UnknownProtocolException, SAXException {
        if (shortForm || commandLineArgs.quiet)
            out.println(Version.version);
        else {
            IrpDatabase irpDatabase = commandLineArgs.setupDatabase();
            out.println(Version.versionString);
            //setupDatabase();
            out.println("Database: " + (commandLineArgs.configFiles != null ? commandLineArgs.configFiles : "")
                    + " version: " + irpDatabase.getConfigFileVersion());

            out.println("JVM: " + System.getProperty("java.vendor") + " " + System.getProperty("java.version") + " " + System.getProperty("os.name") + "-" + System.getProperty("os.arch"));
            out.println();
            out.println(Version.licenseString);
        }
    }

    public void version(PrintStream out, CommandCommonOptions commandLineArgs, IHarcHardware hardware) throws IOException, HarcHardwareException {
        String version = hardware.getVersion();
        out.println(version);
    }

}
