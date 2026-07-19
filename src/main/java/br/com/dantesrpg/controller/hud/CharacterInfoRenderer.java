package br.com.dantesrpg.controller.hud;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.util.DiceRoller;
import br.com.dantesrpg.model.util.EffectTooltipBuilder;
import br.com.dantesrpg.model.util.EffectIconResolver;
import br.com.dantesrpg.model.util.ImageCache;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Renderiza o painel de informações do personagem no HUD de turno detalhado.
 * Extraído de DetailedTurnHUDController para isolamento de responsabilidade.
 * Cobre: info básica (nome/HP/MP/TU), efeitos ativos e atributos detalhados.
 */
public class CharacterInfoRenderer {

	private final Label labelNome;
	private final Label labelClasseRaca;
	private final Label labelHP;
	private final Label labelMP;
	private final Label labelTU;
	private final VBox effectsContainer;
	private final ScrollPane detailedScrollPane;
	private final VBox detailedPane;
	private GridPane attributesGrid;
	private final Map<String, BadgeEfeito> badgesPorEfeito = new HashMap<>();

	private static final class BadgeEfeito {
		private final Label label;
		private final Tooltip tooltip;

		private BadgeEfeito(Label label, Tooltip tooltip) {
			this.label = label;
			this.tooltip = tooltip;
		}
	}

	public CharacterInfoRenderer(Label labelNome, Label labelClasseRaca,
			Label labelHP, Label labelMP, Label labelTU,
			VBox effectsContainer, ScrollPane detailedScrollPane,
			VBox detailedPane, GridPane attributesGrid) {
		this.labelNome = labelNome;
		this.labelClasseRaca = labelClasseRaca;
		this.labelHP = labelHP;
		this.labelMP = labelMP;
		this.labelTU = labelTU;
		this.effectsContainer = effectsContainer;
		this.detailedScrollPane = detailedScrollPane;
		this.detailedPane = detailedPane;
		this.attributesGrid = attributesGrid;
	}

	// ========== API PÚBLICA ==========

	/** Atualiza nome, classe/raça, HP, MP, TU e efeitos ativos. */
	public void renderInfo(Personagem ator) {
		labelNome.setText(ator.getNome());

		String raca = "N/A";
		boolean racaV2 = false;
		if (ator.getRaca() != null) {
			racaV2 = ator.getRaca().isV2();
			raca = (racaV2 && ator.getRaca().getNomeV2() != null)
					? ator.getRaca().getNomeV2() : ator.getRaca().getNome();
		}
		String classe = (ator.getClasse() != null) ? ator.getClasse().getNome() : "N/A";
		labelClasseRaca.setText(raca + " / " + classe);
		labelClasseRaca.setStyle(racaV2 ? "-fx-text-fill: #FFD700;" : "-fx-text-fill: gray;");

		if (ator.isProtagonista()) {
			labelHP.setText("?/?");
			labelHP.setStyle("");
			labelMP.setText("?/?");
		} else if (ator.isPoderoso()) {
			labelHP.setText("?/?");
			labelHP.setStyle("");
			labelMP.setText((int) ator.getManaAtual() + "/" + (int) ator.getManaMaxima());
		} else {
			double dividaContrato = br.com.dantesrpg.model.util.ContratoDeVidaUtils.getReducaoHpMaximoTotal(ator);
			double pctMaldicao = br.com.dantesrpg.model.util.MaldicaoUtils.getReducaoPercentualTotal(ator);
			String hpAtualTexto = formatarNumero(ator.getVidaAtual());
			String hpMaxTexto = formatarNumero(ator.getVidaMaxima());

			if (dividaContrato > 0 || pctMaldicao > 0) {
				StringBuilder text = new StringBuilder();
				if (ator.getEscudoAtual() > 0) {
					text.append(formatarNumero(ator.getEscudoAtual())).append(" / ");
				}
				text.append(hpAtualTexto).append("/").append(hpMaxTexto).append(" (");
				if (dividaContrato > 0) {
					text.append("-").append((int) dividaContrato);
				}
				if (pctMaldicao > 0) {
					if (dividaContrato > 0) {
						text.append(" ");
					}
					text.append("-").append((int) Math.round(pctMaldicao * 100)).append("%");
				}
				text.append(")");
				labelHP.setText(text.toString());
				labelHP.setStyle("-fx-text-fill: #ffaaaa; -fx-font-weight: bold;");
			} else if (ator.getEscudoAtual() > 0) {
				String escudoTexto = formatarNumero(ator.getEscudoAtual());
				labelHP.setText(escudoTexto + " / " + hpAtualTexto);
				labelHP.setStyle("");
			} else {
				labelHP.setText(hpAtualTexto + "/" + hpMaxTexto);
				labelHP.setStyle("");
			}
			labelMP.setText((int) ator.getManaAtual() + "/" + (int) ator.getManaMaxima());
		}
		
		labelTU.setText("TU: " + ator.getContadorTU());

		renderEffects(ator);
	}

	/** Atualiza apenas os badges de efeitos ativos. */
	public void renderEffects(Personagem ator) {
		if (effectsContainer == null) return;
		List<BadgeEfeito> badgesOrdenados = new ArrayList<>();
		Set<String> nomesAtuais = new HashSet<>();
		if (ator.getEfeitosAtivos() != null) {
			for (Efeito efeito : ator.getEfeitosAtivos().values()) {
				if (efeito == null) continue;
				nomesAtuais.add(efeito.getNome());
				BadgeEfeito badge = badgesPorEfeito.computeIfAbsent(efeito.getNome(), chave -> criarBadgeEfeito());
				atualizarBadgeEfeito(badge, efeito);
				badgesOrdenados.add(badge);
			}
		}

		badgesPorEfeito.entrySet().removeIf(entry -> {
			if (nomesAtuais.contains(entry.getKey())) return false;
			Tooltip.uninstall(entry.getValue().label, entry.getValue().tooltip);
			effectsContainer.getChildren().remove(entry.getValue().label);
			return true;
		});
		sincronizarOrdemDosBadges(badgesOrdenados);
	}

	private BadgeEfeito criarBadgeEfeito() {
		Label label = new Label();
		label.setMaxWidth(Double.MAX_VALUE);
		Tooltip tooltip = new Tooltip();
		tooltip.setStyle("-fx-font-size: 12px; -fx-font-family: 'Consolas'; -fx-background-color: #1a1a2e; "
				+ "-fx-text-fill: #e0e0e0; -fx-border-color: #444; -fx-border-width: 1; -fx-padding: 8;");
		tooltip.setShowDelay(javafx.util.Duration.millis(200));
		tooltip.setMaxWidth(350);
		tooltip.setWrapText(true);
		Tooltip.install(label, tooltip);
		return new BadgeEfeito(label, tooltip);
	}

	private void atualizarBadgeEfeito(BadgeEfeito badge, Efeito efeito) {
		String texto = efeito.getNome();
		if (efeito.getStacks() > 0) texto += " (" + efeito.getStacks() + ")";
		badge.label.setText(texto + " [" + efeito.getDuracaoTURestante() + "]");
		badge.label.setStyle(resolverEstiloEfeito(efeito.getTipo()));
		badge.tooltip.setText(EffectTooltipBuilder.buildTooltip(efeito));

		String path = EffectIconResolver.getIconPath(efeito.getNome(), efeito.getTipo());
		Image image = ImageCache.get(path, 14, 14);
		if (image == null || image.isError()) {
			badge.label.setGraphic(null);
			return;
		}
		ImageView view = new ImageView(image);
		view.setFitWidth(14);
		view.setFitHeight(14);
		view.setPreserveRatio(true);
		badge.label.setGraphic(view);
		badge.label.setGraphicTextGap(5);
	}

	private void sincronizarOrdemDosBadges(List<BadgeEfeito> badgesOrdenados) {
		for (int indice = 0; indice < badgesOrdenados.size(); indice++) {
			Label label = badgesOrdenados.get(indice).label;
			int indiceAtual = effectsContainer.getChildren().indexOf(label);
			if (indiceAtual == indice) continue;
			if (indiceAtual >= 0) effectsContainer.getChildren().remove(indiceAtual);
			effectsContainer.getChildren().add(indice, label);
		}
	}

	/** Popula o painel de atributos detalhados (grid S.P.E.C.I.A.L.I.S.T + stats derivados). */
	public void renderDetailedAttributes(Personagem ator) {
		if (detailedPane == null) return;
		garantirAttributesGrid();

		attributesGrid.getChildren().clear();
		attributesGrid.getRowConstraints().clear();

		// Remove labels antigos mas preserva grid, título e separadores
		detailedPane.getChildren().removeIf(node ->
				node != attributesGrid
				&& !(node instanceof Label && ((Label) node).getText().startsWith("Atributos"))
				&& !(node instanceof Separator));

		// Popula atributos base
		Atributo[] ordem = {
			Atributo.FORCA, Atributo.PERCEPCAO, Atributo.ENDURANCE, Atributo.CARISMA,
			Atributo.INTELIGENCIA, Atributo.DESTREZA, Atributo.SORTE, Atributo.INSPIRACAO,
			Atributo.SAGACIDADE, Atributo.TOPOR
		};

		int row = 0;
		for (Atributo atr : ordem) {
			RowConstraints rc = new RowConstraints();
			rc.setMinHeight(25);
			rc.setPrefHeight(25);
			attributesGrid.getRowConstraints().add(rc);

			int valor = ator.getAtributosFinais().getOrDefault(atr, 1);
			int dado = DiceRoller.getTipoDado(valor);

			Label lblNome = new Label(atr.name().substring(0, 3));
			lblNome.setStyle("-fx-text-fill: #aaaaaa; -fx-font-weight: bold; -fx-font-size: 12px;");

			Label lblValor = new Label(valor + " (d" + dado + ")");
			lblValor.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");

			attributesGrid.add(lblNome, 0, row);
			attributesGrid.add(lblValor, 1, row);
			row++;
		}

		// Separador antes dos stats derivados
		boolean temSeparador = detailedPane.getChildren().stream().anyMatch(n -> n instanceof Separator);
		if (!temSeparador) detailedPane.getChildren().add(new Separator());

		// Stats derivados
		addStatLabel("Movimento:",    ator.getMovimento() + " células");
		addStatLabel("Armadura:",     String.valueOf(ator.getArmaduraTotal()));
		addStatLabel("Red. Dano:",    String.format("%.1f%%", ator.getReducaoDanoArmadura() * 100));
		addStatLabel("Taxa Crítica:", String.format("%.1f%%", ator.getTaxaCritica() * 100));
		addStatLabel("Dano Crítico:", String.format("+%.1f%%", ator.getDanoCritico() * 100));
		addStatLabel("Bônus Dano:",   String.format("+%.1f%%", ator.getBonusDanoPercentual() * 100));
		adicionarPecado(ator.getPecado());
		detailedPane.getChildren().add(VidasPortraitRenderer.criar(ator.getVidas()));

		detailedPane.requestLayout();
	}

	// ========== PRIVADOS ==========

	private void garantirAttributesGrid() {
		if (attributesGrid == null) {
			System.out.println("AVISO: attributesGrid era null. Recriando manualmente.");
			attributesGrid = new GridPane();
			attributesGrid.setHgap(10);
			attributesGrid.setVgap(5);
			ColumnConstraints c1 = new ColumnConstraints();
			c1.setPercentWidth(40);
			ColumnConstraints c2 = new ColumnConstraints();
			c2.setPercentWidth(60);
			attributesGrid.getColumnConstraints().addAll(c1, c2);
			detailedPane.getChildren().add(1, attributesGrid);
		}
	}

	private void addStatLabel(String titulo, String valor) {
		HBox row = new HBox(10);
		Label t = new Label(titulo);
		t.setStyle("-fx-text-fill: cyan;");
		Label v = new Label(valor);
		v.setStyle("-fx-text-fill: white;");
		row.getChildren().addAll(t, v);
		detailedPane.getChildren().add(row);
	}

	private void adicionarPecado(int pecado) {
		HBox row = new HBox(10);
		Label titulo = new Label("Pecado:");
		titulo.setStyle("-fx-text-fill: cyan;");
		Label valor = new Label(String.valueOf(pecado));
		valor.setStyle("-fx-text-fill: #b56cff; -fx-font-weight: bold;");
		row.getChildren().addAll(titulo, valor);
		detailedPane.getChildren().add(row);
	}

	private String resolverEstiloEfeito(TipoEfeito tipo) {
		switch (tipo) {
			case BUFF:
				return "-fx-background-color: #004466; -fx-text-fill: cyan; -fx-padding: 3; -fx-background-radius: 3;";
			case DEBUFF:
				return "-fx-background-color: #660000; -fx-text-fill: #ffaaaa; -fx-padding: 3; -fx-background-radius: 3;";
			default:
				return "-fx-background-color: #440044; -fx-text-fill: violet; -fx-padding: 3; -fx-background-radius: 3;";
		}
	}

	private String formatarNumero(double valor) {
		if (valor >= 1000) return String.format("%.1fk", valor / 1000.0).replace(",", ".");
		return String.format("%.0f", valor);
	}
}
