package org.cooder.mos.shell;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

import org.cooder.mos.Utils;

import picocli.CommandLine;

/**
 * <自定义命令行实现，实现自己的流输出>
 *
 * @author lihaitao on 2021/6/6
 */
public class MosCommandLine extends CommandLine {

    private OutputStream out;
    private OutputStream err;
    private IParameterExceptionHandler parameterExceptionHandler = (ex, args) -> {
        CommandLine cmd = ex.getCommandLine();
        Help.ColorScheme colorScheme = cmd.getColorScheme();
        PrintWriter writer = cmd.getErr();
        writer.println(colorScheme.errorText(ex.getMessage()));
        boolean hasSuggestion = ex instanceof UnmatchedArgumentException;
        if (ex instanceof UnmatchedArgumentException) {
            List<String> suggestions = ((UnmatchedArgumentException)ex).getSuggestions();
            boolean unknownOption = ((UnmatchedArgumentException)ex).isUnknownOption();
            String msg = unknownOption ? "Possible solutions: " + str(suggestions)
                : "Did you mean: " + str(suggestions).replace(", ", " or ") + "?";
            Utils.printlnErrorMsg(err, msg.replace("\n", "\r\n"));
            hasSuggestion = !suggestions.isEmpty();
        }
        if (!hasSuggestion) {
            ex.getCommandLine().usage(writer, colorScheme);
        }
        IExitCodeExceptionMapper exitCodeExceptionMapper = cmd.getExitCodeExceptionMapper();
        return (exitCodeExceptionMapper != null) ? exitCodeExceptionMapper.getExitCode(ex)
            : cmd.getCommandSpec().exitCodeOnInvalidInput();
    };

    MosCommandLine(Object command, OutputStream out, OutputStream err) {
        super(command);
        this.out = out;
        this.err = err;
    }

    private static String str(List<String> list) {
        String s = list.toString();
        return s.substring(0, s.length() - 1).substring(1);
    }

    @Override
    public void usage(PrintWriter out, Help.ColorScheme colorScheme) {
        String usageMessage = getUsageMessage().replaceAll("\n", "\r\n");
        Utils.printMsgNotFlush(this.out, usageMessage);
    }

    @Override
    public PrintWriter getErr() {
        if (err == null) {
            setErr(new PrintWriter(System.err, true));
        }
        return new PrintWriter(err);
    }

    @Override
    public PrintWriter getOut() {
        if (out == null) {
            setErr(new PrintWriter(System.out, true));
        }
        return new PrintWriter(out);
    }

    @Override
    public IParameterExceptionHandler getParameterExceptionHandler() {
        return parameterExceptionHandler;
    }
}
