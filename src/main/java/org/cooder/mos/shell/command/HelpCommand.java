/*
 * This file is part of MOS <p> Copyright (c) 2021 by cooder.org <p> For the full copyright and license information,
 * please view the LICENSE file that was distributed with this source code.
 */
package org.cooder.mos.shell.command;

import java.io.OutputStream;
import java.io.PrintWriter;

import org.cooder.mos.Utils;
import org.cooder.mos.shell.Shell;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.ColorScheme;
import picocli.CommandLine.IHelpCommandInitializable2;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;

@Command(name = "help", header = "Displays help information about the specified command~",
    synopsisHeading = "%n@|bold,underline Usage|@:%n", helpCommand = true,
    description = {"%nWhen no COMMAND is given, the usage help for the main command is displayed.",
        "If a COMMAND is specified, the help for that command is shown.%n"},
    usageHelpAutoWidth = true)
public class HelpCommand implements IHelpCommandInitializable2, Runnable {

    @CommandLine.ParentCommand
    protected Shell shell;

    @Parameters(paramLabel = "COMMAND", descriptionKey = "helpCommand.command",
        description = "The COMMAND to display the usage help message for.")
    private String[] commands = new String[0];

    private CommandLine self;
    private OutputStream out;
    private ColorScheme colorScheme;

    @Override
    public void run() {
        CommandLine parent = self == null ? null : self.getParent();
        if (parent == null) {
            return;
        }
        if (commands.length > 0) {
            CommandLine subcommand = parent.getSubcommands().get(commands[0]);
            if (subcommand != null) {
                Utils.printMsg(shell.out, subcommand.getUsageMessage(colorScheme).replaceAll("\n", "\r\n"));
            } else {
                throw new ParameterException(parent, "Unknown subcommand '" + commands[0] + "'.", null, commands[0]);
            }
        } else {
            Utils.printMsg(shell.out, parent.getUsageMessage(colorScheme).replaceAll("\n", "\r\n"));
        }
    }

    @Override
    public void init(CommandLine helpCommandLine, ColorScheme colorScheme, PrintWriter out, PrintWriter err) {
        this.self = helpCommandLine;
        this.colorScheme = colorScheme;
        this.out = shell.out;
    }
}