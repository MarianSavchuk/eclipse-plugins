package org.eclipse.embedcdt.internal.debug.gdbjtag.openocd.ui;

import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.ui.console.ConsoleColorProvider;
import org.eclipse.debug.ui.console.IConsole;

/**
 * OpenOCD writes its whole log, not only errors, to stderr. The console is
 * connected to a proxy that splits the output by severity, so only error
 * lines use the error colour.
 */
public class ServerConsoleColorProvider extends ConsoleColorProvider {

	private IProcess fProcess;
	private IConsole fConsole;

	@Override
	public void connect(IProcess process, IConsole console) {

		fProcess = process;
		fConsole = console;
		IStreamsProxy streamsProxy = process.getStreamsProxy();
		if (streamsProxy != null) {
			console.connect(new SeverityStreamsProxy(streamsProxy));
		}
	}

	@Override
	public void disconnect() {

		fProcess = null;
		fConsole = null;
	}

	@Override
	public boolean isReadOnly() {
		return fProcess == null || fProcess.isTerminated();
	}

	@Override
	protected IProcess getProcess() {
		return fProcess;
	}

	@Override
	protected IConsole getConsole() {
		return fConsole;
	}
}
