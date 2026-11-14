package org.eclipse.embedcdt.internal.debug.gdbjtag.openocd.ui;

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.console.ConsoleColorProvider;
import org.eclipse.swt.graphics.Color;

/**
 * OpenOCD writes its whole log, not only errors, to stderr, so the stderr
 * stream is shown in the standard output colour.
 */
public class ServerConsoleColorProvider extends ConsoleColorProvider {

	@Override
	public Color getColor(String streamIdentifier) {

		if (IDebugUIConstants.ID_STANDARD_ERROR_STREAM.equals(streamIdentifier)) {
			return super.getColor(IDebugUIConstants.ID_STANDARD_OUTPUT_STREAM);
		}
		return super.getColor(streamIdentifier);
	}
}
