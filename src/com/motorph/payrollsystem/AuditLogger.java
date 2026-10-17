package com.motorph.payrollsystem;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class AuditLogger {

	private static final Logger LOGGER = Logger.getLogger("com.motorph.payrollsystem.audit");
	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final String LOG_DIRECTORY = "logs";
	private static final String LOG_FILE = "logs/audit.log";

	private static boolean initialized = false;

	private AuditLogger() {
	}

	private static synchronized void initialize() {
		if (initialized) {
			return;
		}
		try {
			new File(LOG_DIRECTORY).mkdirs();
			FileHandler handler = new FileHandler(LOG_FILE, 0, 1, true);
			handler.setFormatter(new Formatter() {
				@Override
				public String format(LogRecord record) {
					return LocalDateTime.now().format(TIMESTAMP) + "," + record.getMessage()
							+ System.lineSeparator();
				}
			});
			LOGGER.addHandler(handler);
			LOGGER.setUseParentHandlers(false);
			LOGGER.setLevel(Level.INFO);
		} catch (IOException e) {
			System.err.println("Unable to initialize audit log: " + e.getMessage());
		}
		initialized = true;
	}

	public static void log(String action, String details) {
		initialize();
		String user = UserLogin.user;
		if (user == null || user.trim().isEmpty()) {
			user = "SYSTEM";
		}
		LOGGER.info(escape(user) + "," + escape(action) + "," + escape(details));
	}

	private static String escape(String value) {
		if (value == null) {
			return "\"\"";
		}
		String cleaned = value.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ");
		return "\"" + cleaned + "\"";
	}
}
