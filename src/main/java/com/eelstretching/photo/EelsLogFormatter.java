package com.eelstretching.photo;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Provides a log formatter. This formatter generates nicer looking console
 * messages than the default formatter. To use the formatter, set the property
 *
 * <code>java.util.logging.ConsoleHandler.formatter</code> to
 * <code>com.oracle.labs.mlrg.olcut.util.LabsLogFormatter</code>
 *
 * This is typically done in a custom logger.properties file
 */
public class EelsLogFormatter extends Formatter {

    private static final Logger logger = Logger.getLogger(EelsLogFormatter.class.getName());

    private boolean terse;

    /**
     * Sets the level of output
     *
     * @param terse if true, the output level should be terse
     */
    public void setTerse(boolean terse) {
        this.terse = terse;
    }

    /**
     * Retrieves the level of output
     *
     * @return the level of output
     */
    public boolean getTerse() {
        return terse;
    }

    /**
     * Formats the given log record and return the formatted string.
     *
     * @param record the record to format
     * @return the formatted string
     */
    public String format(LogRecord record) {
        String message = formatMessage(record);
        if (terse) {
            return message + '\n';
        } else {
            String cn = record.getSourceClassName();
            int p = cn.lastIndexOf('.');
            if (p > 0) {
                cn = cn.substring(p + 1);
            }
            String mn = record.getSourceMethodName();

            String msg = String.format("[%tD %tT:%tL] %s %s.%s %s",
                    record.getMillis(),
                    record.getMillis(),
                    record.getMillis(),
                    record.getLevel(),
                    cn, mn,
                    message);
            if (record.getThrown() != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                msg = msg + "\n" + sw.toString();
            }
            return msg + '\n';
        }
    }

    public static void setAllLogFormatters() {
        setAllLogFormatters(Level.ALL);
    }

    public static void setAllLogFormatters(Level level) {
        for (Handler h : Logger.getLogger("").getHandlers()) {
            h.setLevel(level);
            h.setFormatter(new EelsLogFormatter());
            try {
                h.setEncoding("utf-8");
            } catch (Exception ex) {
                logger.severe("Error setting output encoding");
            }
        }
    }

}
