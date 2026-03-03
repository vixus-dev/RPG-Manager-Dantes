package br.com.dantesrpg.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AttributeTestPromptController {

	@FXML
	private Label lblContexto;
	@FXML
	private Label lblDificuldade;
	@FXML
	private TextField inputRolagem;

	private Stage stage;
	private boolean confirmado = false;
	private int resultadoRolagem = 0;

	public void setDados(Stage stage, String nomePersonagem, int na, String textoDificuldade) {
		this.stage = stage;
		this.lblContexto.setText(nomePersonagem + " atingiu o limite do Empréstimo.");
		this.lblDificuldade.setText(na + " (" + textoDificuldade + ")");
		this.inputRolagem.requestFocus();
	}

	@FXML
	private void onConfirmarClick() {
		try {
			this.resultadoRolagem = Integer.parseInt(inputRolagem.getText());
			this.confirmado = true;
			stage.close();
		} catch (NumberFormatException e) {
			inputRolagem.setStyle("-fx-border-color: red; -fx-background-color: #333; -fx-text-fill: white;");
		}
	}

	public boolean isConfirmado() {
		return confirmado;
	}

	public int getResultadoRolagem() {
		return resultadoRolagem;
	}
}