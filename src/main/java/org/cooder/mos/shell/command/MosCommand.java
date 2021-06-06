/*
 * This file is part of MOS <p> Copyright (c) 2021 by cooder.org <p> For the full copyright and license information,
 * please view the LICENSE file that was distributed with this source code.
 */
/*
 * This file is part of MOS <p> Copyright (c) 2021 by cooder.org <p> For the full copyright and license information,
 * please view the LICENSE file that was distributed with this source code.
 */
package org.cooder.mos.shell.command;

import static org.cooder.mos.Utils.printlnError;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.Callable;

import org.cooder.mos.api.FileOutputStream;
import org.cooder.mos.api.MosFile;
import org.cooder.mos.fs.FileSystem;
import org.cooder.mos.shell.Shell;

import picocli.CommandLine.Command;
import picocli.CommandLine.Help.ColorScheme;
import picocli.CommandLine.Help.Column;
import picocli.CommandLine.Help.Column.Overflow;
import picocli.CommandLine.Help.TextTable;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command
public abstract class MosCommand implements Callable<Integer> {

    @ParentCommand
    protected Shell shell;

    protected OutputStream out;
    protected InputStream in;
    protected OutputStream err;

    protected abstract int runCommand();

    @Override
    public Integer call() {
        if (in == null) {
            in = shell.in;
        }
        if (out == null) {
            out = shell.out;
        }
        if (err == null) {
            err = shell.err;
        }

        try {
            return runCommand();
        } catch (Throwable t) {
            printlnError(shell.err, t);
        }
        return 0;
    }

    @Command(name = ">")
    public void redirect(@Parameters(paramLabel = "<path>") String path) throws IOException {
        writeFile(path, FileSystem.WRITE);
    }

    @Command(name = ">>")
    public void redirectAppend(@Parameters(paramLabel = "<path>", description = "output file path") String path)
        throws IOException {
        writeFile(path, FileSystem.APPEND);
    }

    private void writeFile(String path, int append) throws IOException {
        String[] paths = shell.absolutePath(path);
        FileOutputStream fos = new FileOutputStream(new MosFile(paths), append);
        this.out = new PrintStream(fos);

        try {
            call();
        } catch (Exception e) {
            // 输出的错误信息要输出到client
            printlnError(shell.out, e);
        }

        out.close();
        out = shell.out;
    }

    TextTable forColumnWidths(int... columnWidths) {
        Column[] columns = new Column[columnWidths.length];
        for (int i = 0; i < columnWidths.length; i++) {
            columns[i] = new Column(columnWidths[i], 0, Overflow.SPAN);
        }
        return TextTable.forColumns(new ColorScheme.Builder().build(), columns);
    }
}
