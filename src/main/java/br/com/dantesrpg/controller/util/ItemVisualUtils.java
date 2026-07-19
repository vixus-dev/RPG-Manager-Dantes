package br.com.dantesrpg.controller.util;

import br.com.dantesrpg.model.Amuleto;
import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.Armadura;
import br.com.dantesrpg.model.Item;
import br.com.dantesrpg.model.enums.Raridade;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.util.Duration;

public final class ItemVisualUtils {

	private static final String TIMELINE_KEY = ItemVisualUtils.class.getName() + ".shinyTimeline";
	private static final String EFEITO_ITEM_TIMELINE_KEY = ItemVisualUtils.class.getName() + ".efeitoItemTimeline";
	private static final String EFEITO_ITEM_CANVAS_KEY = ItemVisualUtils.class.getName() + ".efeitoItemCanvas";
	private static final String COR_SHINY = "#ffd700";
	private static final double INTERVALO_ANIMACAO_MS = 33;
	private static final double EXTRAVASAMENTO_EFEITO = 6;
	private static final double CENTRO_DA_BORDA = 1;

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

	/**
	 * Aplica efeitos animados aos cards da lista de inventário sem interferir na
	 * interação do item. Shiny recebe uma trilha dourada que percorre a borda;
	 * Overclock recebe descargas ciano que atravessam o card.
	 */
	public static void aplicarEfeitosNoCard(Pane card, Item item) {
		if (card == null || item == null || (!item.isShiny() && !item.isOverclockado())) return;
		pararEfeitoItem(card);
		if (item.isShiny()) {
			card.getStyleClass().add("item-shiny-card");
		}
		if (item.isOverclockado()) {
			card.getStyleClass().add("item-overclock-card");
		}

		Canvas camadaEfeito = new Canvas();
		camadaEfeito.setManaged(false);
		camadaEfeito.setMouseTransparent(true);
		camadaEfeito.setViewOrder(-10);
		camadaEfeito.relocate(-EXTRAVASAMENTO_EFEITO, -EXTRAVASAMENTO_EFEITO);
		camadaEfeito.widthProperty().bind(card.widthProperty().add(EXTRAVASAMENTO_EFEITO * 2));
		camadaEfeito.heightProperty().bind(card.heightProperty().add(EXTRAVASAMENTO_EFEITO * 2));
		card.getChildren().add(camadaEfeito);
		camadaEfeito.toFront();

		long inicioAnimacao = System.nanoTime();
		Timeline timeline = new Timeline(new KeyFrame(Duration.ZERO), new KeyFrame(Duration.millis(INTERVALO_ANIMACAO_MS), event -> {
			desenharEfeitosDoItem(camadaEfeito, item, (System.nanoTime() - inicioAnimacao) / 1_000_000.0);
		}));
		timeline.setCycleCount(Timeline.INDEFINITE);
		card.getProperties().put(EFEITO_ITEM_CANVAS_KEY, camadaEfeito);
		card.getProperties().put(EFEITO_ITEM_TIMELINE_KEY, timeline);
		timeline.play();
	}

	public static void pararAnimacao(Node node) {
		if (node == null) return;
		Object existente = node.getProperties().remove(TIMELINE_KEY);
		if (existente instanceof Timeline) {
			((Timeline) existente).stop();
		}
		pararEfeitoItem(node);
	}

	private static void pararEfeitoItem(Node node) {
		Object animacao = node.getProperties().remove(EFEITO_ITEM_TIMELINE_KEY);
		if (animacao instanceof Timeline) {
			((Timeline) animacao).stop();
		}
		Object camada = node.getProperties().remove(EFEITO_ITEM_CANVAS_KEY);
		if (camada instanceof Canvas) {
			Canvas canvas = (Canvas) camada;
			canvas.widthProperty().unbind();
			canvas.heightProperty().unbind();
			if (node instanceof Pane) {
				((Pane) node).getChildren().remove(canvas);
			}
		}
	}

	private static void desenharEfeitosDoItem(Canvas canvas, Item item, double tempoMs) {
		double largura = canvas.getWidth();
		double altura = canvas.getHeight();
		if (largura < 8 || altura < 8) return;

		GraphicsContext contexto = canvas.getGraphicsContext2D();
		contexto.clearRect(0, 0, largura, altura);
		contexto.setLineCap(StrokeLineCap.ROUND);
		contexto.setLineJoin(StrokeLineJoin.ROUND);
		if (item.isShiny()) {
			desenharTrilhaShiny(contexto, largura, altura, tempoMs);
		}
		if (item.isOverclockado()) {
			desenharFagulhasOverclock(contexto, EXTRAVASAMENTO_EFEITO, EXTRAVASAMENTO_EFEITO,
					largura - EXTRAVASAMENTO_EFEITO * 2, altura - EXTRAVASAMENTO_EFEITO * 2, tempoMs, item.getGrauOverclock());
		}
	}

	private static void desenharTrilhaShiny(GraphicsContext contexto, double largura, double altura, double tempoMs) {
		double margem = EXTRAVASAMENTO_EFEITO + CENTRO_DA_BORDA;
		double larguraBorda = largura - margem * 2;
		double alturaBorda = altura - margem * 2;
		double raioDoCanto = Math.min(5, Math.min(larguraBorda, alturaBorda) / 2);
		double perimetro = calcularPerimetroArredondado(larguraBorda, alturaBorda, raioDoCanto);
		if (perimetro <= 0) return;

		double cabeca = (tempoMs * 0.10) % perimetro;
		double comprimentoDaTrilha = perimetro * 0.62;
		for (double posicao = 0; posicao < perimetro; posicao += 2) {
			double distanciaDesdeACabeca = (cabeca - posicao + perimetro) % perimetro;
			double opacidade = 1 - distanciaDesdeACabeca / comprimentoDaTrilha;
			if (opacidade <= 0) continue;

			opacidade = 0.13 + 0.87 * opacidade * opacidade;
			contexto.setStroke(Color.rgb(255, 190, 0, opacidade * 0.30));
			contexto.setLineWidth(distanciaDesdeACabeca < 14 ? 5.4 : 3.1);
			desenharSegmentoDaBorda(contexto, posicao, Math.min(posicao + 2, perimetro), margem, larguraBorda, alturaBorda, raioDoCanto);
			contexto.setStroke(Color.rgb(255, 224, 94, opacidade));
			contexto.setLineWidth(distanciaDesdeACabeca < 14 ? 2.8 : 1.55);
			desenharSegmentoDaBorda(contexto, posicao, Math.min(posicao + 2, perimetro), margem, larguraBorda, alturaBorda, raioDoCanto);
		}

		double[] pontoDaCabeca = obterPontoDaBordaArredondada(cabeca, margem, larguraBorda, alturaBorda, raioDoCanto);
		contexto.setFill(Color.rgb(255, 252, 220, 0.98));
		contexto.fillOval(pontoDaCabeca[0] - 3.4, pontoDaCabeca[1] - 3.4, 6.8, 6.8);
	}

	private static void desenharSegmentoDaBorda(GraphicsContext contexto, double inicio, double fim, double margem, double largura, double altura, double raioDoCanto) {
		double[] pontoInicial = obterPontoDaBordaArredondada(inicio, margem, largura, altura, raioDoCanto);
		double[] pontoFinal = obterPontoDaBordaArredondada(fim, margem, largura, altura, raioDoCanto);
		contexto.strokeLine(pontoInicial[0], pontoInicial[1], pontoFinal[0], pontoFinal[1]);
	}

	private static double calcularPerimetroArredondado(double largura, double altura, double raio) {
		return 2 * (largura + altura - 4 * raio) + 2 * Math.PI * raio;
	}

	private static double[] obterPontoDaBordaArredondada(double posicao, double margem, double largura, double altura, double raio) {
		double perimetro = calcularPerimetroArredondado(largura, altura, raio);
		double restante = (posicao % perimetro + perimetro) % perimetro;
		double trechoHorizontal = largura - 2 * raio;
		double trechoVertical = altura - 2 * raio;
		double arco = Math.PI * raio / 2;

		if (restante <= trechoHorizontal) return new double[] { margem + raio + restante, margem };
		restante -= trechoHorizontal;
		if (restante <= arco) return pontoNoArco(margem + largura - raio, margem + raio, -Math.PI / 2 + restante / raio, raio);
		restante -= arco;
		if (restante <= trechoVertical) return new double[] { margem + largura, margem + raio + restante };
		restante -= trechoVertical;
		if (restante <= arco) return pontoNoArco(margem + largura - raio, margem + altura - raio, restante / raio, raio);
		restante -= arco;
		if (restante <= trechoHorizontal) return new double[] { margem + largura - raio - restante, margem + altura };
		restante -= trechoHorizontal;
		if (restante <= arco) return pontoNoArco(margem + raio, margem + altura - raio, Math.PI / 2 + restante / raio, raio);
		restante -= arco;
		if (restante <= trechoVertical) return new double[] { margem, margem + altura - raio - restante };
		restante -= trechoVertical;
		return pontoNoArco(margem + raio, margem + raio, Math.PI + restante / raio, raio);
	}

	private static double[] pontoNoArco(double centroX, double centroY, double angulo, double raio) {
		return new double[] { centroX + Math.cos(angulo) * raio, centroY + Math.sin(angulo) * raio };
	}

	private static void desenharFagulhasOverclock(GraphicsContext contexto, double origemX, double origemY, double largura, double altura, double tempoMs, int grauOverclock) {
		int grau = Math.max(1, Math.min(Item.OVERCLOCK_MAXIMO, grauOverclock));
		double intensidade = grau / (double) Item.OVERCLOCK_MAXIMO;
		int quantidadeDeFagulhas = 6 + grau * 5;
		for (int indice = 0; indice < quantidadeDeFagulhas; indice++) {
			double periodo = 2450 - grau * 75 + (indice % 5) * 95;
			double fase = ((tempoMs / periodo) + fracao(indice * 0.61803398875)) % 1;
			double entrada = Math.min(1, fase * 8);
			double opacidade = entrada * Math.pow(1 - fase, 1.35);
			if (opacidade < 0.04) continue;

			double xBase = origemX + largura * fracao(indice * 0.7548776662 + 0.13);
			double desvio = Math.sin(fase * 8 + indice * 2.71) * (1.5 + intensidade * 5);
			double x = xBase + desvio;
			double y = origemY + altura * (1 - fase);
			double tamanho = 0.7 + (indice % 6 == 0 ? 1.2 : 0) + intensidade * 0.65;

			contexto.setFill(Color.rgb(0, 175, 255, opacidade * 0.16));
			contexto.fillOval(x - tamanho * 2.8, y - tamanho * 2.8, tamanho * 5.6, tamanho * 5.6);
			contexto.setFill(Color.rgb(104, 234, 255, opacidade * 0.92));
			contexto.fillOval(x - tamanho / 2, y - tamanho / 2, tamanho, tamanho);

			if (indice % 4 == 0) {
				desenharRastroDeFagulha(contexto, x, y, tamanho, desvio, opacidade);
			}
		}
	}

	private static void desenharRastroDeFagulha(GraphicsContext contexto, double x, double y, double tamanho, double desvio, double opacidade) {
		contexto.setStroke(Color.rgb(0, 207, 255, opacidade * 0.7));
		contexto.setLineWidth(Math.max(0.65, tamanho * 0.62));
		contexto.beginPath();
		contexto.moveTo(x - desvio * 0.32, y + tamanho * 4.5);
		contexto.bezierCurveTo(x + tamanho * 1.8, y + tamanho * 2.4, x - tamanho * 1.7, y + tamanho, x, y);
		contexto.stroke();
	}

	private static double fracao(double valor) {
		return valor - Math.floor(valor);
	}

	private static String criarEstiloEquipado(String cor) {
		return "-fx-text-fill: " + cor + "; -fx-font-weight: bold; -fx-font-size: 12px; "
				+ "-fx-effect: dropshadow(gaussian, " + COR_SHINY + ", 5, 0.6, 0, 0);";
	}

}
