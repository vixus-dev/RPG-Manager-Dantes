package br.com.dantesrpg.controller;

import br.com.dantesrpg.model.util.SessionLogger;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

public class SessionLogController {
	@FXML
	private ListView<String> listViewLog;

	@FXML
	public void initialize() {
		listViewLog.setItems(SessionLogger.getLogEntries());
		SessionLogger.getLogEntries().addListener((javafx.collections.ListChangeListener<String>) c -> {
			listViewLog.scrollTo(listViewLog.getItems().size() - 1);
		});
	}
}