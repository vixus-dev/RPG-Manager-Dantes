package br.com.dantesrpg.model.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class SessionLogger {
	private static final ObservableList<String> logEntries = FXCollections.observableArrayList();
	private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");

	public static void log(String message) {
		String timestamp = LocalDateTime.now().format(dtf);
		String entry = "[" + timestamp + "] " + message;
		logEntries.add(entry);
		System.out.println("LOGGER: " + message); // Também imprime no console
	}

	public static ObservableList<String> getLogEntries() {
		return logEntries;
	}

	public static void limparLog() {
		logEntries.clear();
	}
}