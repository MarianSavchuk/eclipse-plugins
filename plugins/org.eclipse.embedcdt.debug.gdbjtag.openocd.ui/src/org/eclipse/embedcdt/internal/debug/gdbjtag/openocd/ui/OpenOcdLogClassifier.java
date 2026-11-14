package org.eclipse.embedcdt.internal.debug.gdbjtag.openocd.ui;

import java.util.regex.Pattern;

/**
 * Decides, line by line, whether an OpenOCD log line is an error.
 * <p>
 * OpenOCD prefixes its own errors with {@code "Error: "}. Tcl script errors
 * are logged without that prefix as {@code "<file>:<line>: Error: <msg>"},
 * followed by a traceback, which has a different format in v0.12.0 and in
 * later versions.
 */
public class OpenOcdLogClassifier {

	private static final String ERROR_PREFIX = "Error: "; //$NON-NLS-1$
	private static final Pattern TCL_ERROR = Pattern.compile("^\\S+:\\d+: Error: "); //$NON-NLS-1$

	private boolean fInErrorBlock;

	/**
	 * @param line one line, with or without the line terminator
	 * @return true if the line belongs to an error message
	 */
	public boolean isError(String line) {

		if (line.startsWith(ERROR_PREFIX) || TCL_ERROR.matcher(line).find()) {
			fInErrorBlock = true;
			return true;
		}
		if (fInErrorBlock && isTracebackLine(line)) {
			return true;
		}
		fInErrorBlock = false;
		return false;
	}

	private static boolean isTracebackLine(String line) {

		return line.startsWith("Traceback (most recent call last):") //$NON-NLS-1$
				|| line.startsWith(" ") //$NON-NLS-1$
				|| line.startsWith("\t") //$NON-NLS-1$
				|| line.startsWith("in procedure ") //$NON-NLS-1$
				|| line.startsWith("at file "); //$NON-NLS-1$
	}
}
