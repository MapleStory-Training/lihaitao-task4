/*
 * This file is part of MOS <p> Copyright (c) 2021 by cooder.org <p> For the full copyright and license information,
 * please view the LICENSE file that was distributed with this source code.
 */
package org.cooder.mos.shell.command;

import org.cooder.mos.Utils;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "echo", description = "输出参数内容")
public class Echo extends MosCommand {
    @Parameters(paramLabel = "<content>")
    private String content;

    @Override
    public int runCommand() {
        Utils.printMsg(out, content);
        if (out == shell.out) {
            Utils.writeNewLine(shell.out);
        }
        return 0;
    }
}
