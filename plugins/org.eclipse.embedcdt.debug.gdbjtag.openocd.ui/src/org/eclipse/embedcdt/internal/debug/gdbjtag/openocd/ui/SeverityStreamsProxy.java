package org.eclipse.embedcdt.internal.debug.gdbjtag.openocd.ui;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IFlushableStreamMonitor;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.core.model.IStreamsProxy2;

/**
 * Re-splits the OpenOCD output by severity instead of by file descriptor:
 * error lines go to the error monitor, everything else (stdout and the
 * non-error stderr lines) goes to the output monitor. Input is passed through.
 */
public class SeverityStreamsProxy implements IStreamsProxy, IStreamsProxy2 {

	private final IStreamsProxy fProxy;
	private final TextStreamMonitor fErrorMonitor = new TextStreamMonitor();
	private final TextStreamMonitor fOutputMonitor = new TextStreamMonitor();
	private final OpenOcdLogClassifier fClassifier = new OpenOcdLogClassifier();

	// Monitor that receives the rest of a line whose start was already routed.
	private TextStreamMonitor fOpenLineMonitor;

	public SeverityStreamsProxy(IStreamsProxy proxy) {

		fProxy = proxy;
		attach(proxy.getOutputStreamMonitor(), false);
		attach(proxy.getErrorStreamMonitor(), true);
	}

	private void attach(IStreamMonitor monitor, boolean isStderr) {

		if (monitor == null) {
			return;
		}
		IStreamListener listener = (text, m) -> route(text, isStderr);
		String contents;
		synchronized (monitor) {
			monitor.addListener(listener);
			contents = monitor.getContents();
			if (monitor instanceof IFlushableStreamMonitor) {
				((IFlushableStreamMonitor) monitor).flushContents();
				((IFlushableStreamMonitor) monitor).setBuffered(false);
			}
		}
		route(contents, isStderr);
	}

	private synchronized void route(String text, boolean isStderr) {

		if (text == null || text.isEmpty()) {
			return;
		}
		if (!isStderr) {
			fOutputMonitor.append(text);
			return;
		}

		int start = 0;
		while (start < text.length()) {
			int nl = text.indexOf('\n', start);
			int end = (nl < 0) ? text.length() : nl + 1;
			String piece = text.substring(start, end);

			TextStreamMonitor target = fOpenLineMonitor;
			if (target == null) {
				target = fClassifier.isError(piece) ? fErrorMonitor : fOutputMonitor;
			}
			target.append(piece);

			// A piece without '\n' is a partial line: its rest follows the same route.
			fOpenLineMonitor = (nl < 0) ? target : null;
			start = end;
		}
	}

	@Override
	public IStreamMonitor getErrorStreamMonitor() {
		return fErrorMonitor;
	}

	@Override
	public IStreamMonitor getOutputStreamMonitor() {
		return fOutputMonitor;
	}

	@Override
	public void write(String input) throws IOException {
		fProxy.write(input);
	}

	@Override
	public void closeInputStream() throws IOException {
		if (fProxy instanceof IStreamsProxy2) {
			((IStreamsProxy2) fProxy).closeInputStream();
		}
	}

	private static class TextStreamMonitor implements IFlushableStreamMonitor {

		private final List<IStreamListener> fListeners = new CopyOnWriteArrayList<>();
		private final StringBuilder fContents = new StringBuilder();
		private boolean fBuffered = true;

		void append(String text) {
			synchronized (this) {
				if (fBuffered) {
					fContents.append(text);
				}
			}
			for (IStreamListener listener : fListeners) {
				listener.streamAppended(text, this);
			}
		}

		@Override
		public void addListener(IStreamListener listener) {
			fListeners.add(listener);
		}

		@Override
		public void removeListener(IStreamListener listener) {
			fListeners.remove(listener);
		}

		@Override
		public synchronized String getContents() {
			return fContents.toString();
		}

		@Override
		public synchronized void flushContents() {
			fContents.setLength(0);
		}

		@Override
		public synchronized void setBuffered(boolean buffer) {
			fBuffered = buffer;
		}

		@Override
		public synchronized boolean isBuffered() {
			return fBuffered;
		}
	}
}
