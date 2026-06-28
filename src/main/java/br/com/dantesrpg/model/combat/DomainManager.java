package br.com.dantesrpg.model.combat;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.map.Dominio;
import br.com.dantesrpg.model.util.DiceRoller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Responsável pelo sistema de colisão de domínios: detecção de sobreposição,
 * diálogo de disputa por dados, vitória/derrota/empate com expansão ou fusão.
 */
public class DomainManager {

	private final CombatManager combatManager;

	public DomainManager(CombatManager combatManager) {
		this.combatManager = combatManager;
	}

	private CombatController getController() {
		return combatManager.getMainController();
	}

	// ========== ATIVAÇÃO DE DOMÍNIO ==========

	public void ativarDominioNoMapa(Dominio novoDominio, Personagem ator, EstadoCombate estado) {
		if (getController() == null)
			return;

		if (!novoDominio.isDisputavel()) {
			getController().registrarDominio(novoDominio);
			return;
		}

		java.util.Map<String, Dominio> ativos = getController().getDominiosAtivos();
		Dominio conflitante = null;

		for (Dominio existente : ativos.values()) {
			if (!existente.isDisputavel())
				continue;
			if (existente.getId().equals(novoDominio.getId()))
				continue;
			if (novoDominio.sobrepoe(existente)) {
				conflitante = existente;
				break;
			}
		}

		if (conflitante == null) {
			getController().registrarDominio(novoDominio);
			return;
		}

		System.out.println(
				">>> COLISÃO DE DOMÍNIOS! [" + novoDominio.getId() + "] vs [" + conflitante.getId() + "]");

		Personagem donoExistente = conflitante.getDono();
		if (donoExistente == null) {
			getController().removerDominio(conflitante.getId());
			getController().registrarDominio(novoDominio);
			return;
		}

		int dadoNovo = getDadoArmaPersonagem(ator);
		int dadoExistente = getDadoArmaPersonagem(donoExistente);

		int[] resultados = abrirDialogDisputaDominio(ator, dadoNovo, donoExistente, dadoExistente);
		int rolagemNovo = resultados[0];
		int rolagemExistente = resultados[1];

		System.out.println(">>> Disputa: " + ator.getNome() + " rolou " + rolagemNovo
				+ " (d" + dadoNovo + ") vs " + donoExistente.getNome() + " rolou " + rolagemExistente
				+ " (d" + dadoExistente + ")");

		if (rolagemNovo > rolagemExistente) {
			// VITÓRIA DO NOVO
			System.out.println(">>> " + ator.getNome() + " VENCEU a disputa! Domínio expandido!");
			getController().removerDominio(conflitante.getId());
			Dominio expandido = Dominio.criarExpandido(novoDominio, conflitante);
			getController().registrarDominio(expandido);

			donoExistente.removerEfeito(conflitante.getNomeEfeito());
			donoExistente.recalcularAtributosEstatisticas();

		} else if (rolagemExistente > rolagemNovo) {
			// VITÓRIA DO EXISTENTE
			System.out.println(">>> " + donoExistente.getNome() + " VENCEU a disputa! Domínio expandido!");
			getController().removerDominio(conflitante.getId());
			Dominio expandido = Dominio.criarExpandido(conflitante, novoDominio);
			getController().registrarDominio(expandido);

			ator.removerEfeito(novoDominio.getNomeEfeito());
			ator.recalcularAtributosEstatisticas();

		} else {
			// EMPATE — fusão
			System.out.println(">>> EMPATE! Domínios FUNDIDOS! Ambos os efeitos se aplicam na área unida.");
			getController().removerDominio(conflitante.getId());

			String fusaoId = "fusao_" + novoDominio.getId() + "_" + conflitante.getId();
			Dominio fusao = Dominio.criarFusao(novoDominio, conflitante, fusaoId,
					"Fusão: " + novoDominio.getNomeEfeito() + " + " + conflitante.getNomeEfeito());
			getController().registrarDominio(fusao);

			Efeito efeitoNovo = ator.getEfeitosAtivos().get(novoDominio.getNomeEfeito());
			Efeito efeitoExist = donoExistente.getEfeitosAtivos().get(conflitante.getNomeEfeito());
			if (efeitoNovo != null && efeitoExist != null) {
				int media = (efeitoNovo.getDuracaoTURestante() + efeitoExist.getDuracaoTURestante()) / 2;
				efeitoNovo.setDuracaoTURestante(media);
				efeitoExist.setDuracaoTURestante(media);
				System.out.println(">>> Duração dos efeitos ajustada para média: " + media + " TU");
			}
		}
	}

	// ========== LIMPEZA DE FUSÕES ==========

	public void limparFusoesComDominio(String dominioIdOriginal) {
		if (getController() == null)
			return;
		java.util.Map<String, Dominio> ativos = getController().getDominiosAtivos();
		List<String> fusoesParaRemover = new ArrayList<>();
		for (Dominio dom : ativos.values()) {
			if (dom.isFusao()) {
				for (Dominio original : dom.getDominiosOriginais()) {
					if (original.getId().equals(dominioIdOriginal)) {
						fusoesParaRemover.add(dom.getId());
						break;
					}
				}
			}
		}
		for (String fusaoId : fusoesParaRemover) {
			System.out.println(">>> Removendo domínio fundido [" + fusaoId + "] (componente expirou).");
			getController().removerDominio(fusaoId);
		}
	}

	// ========== HELPERS ==========

	private int getDadoArmaPersonagem(Personagem p) {
		Arma arma = p.getArmaEquipada();
		if (arma == null)
			return 4;
		Atributo atrib = arma.getAtributoMultiplicador();
		int valorAtrib = p.getAtributosFinais().getOrDefault(atrib, 2);
		return DiceRoller.getTipoDado(valorAtrib);
	}

	// ========== DIÁLOGO DE DISPUTA ==========

	private int[] abrirDialogDisputaDominio(Personagem jogador1, int dado1, Personagem jogador2, int dado2) {
		javafx.scene.control.Dialog<int[]> dialog = new javafx.scene.control.Dialog<>();
		dialog.setTitle("COLISÃO DE DOMÍNIOS!");
		dialog.setHeaderText("Os domínios se sobrepõem! Rolagem de disputa necessária.");

		javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10);
		content.setStyle("-fx-padding: 20; -fx-background-color: #1a1a2a;");

		javafx.scene.control.Label lblTitulo = new javafx.scene.control.Label("DISPUTA DE DOMÍNIOS");
		lblTitulo.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 18px; -fx-font-weight: bold;"
				+ " -fx-effect: dropshadow(gaussian, red, 6, 0.3, 0, 0);");

		javafx.scene.control.Label lbl1 = new javafx.scene.control.Label(
				jogador1.getNome() + " — d" + dado1);
		lbl1.setStyle("-fx-text-fill: cyan; -fx-font-size: 14px; -fx-font-weight: bold;");
		javafx.scene.control.TextField input1 = new javafx.scene.control.TextField();
		input1.setPromptText("Resultado do d" + dado1 + "...");
		input1.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-font-size: 16px;"
				+ " -fx-alignment: center;");

		javafx.scene.control.Label lblVs = new javafx.scene.control.Label("VS");
		lblVs.setStyle("-fx-text-fill: #ff6666; -fx-font-size: 22px; -fx-font-weight: bold;"
				+ " -fx-alignment: center;");
		lblVs.setMaxWidth(Double.MAX_VALUE);
		lblVs.setAlignment(javafx.geometry.Pos.CENTER);

		javafx.scene.control.Label lbl2 = new javafx.scene.control.Label(
				jogador2.getNome() + " — d" + dado2);
		lbl2.setStyle("-fx-text-fill: orange; -fx-font-size: 14px; -fx-font-weight: bold;");
		javafx.scene.control.TextField input2 = new javafx.scene.control.TextField();
		input2.setPromptText("Resultado do d" + dado2 + "...");
		input2.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-font-size: 16px;"
				+ " -fx-alignment: center;");

		javafx.scene.control.Label lblResultado = new javafx.scene.control.Label("");
		lblResultado.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
		lblResultado.setMaxWidth(Double.MAX_VALUE);
		lblResultado.setAlignment(javafx.geometry.Pos.CENTER);

		javafx.beans.value.ChangeListener<String> previewListener = (obs, oldVal, newVal) -> {
			try {
				int v1 = Integer.parseInt(input1.getText().trim());
				int v2 = Integer.parseInt(input2.getText().trim());
				if (v1 > v2) {
					lblResultado.setText(jogador1.getNome() + " VENCE — Domínio Expandido!");
					lblResultado.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: cyan;");
				} else if (v2 > v1) {
					lblResultado.setText(jogador2.getNome() + " VENCE — Domínio Expandido!");
					lblResultado.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: orange;");
				} else {
					lblResultado.setText("EMPATE — Domínios FUNDIDOS!");
					lblResultado.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #cc44cc;"
							+ " -fx-effect: dropshadow(gaussian, purple, 4, 0.3, 0, 0);");
				}
			} catch (NumberFormatException e) {
				lblResultado.setText("");
			}
		};
		input1.textProperty().addListener(previewListener);
		input2.textProperty().addListener(previewListener);

		content.getChildren().addAll(lblTitulo, lbl1, input1, lblVs, lbl2, input2, lblResultado);

		dialog.getDialogPane().setContent(content);
		dialog.getDialogPane().setStyle("-fx-background-color: #1a1a2a;");

		javafx.scene.control.ButtonType btnConfirmar = new javafx.scene.control.ButtonType("Confirmar Disputa",
				javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().add(btnConfirmar);

		dialog.setOnCloseRequest(e -> {
			try {
				Integer.parseInt(input1.getText().trim());
				Integer.parseInt(input2.getText().trim());
			} catch (Exception ex) {
				e.consume();
			}
		});

		dialog.setResultConverter(buttonType -> {
			if (buttonType == btnConfirmar) {
				try {
					int v1 = Integer.parseInt(input1.getText().trim());
					int v2 = Integer.parseInt(input2.getText().trim());
					return new int[] { v1, v2 };
				} catch (NumberFormatException e) {
					return new int[] { 1, 1 };
				}
			}
			return new int[] { 1, 1 };
		});

		Optional<int[]> result = dialog.showAndWait();
		return result.orElse(new int[] { 1, 1 });
	}
}
