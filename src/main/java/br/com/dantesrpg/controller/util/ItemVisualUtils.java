package br.com.dantesrpg.controller.util;

import br.com.dantesrpg.model.Amuleto;
import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.Armadura;
import br.com.dantesrpg.model.Item;
import br.com.dantesrpg.model.enums.Raridade;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.util.Duration;

public final class ItemVisualUtils {

	private static final String TIMELINE_KEY = ItemVisualUtils.class.getName() + ".shinyTimeline";
	private static final String COR_SHINY = "#ffd700";

	private ItemVisualUtils() {
	}

	public static Raridade obterRaridade(Item item) {
		if (item instanceof Arma) {
			return ((Arma) item).getRaridade();
		} else if (item instanceof Armadura) {
			return ((Armadura) item).getRaridade();
		} else if (item instanceof Amuleto) {
			return ((Amuleto) item).getRaridade();
		}
		return null;
	}

	public static String obterCorRaridade(Item item) {
		Raridade raridade = obterRaridade(item);

		if (raridade == null) return "#ffffff";
		switch (raridade) {
			case COMUM:
				return "#ffffff";
			case INCOMUM:
				return "#2ecc71";
			case RARO:
				return "#3498db";
			case EPICO:
				return "#9b59b6";
			case LENDARIO:
				return "#f1c40f";
			case UNICO:
				return "#00e5ff";
			case MITICO:
				return "#e74c3c";
			default:
				return "#ffffff";
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

	public static void aplicarBordaShinyPulsante(Region card, Item item) {
		if (card == null || item == null || !item.isShiny()) return;
		pararAnimacao(card);

		String bordaSuave = criarEstiloBordaShiny("rgba(255, 215, 0, 0.35)", "rgba(255, 215, 0, 0.15)", 5);
		String bordaIntensa = criarEstiloBordaShiny(COR_SHINY, "rgba(255, 215, 0, 0.9)", 12);
		Timeline timeline = new Timeline(
				new KeyFrame(Duration.ZERO, event -> card.setStyle(bordaSuave)),
				new KeyFrame(Duration.seconds(0.75), event -> card.setStyle(bordaIntensa)),
				new KeyFrame(Duration.seconds(1.5), event -> card.setStyle(bordaSuave)));
		timeline.setCycleCount(Timeline.INDEFINITE);
		card.getProperties().put(TIMELINE_KEY, timeline);
		timeline.play();
	}

	public static void pararAnimacao(Node node) {
		if (node == null) return;
		Object existente = node.getProperties().remove(TIMELINE_KEY);
		if (existente instanceof Timeline) {
			((Timeline) existente).stop();
		}
	}

	private static String criarEstiloEquipado(String cor) {
		return "-fx-text-fill: " + cor + "; -fx-font-weight: bold; -fx-font-size: 12px; "
				+ "-fx-effect: dropshadow(gaussian, " + COR_SHINY + ", 5, 0.6, 0, 0);";
	}

	private static String criarEstiloBordaShiny(String corBorda, String corBrilho, int raioBrilho) {
		return "-fx-border-color: " + corBorda + "; -fx-effect: dropshadow(gaussian, " + corBrilho + ", "
				+ raioBrilho + ", 0.7, 0, 0);";
	}
}
