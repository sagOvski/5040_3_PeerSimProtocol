package org.sagovski.inf5040.peersim.reports;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class ExceptionUtils {

	public static final String getStrStackTrace(final Exception exception) {
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		exception.printStackTrace(printWriter);
		printWriter.flush();
		return writer.toString();
	}

}
