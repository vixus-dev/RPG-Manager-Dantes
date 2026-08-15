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
			if (fn != null && fn.possuiAcaoAtiva()) {
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
				Button btn = criarBotaoAcao(itemModelo.getNome() + "\nQuantidade: " + qtd, "hud-action-item");
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
			String estilo = "hud-action-basic";
			if (arma != null) {
				if (arma.isOverclockado()) {
					texto = arma.getNomeComOverclock();
					estilo = "hud-action-overclock";
				}
				texto += "\nTU " + arma.getCustoTU();
				if (arma.isRequerMunicao()) texto += "  •  Munição " + arma.getMunicaoAtual() + "/" + arma.getMunicaoMaxima();
			}
			Button btn = criarBotaoAcao(texto, estilo);
			btn.setOnAction(e -> cb.onSelected("Ataque Básico", null, null, null, true));
			adicionarAoGrid(btn, col++, row);
		} else {
			Label lbl = new Label("Ataque Físico Bloqueado\n(Modo Justiça Ativo)");
			lbl.setWrapText(true);
			lbl.getStyleClass().add("hud-action-placeholder");
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
		String resumo = hab.getNome() + "\nMP " + hab.getCustoMana() + "  •  TU "
				+ hab.getCustoTUModificado(ator);
		Button btn = criarBotaoAcao(resumo, "hud-action-item");
		String cdName = "CD:" + hab.getNome();
		if (ator.isHabilidadeBloqueadaPorCoral(hab.getNome())) {
			marcarIndisponivel(btn, hab.getNome() + "\nBLOQUEADA • CORAL",
					"hud-action-blocked", "A Maldição de Coral bloqueou esta habilidade.");
		} else if (ator.getEfeitosAtivos().containsKey(cdName)) {
			marcarIndisponivel(btn, hab.getNome() + "\nEM RECARGA",
					"hud-action-unavailable", "Esta habilidade ainda está em recarga.");
		} else if (ator.getManaAtual() < hab.getCustoMana()) {
			marcarIndisponivel(btn, hab.getNome() + "\nMANA INSUFICIENTE",
					"hud-action-unavailable", "Mana insuficiente para usar esta habilidade.");
		}
		return btn;
	}

	private Button criarBotaoFantasmaNobre(FantasmaNobre fn, Personagem ator) {
		Button btn = criarBotaoAcao("FN: " + fn.getNome() + "\nMP " + fn.getCustoMana()
				+ "  •  TU " + fn.getCustoTU(), "hud-action-ultimate");
		if (ator.getEfeitosAtivos().containsKey("CD:" + fn.getNome())) {
			marcarIndisponivel(btn, "FN: " + fn.getNome() + "\nEM RECARGA",
					"hud-action-unavailable", "Fantasma Nobre em recarga.");
		} else if (ator.getManaAtual() < fn.getCustoMana()) {
			marcarIndisponivel(btn, "FN: " + fn.getNome() + "\nMANA INSUFICIENTE",
					"hud-action-unavailable", "Mana insuficiente.");
		} else {
			String motivoBloqueio = fn.getMotivoBloqueio(ator);
			if (motivoBloqueio != null) {
				marcarIndisponivel(btn, "FN: " + fn.getNome() + "\nINDISPONÍVEL",
						"hud-action-unavailable", motivoBloqueio);
			}
		}
		return btn;
	}

	private Button criarBotaoAcao(String texto, String classeVisual) {
		Button btn = new Button(texto);
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.setPrefHeight(60);
		btn.setWrapText(true);
		btn.getStyleClass().addAll("hud-action-button", classeVisual);
		return btn;
	}

	private void marcarIndisponivel(Button botao, String texto, String classe, String motivo) {
		botao.setDisable(true);
		botao.setText(texto);
		botao.getStyleClass().add(classe);
		Tooltip tooltip = new Tooltip(motivo);
		tooltip.setWrapText(true);
		tooltip.setMaxWidth(320);
		tooltip.getStyleClass().add("hud-effect-tooltip");
		botao.setTooltip(tooltip);
	}

	private void adicionarAoGrid(Node node, int col, int row) {
		actionsGrid.add(node, col, row);
	}
}
