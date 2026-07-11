package br.com.dantesrpg.controller.util;

import br.com.dantesrpg.model.Amuleto;
import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.Armadura;
import br.com.dantesrpg.model.Item;
import br.com.dantesrpg.model.enums.Raridade;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.util.Duration;

public final class ItemVisualUtils {

	private static final String TIMELINE_KEY = ItemVisualUtils.class.getName() + ".shinyTimeline";
	private static final String COR_SHINY = "#ffd700";

	private ItemVisualUtils() {
	}

	public static String obterCorRaridade(Item item) {
		Raridade raridade = null;
		if (item instanceof Arma) {
			raridade = ((Arma) item).getRaridade();
		} else if (item instanceof Armadura) {
			raridade = ((Armadura) item).getRaridade();
		} else if (item instanceof Amuleto) {
			raridade = ((Amuleto) item).getRaridade();
		}

		if (raridade == null) return "#c0c0c0";
		switch (raridade) {
			case COMUM:
				return "#c0c0c0";
			case INCOMUM:
				return "#2ecc71";
			case RARO:
				return "#3498db";
			case EPICO:
				return "#9b59b6";
			case LENDARIO:
				return "#f39c12";
			case UNICO:
				return "#e74c3c";
			case MITICO:
				return "#ff00ff";
			default:
				return "#c0c0c0";
		}
	}

	public static void aplicarBrilhoNoInventario(Label label, Item item) {
		if (label == null || item == null || !item.isShiny()) return;
		pararAnimacao(label);
		String corRaridade = obterCorRaridade(item);
		label.setStyle("-fx-text-fill: " + corRaridade + "; -fx-font-size: 12px; "
				+ "-fx-effect: dropshadow(gaussian, " + COR_SHINY + ", 7, 0.65, 0, 0);");
	}

	public static void aplicarPulsacaoEquipado(Label label, Item item) {
		if (label == null || item == null || !item.isShiny()) return;
		pararAnimacao(label);

		String corRaridade = obterCorRaridade(item);
		String estiloRaridade = criarEstiloEquipado(corRaridade);
		String estiloShiny = criarEstiloEquipado(COR_SHINY);
		Timeline timeline = new Timeline(
				new KeyFrame(Duration.ZERO, event -> label.setStyle(estiloRaridade)),
				new KeyFrame(Duration.seconds(0.75), event -> label.setStyle(estiloShiny)),
				new KeyFrame(Duration.seconds(1.5), event -> label.setStyle(estiloRaridade)));
		timeline.setCycleCount(Timeline.INDEFINITE);
		label.getProperties().put(TIMELINE_KEY, timeline);
		timeline.play();
	}

	private static String criarEstiloEquipado(String cor) {
		return "-fx-text-fill: " + cor + "; -fx-font-weight: bold; -fx-font-size: 12px; "
				+ "-fx-effect: dropshadow(gaussian, " + COR_SHINY + ", 5, 0.6, 0, 0);";
	}

	private static void pararAnimacao(Label label) {
		Object existente = label.getProperties().remove(TIMELINE_KEY);
		if (existente instanceof Timeline) {
			((Timeline) existente).stop();
		}
	}
}
