package br.com.dantesrpg.controller.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.controller.PlayerCardController;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.util.ImageCache;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.util.Duration;

public class CombatUiRefresher {

	private final CombatController controller;
	private final Supplier<EstadoCombate> estadoSupplier;
	private final Supplier<VBox> playerListSupplier;
	private final Supplier<VBox> enemyListSupplier;
	private final Supplier<Pane> timelineSupplier;

	private Node tuPreviewNode;
	private ParallelTransition activePreviewTransition;
	private final List<TranslateTransition> activeSlideTransitions = new ArrayList<>();
	private static final double SHIFT_AMOUNT = 55.0; // 40px token + 15px spacing
	private static final double PIXELS_PER_TU = 2.0;
	private javafx.animation.Transition activeCountdownTransition;
	private int lastTickCounter = -1;

	public CombatUiRefresher(CombatController controller, Supplier<EstadoCombate> estadoSupplier,
			Supplier<VBox> playerListSupplier, Supplier<VBox> enemyListSupplier, Supplier<Pane> timelineSupplier) {
		this.controller = controller;
		this.estadoSupplier = estadoSupplier;
		this.playerListSupplier = playerListSupplier;
		this.enemyListSupplier = enemyListSupplier;
		this.timelineSupplier = timelineSupplier;
	}

	public void popularListasDeCombatentes() {
		VBox playerListContainer = playerListSupplier.get();
		VBox enemyListContainer = enemyListSupplier.get();
		EstadoCombate estado = estadoSupplier.get();
		if (playerListContainer == null || enemyListContainer == null || estado == null
				|| estado.getCombatentes() == null) {
			return;
		}

		playerListContainer.getChildren().clear();
		enemyListContainer.getChildren().clear();
		controller.forEachMap(m -> m.desenharPeoes(new ArrayList<>()));

		int playerIndex = 0;
		int enemyIndex = 0;
		for (Personagem personagem : estado.getCombatentes()) {
			if (personagem == null || personagem.isAusente()) {
				continue;
			}
			if (!controller.isPlayer(personagem) && !personagem.isAtivoNoCombate()) {
				continue;
			}

			try {
				FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/dantesrpg/view/PlayerCard.fxml"));
				AnchorPane cardNode = loader.load();
				PlayerCardController cardController = loader.getController();
				cardNode.setUserData(cardController);
				cardController.setPersonagem(personagem, controller.isPlayer(personagem) ? "player" : "enemy");

				VBox.setMargin(cardNode, new javafx.geometry.Insets(0, 0, 0, 0));

				if (controller.isPlayer(personagem)) {
					playerListContainer.getChildren().add(cardNode);
					cardNode.setTranslateX(playerIndex % 2 != 0 ? 70 : 10);
					playerIndex++;
				} else {
					enemyListContainer.getChildren().add(cardNode);
					cardNode.setTranslateX(enemyIndex % 2 != 0 ? 60 : 0);
					enemyIndex++;
				}
			} catch (Exception e) {
				System.err.println("Erro ao carregar PlayerCard.fxml para: " + personagem.getNome());
				e.printStackTrace();
			}
		}

		controller.forEachMap(m -> m.desenharPeoes(estado.getCombatentes()));
	}

	private VBox criarTokenVisivel(Personagem personagem, int tuExibido, boolean isPreview) {
		VBox token = new VBox();
		token.setAlignment(Pos.CENTER);
		token.setSpacing(4);
		token.setUserData(personagem);

		// Portrait StackPane
		StackPane portraitContainer = new StackPane();
		portraitContainer.setPrefSize(40, 40);
		portraitContainer.setMinSize(40, 40);
		portraitContainer.setMaxSize(40, 40);

		// ImageView
		ImageView img = new ImageView();
		img.setFitWidth(36);
		img.setFitHeight(36);
		img.setPreserveRatio(true);

		// Load portrait
		String nomeBase = personagem.getNome().toLowerCase().replace(" ", "_");
		if (personagem.isClone()) {
			Personagem criador = personagem.getCriador();
			if (criador != null) {
				nomeBase = criador.getNome().toLowerCase().replace(" ", "_");
			}
		}
		String imagePath = "/portraits/" + nomeBase + ".png";
		Image portraitImage = ImageCache.get(imagePath, 36, 36);

		// Fallback portrait (colored circle with initials)
		if (portraitImage == null || portraitImage.isError()) {
			Circle placeholderCircle = new Circle(18);
			placeholderCircle.setFill(Color.web("#2c2c35"));
			Label initials = new Label(personagem.getNome().substring(0, Math.min(2, personagem.getNome().length())).toUpperCase());
			initials.setStyle("-fx-font-family: 'Oxanium'; -fx-font-size: 11px; -fx-text-fill: #888899; -fx-font-weight: bold;");
			StackPane placeholder = new StackPane(placeholderCircle, initials);
			placeholder.setPrefSize(36, 36);
			portraitContainer.getChildren().add(placeholder);
		} else {
			img.setImage(portraitImage);
			Circle clip = new Circle(18, 18, 18);
			img.setClip(clip);
			portraitContainer.getChildren().add(img);
		}

		// Border Circle
		Circle border = new Circle(18, 18, 18);
		border.setFill(Color.TRANSPARENT);
		border.setStrokeWidth(2.5);

		// Color mapping and glows
		if (isPreview) {
			border.setStroke(Color.web("#00ff88")); // Holographic green
			border.setEffect(new DropShadow(8, Color.web("#00ff88")));
			token.setOpacity(0.7);
		} else {
			boolean isAtorAtual = (estadoSupplier.get() != null && estadoSupplier.get().getAtorAtual() == personagem);
			if (isAtorAtual) {
				border.setStroke(Color.web("#ffd700")); // Active turn is Golden
				border.setEffect(new DropShadow(10, Color.web("#ffd700")));
			} else if (personagem.isClone()) {
				border.setStroke(Color.web("#ee82ee")); // Violet for clones
				border.setEffect(new DropShadow(5, Color.web("#ee82ee")));
			} else if (controller.isPlayer(personagem)) {
				border.setStroke(Color.web("#00f0ff")); // Cyan for players
				border.setEffect(new DropShadow(5, Color.web("#00f0ff")));
			} else {
				border.setStroke(Color.web("#ff3333")); // Red for enemies
				border.setEffect(new DropShadow(5, Color.web("#ff3333")));
			}
		}
		portraitContainer.getChildren().add(border);

		// Balloon with TU
		Label lblTU = new Label(String.valueOf(tuExibido));
		if (isPreview) {
			lblTU.setStyle("-fx-font-family: 'Oxanium'; -fx-font-size: 10px; -fx-text-fill: #00ff88; -fx-font-weight: bold;");
		} else {
			lblTU.setStyle("-fx-font-family: 'Oxanium'; -fx-font-size: 10px; -fx-text-fill: white; -fx-font-weight: bold;");
		}

		StackPane balloon = new StackPane(lblTU);
		if (isPreview) {
			balloon.setStyle("-fx-background-color: rgba(0, 40, 20, 0.7); -fx-border-color: #00ff88; -fx-border-width: 0.5px; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 1px 5px;");
		} else {
			balloon.setStyle("-fx-background-color: rgba(0, 0, 0, 0.65); -fx-background-radius: 4px; -fx-padding: 1px 5px;");
		}

		token.getChildren().addAll(portraitContainer, balloon);

		// Tooltip
		String tooltipText = personagem.getNome();
		if (personagem.isClone()) {
			tooltipText += " (Clone)";
		}
		Tooltip tooltip = new Tooltip(tooltipText);
		tooltip.setStyle("-fx-font-family: 'Oxanium'; -fx-font-size: 12px;");
		Tooltip.install(token, tooltip);

		// Interactive micro-animations
		if (!isPreview) {
			token.setOnMouseEntered(e -> {
				ScaleTransition hoverScale = new ScaleTransition(Duration.millis(150), portraitContainer);
				hoverScale.setToX(1.15);
				hoverScale.setToY(1.15);
				hoverScale.play();
			});
			token.setOnMouseExited(e -> {
				ScaleTransition hoverScale = new ScaleTransition(Duration.millis(150), portraitContainer);
				hoverScale.setToX(1.0);
				hoverScale.setToY(1.0);
				hoverScale.play();
			});
		}

		return token;
	}

	private static class DotPrediction {
		final Personagem alvo;
		final String nomeEfeito;
		final int offsetTU;

		DotPrediction(Personagem alvo, String nomeEfeito, int offsetTU) {
			this.alvo = alvo;
			this.nomeEfeito = nomeEfeito;
			this.offsetTU = offsetTU;
		}
	}

	private List<DotPrediction> calcularPrevisoesDoT(EstadoCombate estado, int maxTU) {
		List<DotPrediction> predictions = new ArrayList<>();
		if (estado == null || estado.getCombatentes() == null) return predictions;
		int currentTick = estado.getTickCounter();

		for (Personagem p : estado.getCombatentes()) {
			if (!p.isAtivoNoCombate()) {
				continue;
			}
			for (Efeito ef : p.getEfeitosAtivos().values()) {
				if (ef.getTipo() == TipoEfeito.DOT && ef.getIntervaloTickTU() > 0) {
					int interval = ef.getIntervaloTickTU();
					int duration = ef.getDuracaoTURestante();
					
					int offset = interval - (currentTick % interval);
					if (offset == 0) {
						offset = interval;
					}
					while (offset <= duration && offset <= maxTU) {
						predictions.add(new DotPrediction(p, ef.getNome(), offset));
						offset += interval;
					}
				}
			}
		}
		return predictions;
	}

	private VBox criarMarcadorDoT(DotPrediction prediction, int offsetExibido) {
		VBox marker = new VBox();
		marker.setAlignment(Pos.TOP_CENTER);
		marker.setSpacing(2);
		marker.setUserData(prediction);

		HBox topBox = new HBox(2);
		topBox.setAlignment(Pos.CENTER);

		ImageView dotImg = new ImageView();
		dotImg.setFitWidth(16);
		dotImg.setFitHeight(16);
		dotImg.setPreserveRatio(true);

		String effectName = prediction.nomeEfeito.toLowerCase().replace(" ", "_");
		if (effectName.equals("queimação")) effectName = "queimacao";
		if (effectName.equals("bênção_do_gekkyūden")) effectName = "bencao_gekkyuden";
		if (effectName.equals("maldição_do_gekkyūden")) effectName = "maldicao_gekkyuden";

		String imagePath = "/effects/dot/" + effectName + ".png";
		Image iconImage = ImageCache.get(imagePath, 16, 16);

		if (iconImage == null || iconImage.isError()) {
			Circle fbCircle = new Circle(8);
			String letter = "?";
			Color fill = Color.PURPLE;
			if (prediction.nomeEfeito.toLowerCase().contains("venen") || prediction.nomeEfeito.toLowerCase().contains("toxin")) {
				fill = Color.web("#2ecc71");
				letter = "V";
			} else if (prediction.nomeEfeito.toLowerCase().contains("sangra") || prediction.nomeEfeito.toLowerCase().contains("hemorr")) {
				fill = Color.web("#e74c3c");
				letter = "S";
			} else if (prediction.nomeEfeito.toLowerCase().contains("queim") || prediction.nomeEfeito.toLowerCase().contains("fire") || prediction.nomeEfeito.toLowerCase().contains("chama")) {
				fill = Color.web("#f39c12");
				letter = "F";
			}
			fbCircle.setFill(fill);
			Label fbLbl = new Label(letter);
			fbLbl.setStyle("-fx-font-family: 'Oxanium'; -fx-font-size: 9px; -fx-text-fill: white; -fx-font-weight: bold;");
			StackPane fbStack = new StackPane(fbCircle, fbLbl);
			fbStack.setPrefSize(16, 16);
			topBox.getChildren().add(fbStack);
		} else {
			dotImg.setImage(iconImage);
			topBox.getChildren().add(dotImg);
		}

		ImageView charImg = new ImageView();
		charImg.setFitWidth(14);
		charImg.setFitHeight(14);
		charImg.setPreserveRatio(true);

		String charNameBase = prediction.alvo.getNome().toLowerCase().replace(" ", "_");
		String charImagePath = "/portraits/" + charNameBase + ".png";
		Image charImage = ImageCache.get(charImagePath, 14, 14);

		if (charImage != null && !charImage.isError()) {
			charImg.setImage(charImage);
			Circle clip = new Circle(7, 7, 7);
			charImg.setClip(clip);
			topBox.getChildren().add(charImg);
		} else {
			Circle fbChar = new Circle(7, Color.web("#444444"));
			Label fbCharLbl = new Label(prediction.alvo.getNome().substring(0, 1).toUpperCase());
			fbCharLbl.setStyle("-fx-font-family: 'Oxanium'; -fx-font-size: 8px; -fx-text-fill: #aaa;");
			StackPane fbCharStack = new StackPane(fbChar, fbCharLbl);
			topBox.getChildren().add(fbCharStack);
		}

		Pane line = new Pane();
		line.setPrefSize(1.5, 12);
		line.setMinSize(1.5, 12);
		line.setMaxSize(1.5, 12);

		String lineStyle = "-fx-background-color: #555555;";
		if (prediction.nomeEfeito.toLowerCase().contains("venen") || prediction.nomeEfeito.toLowerCase().contains("toxin")) {
			lineStyle = "-fx-background-color: #2ecc71;";
		} else if (prediction.nomeEfeito.toLowerCase().contains("sangra") || prediction.nomeEfeito.toLowerCase().contains("hemorr")) {
			lineStyle = "-fx-background-color: #e74c3c;";
		} else if (prediction.nomeEfeito.toLowerCase().contains("queim") || prediction.nomeEfeito.toLowerCase().contains("fire") || prediction.nomeEfeito.toLowerCase().contains("chama")) {
			lineStyle = "-fx-background-color: #f39c12;";
		}
		line.setStyle(lineStyle);

		Label lblOffset = new Label(offsetExibido + " TU");
		lblOffset.setStyle("-fx-font-family: 'Oxanium'; -fx-font-size: 8px; -fx-text-fill: #888888; -fx-font-weight: bold;");

		marker.getChildren().addAll(topBox, line, lblOffset);

		Tooltip tooltip = new Tooltip(prediction.nomeEfeito + " tick em " + prediction.alvo.getNome());
		tooltip.setStyle("-fx-font-family: 'Oxanium'; -fx-font-size: 11px;");
		Tooltip.install(marker, tooltip);

		return marker;
	}

	public void atualizarTimelineTU() {
		Pane timelineContainer = timelineSupplier.get();
		EstadoCombate estado = estadoSupplier.get();
		if (timelineContainer == null || estado == null || estado.getCombatentes() == null) {
			return;
		}

		if (activeCountdownTransition != null) {
			activeCountdownTransition.stop();
			activeCountdownTransition = null;
		}

		limparTUPreview();
		timelineContainer.getChildren().clear();

		int currentTick = estado.getTickCounter();
		final int tempoParaAvancar = (lastTickCounter != -1 && currentTick > lastTickCounter) 
				? (currentTick - lastTickCounter) : 0;
		lastTickCounter = currentTick;

		List<Personagem> ordenadosPorTU = new ArrayList<>(estado.getCombatentes());
		ordenadosPorTU.sort(Comparator.comparingInt(Personagem::getContadorTU)
				.thenComparing((p1, p2) -> Boolean.compare(p2.isProtagonista(), p1.isProtagonista()))
				.thenComparing((p1, p2) -> Integer.compare(p2.getPlacarIniciativa(), p1.getPlacarIniciativa())));
		Set<Personagem> mestresComCloneExibido = new HashSet<>();

		List<Personagem> exibidosNaTimeline = new ArrayList<>();
		int maxTU = 400;

		for (Personagem personagem : ordenadosPorTU) {
			if (!personagem.isAtivoNoCombate()) {
				continue;
			}
			if (personagem.isClone()) {
				Personagem criador = personagem.getCriador();
				if (mestresComCloneExibido.contains(criador)) {
					continue;
				}
				mestresComCloneExibido.add(criador);
			}
			exibidosNaTimeline.add(personagem);
			maxTU = Math.max(maxTU, personagem.getContadorTU());
		}

		int rulerLengthTU = maxTU + 100;
		double rulerWidth = rulerLengthTU * PIXELS_PER_TU;
		timelineContainer.setPrefWidth(rulerWidth);

		// 1. Draw horizontal timeline line
		Line horizontalLine = new Line(0, 30, rulerWidth, 30);
		horizontalLine.setStroke(Color.web("#3a3a3a"));
		horizontalLine.setStrokeWidth(2.0);
		timelineContainer.getChildren().add(horizontalLine);

		// 2. Draw ruler tick marks every 100 TU
		for (int tu = 100; tu <= rulerLengthTU; tu += 100) {
			double x = tu * PIXELS_PER_TU;
			Line tick = new Line(x, 25, x, 35);
			tick.setStroke(Color.web("#555555"));
			tick.setStrokeWidth(1.5);

			Label lblTick = new Label(tu + " TU");
			lblTick.setStyle("-fx-font-family: 'Oxanium'; -fx-font-size: 9px; -fx-text-fill: #555555; -fx-font-weight: bold;");
			lblTick.setLayoutX(x - 15);
			lblTick.setLayoutY(38);

			timelineContainer.getChildren().addAll(tick, lblTick);
		}

		// 3. Draw predicted DoT tick marks
		List<DotPrediction> dotPredictions = calcularPrevisoesDoT(estado, rulerLengthTU);
		for (DotPrediction pred : dotPredictions) {
			int startingOffset = pred.offsetTU + tempoParaAvancar;
			VBox dotMarker = criarMarcadorDoT(pred, startingOffset);
			dotMarker.setLayoutX(startingOffset * PIXELS_PER_TU - 15);
			dotMarker.setLayoutY(2);
			timelineContainer.getChildren().add(dotMarker);
		}

		// 4. Draw character tokens with overlap resolution
		double lastX = -50.0;
		double minGap = 45.0;
		for (Personagem p : exibidosNaTimeline) {
			int startingTU = p.getContadorTU() + tempoParaAvancar;
			VBox token = criarTokenVisivel(p, startingTU, false);
			
			double spatialX = startingTU * PIXELS_PER_TU;
			double layoutX = Math.max(spatialX, lastX + minGap);
			token.setLayoutX(layoutX);
			token.setLayoutY(10);
			
			timelineContainer.getChildren().add(token);
			lastX = layoutX;
		}

		// 5. Animate countdown and glide characters/DoTs to the left
		if (tempoParaAvancar > 0) {
			activeCountdownTransition = new javafx.animation.Transition() {
				{
					setCycleDuration(Duration.millis(800));
				}
				@Override
				protected void interpolate(double frac) {
					double currentTempo = tempoParaAvancar * (1.0 - frac);
					
					// Recalculate layoutX for characters
					double interpLastX = -50.0;
					for (Node node : timelineContainer.getChildren()) {
						if (node instanceof VBox) {
							VBox box = (VBox) node;
							if (box.getUserData() instanceof Personagem) {
								Personagem p = (Personagem) box.getUserData();
								double currentTU = p.getContadorTU() + currentTempo;
								
								if (box.getChildren().size() > 1 && box.getChildren().get(1) instanceof StackPane) {
									StackPane balloon = (StackPane) box.getChildren().get(1);
									if (!balloon.getChildren().isEmpty() && balloon.getChildren().get(0) instanceof Label) {
										Label lbl = (Label) balloon.getChildren().get(0);
										lbl.setText(String.valueOf((int) Math.round(currentTU)));
									}
								}

								double spatialX = currentTU * PIXELS_PER_TU;
								double layoutX = Math.max(spatialX, interpLastX + minGap);
								box.setLayoutX(layoutX);
								interpLastX = layoutX;
							} else if (box.getUserData() instanceof DotPrediction) {
								DotPrediction pred = (DotPrediction) box.getUserData();
								double currentOffset = pred.offsetTU + currentTempo;

								if (box.getChildren().size() > 2 && box.getChildren().get(2) instanceof Label) {
									Label lbl = (Label) box.getChildren().get(2);
									lbl.setText(String.valueOf((int) Math.round(currentOffset)) + " TU");
								}

								box.setLayoutX(currentOffset * PIXELS_PER_TU - 15);
							}
						}
					}
				}
			};

			activeCountdownTransition.setOnFinished(e -> {
				double finishedLastX = -50.0;
				for (Node node : timelineContainer.getChildren()) {
					if (node instanceof VBox) {
						VBox box = (VBox) node;
						if (box.getUserData() instanceof Personagem) {
							Personagem p = (Personagem) box.getUserData();
							if (box.getChildren().size() > 1 && box.getChildren().get(1) instanceof StackPane) {
								StackPane balloon = (StackPane) box.getChildren().get(1);
								if (!balloon.getChildren().isEmpty() && balloon.getChildren().get(0) instanceof Label) {
									Label lbl = (Label) balloon.getChildren().get(0);
									lbl.setText(String.valueOf(p.getContadorTU()));
								}
							}
							double spatialX = p.getContadorTU() * PIXELS_PER_TU;
							double layoutX = Math.max(spatialX, finishedLastX + minGap);
							box.setLayoutX(layoutX);
							finishedLastX = layoutX;
						} else if (box.getUserData() instanceof DotPrediction) {
							DotPrediction pred = (DotPrediction) box.getUserData();
							if (box.getChildren().size() > 2 && box.getChildren().get(2) instanceof Label) {
								Label lbl = (Label) box.getChildren().get(2);
								lbl.setText(pred.offsetTU + " TU");
							}
							box.setLayoutX(pred.offsetTU * PIXELS_PER_TU - 15);
						}
					}
				}
			});
			activeCountdownTransition.play();
		} else {
			double fallbackLastX = -50.0;
			for (Node node : timelineContainer.getChildren()) {
				if (node instanceof VBox && node.getUserData() instanceof Personagem) {
					Personagem p = (Personagem) node.getUserData();
					double spatialX = p.getContadorTU() * PIXELS_PER_TU;
					double layoutX = Math.max(spatialX, fallbackLastX + minGap);
					node.setLayoutX(layoutX);
					fallbackLastX = layoutX;
				}
			}
		}
	}

	public void mostrarTUPreview(Personagem ator, int tuPrevisto) {
		Pane timelineContainer = timelineSupplier.get();
		if (timelineContainer == null) {
			return;
		}

		limparTUPreview();

		tuPreviewNode = criarTokenVisivel(ator, tuPrevisto, true);
		tuPreviewNode.setLayoutX(tuPrevisto * PIXELS_PER_TU - 20);
		tuPreviewNode.setLayoutY(10);

		int insertIndex = 0;
		List<Node> currentTokens = new ArrayList<>();
		for (Node child : timelineContainer.getChildren()) {
			if (child instanceof VBox && child.getUserData() instanceof Personagem) {
				currentTokens.add(child);
			}
		}

		for (int i = 0; i < currentTokens.size(); i++) {
			Node child = currentTokens.get(i);
			Personagem p = (Personagem) child.getUserData();
			if (tuPrevisto > p.getContadorTU()) {
				insertIndex = timelineContainer.getChildren().indexOf(child) + 1;
			}
		}

		timelineContainer.getChildren().add(insertIndex, tuPreviewNode);

		tuPreviewNode.setScaleX(0.0);
		tuPreviewNode.setScaleY(0.0);
		tuPreviewNode.setOpacity(0.0);

		ScaleTransition st = new ScaleTransition(Duration.millis(250), tuPreviewNode);
		st.setToX(1.0);
		st.setToY(1.0);

		FadeTransition ft = new FadeTransition(Duration.millis(250), tuPreviewNode);
		ft.setToValue(0.7);

		activePreviewTransition = new ParallelTransition(st, ft);
		activePreviewTransition.play();

		for (Node child : currentTokens) {
			Personagem p = (Personagem) child.getUserData();
			if (p.getContadorTU() >= tuPrevisto) {
				child.setTranslateX(-SHIFT_AMOUNT);
				TranslateTransition tt = new TranslateTransition(Duration.millis(250), child);
				tt.setToX(0.0);
				activeSlideTransitions.add(tt);
				tt.play();
			}
		}
	}

	public void limparTUPreview() {
		Pane timelineContainer = timelineSupplier.get();
		if (activePreviewTransition != null) {
			activePreviewTransition.stop();
			activePreviewTransition = null;
		}
		for (TranslateTransition tt : activeSlideTransitions) {
			tt.stop();
		}
		activeSlideTransitions.clear();

		if (tuPreviewNode != null && timelineContainer != null) {
			timelineContainer.getChildren().remove(tuPreviewNode);
			tuPreviewNode = null;
		}

		if (timelineContainer != null) {
			for (Node child : timelineContainer.getChildren()) {
				child.setTranslateX(0.0);
			}
		}
	}

	public Personagem getProximoAtorCalculado() {
		EstadoCombate estado = estadoSupplier.get();
		if (estado == null || estado.getCombatentes() == null || estado.getCombatentes().isEmpty()) {
			return null;
		}
		return estado.getCombatentes().stream()
				.filter(Personagem::isAtivoNoCombate)
				.min(Comparator.comparingInt(Personagem::getContadorTU)
						.thenComparing((p1, p2) -> Boolean.compare(p2.isProtagonista(), p1.isProtagonista()))
						.thenComparing((p1, p2) -> Integer.compare(p2.getPlacarIniciativa(), p1.getPlacarIniciativa())))
				.orElse(null);
	}

	public void removerDestaques() {
		aplicarEstiloDestaque(null);
	}

	public void destacarCardAtor(Personagem atorAtual) {
		aplicarEstiloDestaque(atorAtual);
	}

	private void aplicarEstiloDestaque(Personagem atorParaDestacar) {
		List<Node> allCards = new ArrayList<>();
		VBox playerListContainer = playerListSupplier.get();
		VBox enemyListContainer = enemyListSupplier.get();
		if (playerListContainer != null) {
			allCards.addAll(playerListContainer.getChildren());
		}
		if (enemyListContainer != null) {
			allCards.addAll(enemyListContainer.getChildren());
		}
		for (Node node : allCards) {
			if (node != null && node.getUserData() instanceof PlayerCardController) {
				PlayerCardController cardController = (PlayerCardController) node.getUserData();
				if (cardController != null && cardController.getPersonagem() != null) {
					boolean destacar = cardController.getPersonagem() == atorParaDestacar;
					cardController.setHighlight(destacar);
				}
			}
		}
	}
}
