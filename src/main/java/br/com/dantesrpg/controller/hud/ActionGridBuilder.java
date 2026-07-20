package br.com.dantesrpg.controller.hud;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.*;
import java.util.function.Consumer;

/**
 * Constrói o grid de botões de ação das abas de Ataques e Itens.
 * Extraído de DetailedTurnHUDController para isolamento de responsabilidade.
 */
public class ActionGridBuilder {

	@FunctionalInterface
	public interface ActionSelectionCallback {
		void onSelected(String titulo, Habilidade hab, Item item, FantasmaNobre fn, boolean isBasicAttack);
	}

	private final GridPane actionsGrid;
	private final CombatController mainController;

	public ActionGridBuilder(GridPane actionsGrid, CombatController mainController) {
		this.actionsGrid = actionsGrid;
		this.mainController = mainController;
	}

	// ========== ABA ATAQUES ==========

	/**
	 * @param ator         Personagem ativo no turno
	 * @param selectionCb  Callback chamado quando o jogador seleciona uma ação
	 * @param murasameCb   Callback especial para Invocação Murasame (tratado no controller)
	 */
	public void popularAbaAtaques(Personagem ator, ActionSelectionCallback selectionCb,
			Consumer<FantasmaNobre> murasameCb) {
		actionsGrid.getChildren().clear();
		int row = 0, col = 0;

		// Ataque Básico
		col = adicionarBotaoAtaqueBasico(ator, selectionCb, col, row);
		if (col > 1) { col = 0; row++; }

		// Habilidades (regra diferenciada para clones)
		for (Habilidade hab : resolverHabilidades(ator)) {
			if (hab.getTipo() == TipoHabilidade.ATIVA) {
				Button btn = criarBotaoHabilidade(hab, ator);
				btn.setOnAction(e -> selectionCb.onSelected(hab.getNome(), hab, null, null, false));
				adicionarAoGrid(btn, col++, row);
				if (col > 1) { col = 0; row++; }
			}
		}

		// Fantasma Nobre (apenas personagens não-clone)
		if (!ator.isClone()) {
			FantasmaNobre fn = ator.getFantasmaNobre();
			if (fn != null) {
				Button btnFN = criarBotaoFantasmaNobre(fn, ator);
				btnFN.setOnAction(e -> {
					if (fn instanceof br.com.dantesrpg.model.fantasmasnobres.InvocacaoMurasame) {
						murasameCb.accept(fn);
					} else {
						selectionCb.onSelected(fn.getNome(), null, null, fn, false);
					}
				});
				adicionarAoGrid(btnFN, col, row);
			}
		}
	}

	// ========== ABA ITENS ==========

	public void popularAbaItens(Personagem ator, ActionSelectionCallback selectionCb) {
		actionsGrid.getChildren().clear();
		int row = 0, col = 0;

		if (ator.getInventario() == null) return;

		for (Map.Entry<String, Integer> entry : ator.getInventario().getItensAgrupados().entrySet()) {
			Item itemModelo = mainController.getItem(entry.getKey());
			if (itemModelo != null && itemModelo.isUsavelEmCombate()) {
				int qtd = entry.getValue();
				Button btn = criarBotaoAcao(itemModelo.getNome() + "\n(x" + qtd + ")", "-fx-base: #333;");
				btn.setOnAction(e -> selectionCb.onSelected(itemModelo.getNome(), null, itemModelo, null, false));
				adicionarAoGrid(btn, col++, row);
				if (col > 1) { col = 0; row++; }
			}
		}
	}

	// ========== PRIVADOS ==========

	private int adicionarBotaoAtaqueBasico(Personagem ator, ActionSelectionCallback cb, int col, int row) {
		if (!ator.getEfeitosAtivos().containsKey("Modo Justiça")) {
			Arma arma = ator.getArmaEquipada();
			String texto = "Ataque Básico";
			String estilo = "-fx-base: #500;";
			if (arma != null) {
				if (arma.isOverclockado()) {
					texto = arma.getNomeComOverclock();
					estilo = "-fx-base: #005555; -fx-text-fill: cyan; -fx-effect: dropshadow(gaussian, cyan, 3, 0.2, 0, 0);";
				}
				if (arma.isRequerMunicao()) {
					texto += "\n(" + arma.getMunicaoAtual() + "/" + arma.getMunicaoMaxima() + ")";
				}
			}
			Button btn = criarBotaoAcao(texto, estilo);
			btn.setOnAction(e -> cb.onSelected("Ataque Básico", null, null, null, true));
			adicionarAoGrid(btn, col++, row);
		} else {
			Label lbl = new Label("Ataque Físico Bloqueado\n(Modo Justiça Ativo)");
			lbl.setStyle("-fx-text-fill: gray; -fx-font-style: italic; -fx-font-size: 10px;");
			adicionarAoGrid(lbl, col++, row);
		}
		return col;
	}

	private List<Habilidade> resolverHabilidades(Personagem ator) {
		List<Habilidade> lista = new ArrayList<>();
		if (ator.isClone() && ator.getCriador() != null) {
			Habilidade ultimaHab = ator.getCriador().getUltimaHabilidadeUsada();
			if (ultimaHab != null && mainController.getCombatManager().habilidadePodeSerCopiadaPorClone(ultimaHab)) {
				lista.add(ultimaHab);
			}
		} else if (ator.getHabilidadesDeClasse() != null) {
			lista.addAll(ator.getHabilidadesDeClasse());
		}
		return lista;
	}

	private Button criarBotaoHabilidade(Habilidade hab, Personagem ator) {
		Button btn = criarBotaoAcao(hab.getNome(), "-fx-base: #333;");
		String cdName = "CD:" + hab.getNome();
		if (ator.isHabilidadeBloqueadaPorCoral(hab.getNome())) {
			btn.setDisable(true);
			btn.setText("\uD83E\uDEB8 CORAL\n" + hab.getNome());
			btn.setTooltip(new Tooltip("A Maldição de Coral bloqueou esta habilidade."));
			btn.setStyle("-fx-base: #7d2935; -fx-text-fill: #ffd5cb; -fx-border-color: #ff7f6e;"
					+ " -fx-border-width: 2; -fx-opacity: 0.88;");
		} else if (ator.getEfeitosAtivos().containsKey(cdName)) {
			btn.setDisable(true);
			btn.setText(hab.getNome() + "\n(Recarga)");
		} else if (ator.getManaAtual() < hab.getCustoMana()) {
			btn.setDisable(true);
			btn.setStyle("-fx-opacity: 0.5;");
		}
		return btn;
	}

	private Button criarBotaoFantasmaNobre(FantasmaNobre fn, Personagem ator) {
		Button btn = criarBotaoAcao("FN: " + fn.getNome(), "-fx-base: #400040; -fx-border-color: violet;");
		if (ator.getEfeitosAtivos().containsKey("CD:" + fn.getNome())) {
			btn.setDisable(true);
			btn.setText("FN: " + fn.getNome() + "\n(Recarga)");
			btn.setTooltip(new Tooltip("Fantasma nobre em recarga."));
		} else if (ator.getManaAtual() < fn.getCustoMana()) {
			btn.setDisable(true);
			btn.setText("FN: " + fn.getNome() + "\n(Sem Mana)");
			btn.setTooltip(new Tooltip("Mana insuficiente."));
			btn.setStyle(btn.getStyle() + " -fx-opacity: 0.5;");
		} else {
			String motivoBloqueio = fn.getMotivoBloqueio(ator);
			if (motivoBloqueio != null) {
				btn.setDisable(true);
				btn.setText("FN: " + fn.getNome() + "\n(Indisponivel)");
				btn.setTooltip(new Tooltip(motivoBloqueio));
				btn.setStyle(btn.getStyle() + " -fx-opacity: 0.5;");
			}
		}
		return btn;
	}

	private Button criarBotaoAcao(String texto, String style) {
		Button btn = new Button(texto);
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.setPrefHeight(60);
		btn.setWrapText(true);
		btn.setStyle(style + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
		return btn;
	}

	private void adicionarAoGrid(Node node, int col, int row) {
		actionsGrid.add(node, col, row);
	}
}
