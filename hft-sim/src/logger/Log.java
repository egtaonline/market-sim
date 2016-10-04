package logger;

import static logger.Log.Level.NO_LOGGING;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * A simple logger that just allows writing structured messages to a stream with
 * some filtering. This is not meant to log long running processes.
 * 
 * @author erik
 * 
 */
public final class Log {

	public static enum Level { NO_LOGGING, ERROR, INFO, DEBUG };
	
	private static final Prefix emptyPrefix = new Prefix() {
		@Override
		public String getPrefix() { return ""; }
	};
	private static Log nullLogger = Log.create(NO_LOGGING, new PrintWriter(new Writer() {
		@Override
		public void close() throws IOException { }
		@Override
		public void flush() throws IOException { }
		@Override
		public void write(char[] cbuf, int off, int len) throws IOException { }
	}), emptyPrefix);
	
	/**
	 * Use this as the global default logger to log any/all messages. Defaults to a nullLogger
	 */
	public static Log log = nullLogger;

	private PrintWriter printwriter;
	private Prefix prefix;
	private Level level;
	
	private Log(Level level, PrintWriter printwriter, Prefix prefix) {
		this.level = level;
		this.printwriter = printwriter;
		this.prefix = prefix;
	}
	
	public static Log create(Level level, PrintWriter printwriter, Prefix prefix) {
		return new Log(level, printwriter, prefix);
	}
	
	public static Log create(Level level, File logFile, Prefix prefix) throws IOException {
		logFile.getParentFile().mkdirs();
		return new Log(level, new PrintWriter(logFile), prefix);
	}
	
	public static Log create(Level level, File logFile) throws IOException {
		return create(level, logFile, emptyPrefix);
	}
	
	public void closeLogger() {
		printwriter.flush();
		printwriter.close();
	}
	
	/**
	 * Creates a logger that prints everything to System.err.
	 * 
	 * Potentially useful for tests
	 * 
	 * @param level
	 * @param prefix
	 * @return
	 */
	public static Log createStderrLogger(Level level, Prefix prefix) {
		return Log.create(level, new PrintWriter(System.err), prefix);
	}
	
	/**
	 * Creates a logger that ignores all logging
	 * @return
	 */
	public static Log nullLogger() {
		return nullLogger;
	}

	/**
	 * Method to log a message. This should be used in a very strict way, that
	 * is format should be a static string (not one that you build), and all of
	 * the paramaterised options should be stuck in parameters. An example call
	 * is
	 * 
	 * <code>log(INFO, "Object %s, Int %d, Float %.4f", new Object(), 5, 6.7)</code>
	 * 
	 * By calling log like this you can avoid expensive logging operations when
	 * you're not actually logging, and use the fact that the strings are
	 * written directly to the file without having to build a large string in
	 * memory first.
	 * 
	 * For more information on how to properly format strings you can look at
	 * the String.format method, or the PrintWriter.format method.
	 * 
	 * @param level
	 * @param format
	 * @param parameters
	 */
	public void log(Level level, String format, Object... parameters) {
		if (level.ordinal() > this.level.ordinal())
			return;
		printwriter.append(prefix.getPrefix());
		printwriter.append(Integer.toString(level.ordinal())).append("| ");
		printwriter.format(format, parameters);
		printwriter.append('\n');
	}
	
	/**
	 * An interface to allow an arbitrary prefix before every logged method. An
	 * example prefix might generate the system time so that you know when a
	 * line is logged.
	 * 
	 * @author erik
	 * 
	 */
	public static interface Prefix {
		public String getPrefix();
	}

}
