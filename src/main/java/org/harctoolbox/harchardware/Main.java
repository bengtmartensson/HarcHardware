/*
Copyright (C) 2012, 2013, 2014, 2019, 2020 Bengt Martensson.

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
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.cmdline.CmdLineProgram;
import org.harctoolbox.cmdline.ProgramExitStatus;
import org.harctoolbox.cmdline.UsageException;
import org.harctoolbox.devslashlirc.LircHardware;
import org.harctoolbox.harchardware.cmdline.CommandCapture;
import org.harctoolbox.harchardware.cmdline.CommandCommonOptions;
import org.harctoolbox.harchardware.cmdline.CommandGetCommands;
import org.harctoolbox.harchardware.cmdline.CommandGetRemotes;
import org.harctoolbox.harchardware.cmdline.CommandReceive;
import org.harctoolbox.harchardware.cmdline.CommandTransmit;
import org.harctoolbox.harchardware.cmdline.CommandVersion;
import org.harctoolbox.harchardware.ir.NoSuchTransmitterException;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.irp.IrpException;
import org.harctoolbox.irp.IrpParseException;
import org.harctoolbox.irp.UnknownProtocolException;
import org.xml.sax.SAXException;

/**
 * This class contains a command line main routine, allowing command line access to most things in the package.
 */
public final class Main extends CmdLineProgram {

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    private static void main(String[] args, PrintStream out) {
        Main instance = new Main(out);
        ProgramExitStatus status = instance.run(args);
        status.die();
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] args) {
        main(args, System.out);
    }

    private final CommandCommonOptions commandLineArgs;
    private final CommandTransmit commandTransmit = new CommandTransmit();
    private final CommandReceive commandReceive = new CommandReceive();
    private final CommandCapture commandCapture = new CommandCapture();
    private final CommandGetRemotes commandGetRemotes = new CommandGetRemotes();
    private final CommandGetCommands commandGetCommands = new CommandGetCommands();
    private final CommandVersion commandVersion = new CommandVersion();

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public Main() {
        this(System.out);
    }

    public Main(PrintStream out) {
        super(out, new CommandCommonOptions(), Version.appName);
        setupCmds(commandTransmit,
                commandReceive,
                commandCapture,
                commandGetRemotes,
                commandGetCommands,
                commandVersion);
        commandLineArgs = (CommandCommonOptions) commandBasicOptions;
    }

    @Override
    public ProgramExitStatus processCommand() {
        try {
            LircHardware.loadLibrary(); // FIXME
        } catch (UnsatisfiedLinkError ex) {
            logger.log(Level.INFO, "Library DevSlashLirc could not be loaded");
        }

        try (IHarcHardware hardware = commandLineArgs.hardware()) {
            if (hardware != null)
                hardware.open();
            switch (command) {
                case "transmit":
                    commandTransmit.transmit(out, commandLineArgs, hardware);
                    break;
                case "capture":
                    commandCapture.collect(out, commandLineArgs, hardware);
                    break;
                case "receive":
                    commandReceive.collect(out, commandLineArgs, hardware);
                    break;
                case "remotes":
                    commandGetRemotes.getRemotes(out, commandLineArgs, hardware);
                    break;
                case "commands":
                    commandGetCommands.getCommands(out, commandLineArgs, hardware);
                    break;
                case "help":
                    commandHelp.help(out, new CommandCommonOptions(), argumentParser, Version.documentationUrl);
                    break;
                case "version":
                    if (commandLineArgs.hasClass()) {
                        commandVersion.version(out, commandLineArgs, hardware);
                    } else {
                        commandVersion.version(out, commandLineArgs);
                    }
                    break;
                default:
                    return new ProgramExitStatus(Version.appName, ProgramExitStatus.EXIT_USAGE_ERROR, "Unknown command: " + command);
            }
            return new ProgramExitStatus();
        } catch (ClassCastException ex) {
            return new ProgramExitStatus(Version.appName, ProgramExitStatus.EXIT_USAGE_ERROR, "Class " + commandLineArgs.className
                    + " does not implement the requested functionallity for command \"" + command + "\".");
        } catch (IOException ex) {
            // Likely a programming error or fatal error in the data base. Barf.
            ex.printStackTrace();
            return new ProgramExitStatus(Version.appName, ProgramExitStatus.EXIT_FATAL_PROGRAM_FAILURE, ex.getLocalizedMessage());
        } catch (NoSuchTransmitterException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            return new ProgramExitStatus(Version.appName, ProgramExitStatus.EXIT_FATAL_PROGRAM_FAILURE, ex.getLocalizedMessage());
        } catch (HarcHardwareException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            return new ProgramExitStatus(Version.appName, ProgramExitStatus.EXIT_FATAL_PROGRAM_FAILURE, ex.getLocalizedMessage());
        } catch (UsageException | UnknownProtocolException ex) {
            // Exceptions likely from silly user input, just print the exception
            return new ProgramExitStatus(Version.appName, ProgramExitStatus.EXIT_USAGE_ERROR, ex.getLocalizedMessage());
        } catch (IrpParseException ex) {
            // TODO: Improve error message
            if (commandLineArgs.logLevel.intValue() < Level.INFO.intValue())
                ex.printStackTrace();
            return new ProgramExitStatus(Version.appName, ProgramExitStatus.EXIT_USAGE_ERROR, "Parse error in \"" + ex.getText() + "\": " + ex.getLocalizedMessage());
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            ex.printStackTrace();
            return new ProgramExitStatus(Version.appName, ProgramExitStatus.EXIT_FATAL_PROGRAM_FAILURE, ex.getLocalizedMessage());
        } catch (InvalidArgumentException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            return new ProgramExitStatus(Version.appName, ProgramExitStatus.EXIT_FATAL_PROGRAM_FAILURE, ex.getLocalizedMessage());
        } catch (IrpException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            return new ProgramExitStatus(Version.appName, ProgramExitStatus.EXIT_FATAL_PROGRAM_FAILURE, ex.getLocalizedMessage());
        } catch (SAXException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            return new ProgramExitStatus(Version.appName, ProgramExitStatus.EXIT_XML_ERROR, ex.getLocalizedMessage());
        }
    }
}
