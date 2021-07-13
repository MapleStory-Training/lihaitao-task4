/*
 * This file is part of MOS <p> Copyright (c) 2021 by cooder.org <p> For the full copyright and license information,
 * please view the LICENSE file that was distributed with this source code.
 */
package org.cooder.mos.shell.command;

import org.cooder.mos.Utils;

import picocli.CommandLine.Command;

@Command(name = "pwd", description = "展示当前工作目录的绝对路径")
public class Pwd extends MosCommand {

    @Override
    public int runCommand() {
        Utils.printMsg(out, shell.currentPath());
        Utils.writeNewLine(shell.getOut());
        return 0;
    }
}
