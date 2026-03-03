package br.com.dantesrpg.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

public class DiceRollPromptController {

	@FXML
	private Label labelPrompt;
	@FXML
	private TextField inputRolagem;
	@FXML
	private Button btnConfirmar;

	private CombatController mainController;

	@FXML
	public void initialize() {
		// Permite confirmar pressionando ENTER no TextField
		inputRolagem.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER) {
				onConfirmarClick();
			}
		});
	}

	public void setMainController(CombatController controller) {
		this.mainController = controller;
	}

	public void preparar(String textoPrompt, String promptDado) {
		labelPrompt.setText(textoPrompt);
		inputRolagem.setPromptText(promptDado);
		inputRolagem.clear();
		inputRolagem.requestFocus();
	}

	@FXML
	private void onConfirmarClick() {
		if (mainController != null) {
			mainController.resolverRolagemReacao(inputRolagem.getText());
		} else {
			System.err.println("Erro Crítico: DiceRollPrompt não tem referência ao MainController.");
		}
	}

	public void requestFocus() {
		inputRolagem.requestFocus();
	}
}