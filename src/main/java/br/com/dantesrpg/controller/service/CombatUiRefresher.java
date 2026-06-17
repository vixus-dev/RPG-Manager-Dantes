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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class CombatUiRefresher {

	private final CombatController controller;
	private final Supplier<EstadoCombate> estadoSupplier;
	private final Supplier<VBox> playerListSupplier;
	private final Supplier<VBox> enemyListSupplier;
	private final Supplier<HBox> timelineSupplier;

	private Node tuPreviewNode;
	private ParallelTransition activePreviewTransition;
	private final List<TranslateTransition> activeSlideTransitions = new ArrayList<>();
	private static final double SHIFT_AMOUNT = 55.0; // 40px token + 15px spacing
	private javafx.animation.Transition activeCountdownTransition;
	private int lastTickCounter = -1;

	public CombatUiRefresher(CombatController controller, Supplier<EstadoCombate> estadoSupplier,
			Supplier<VBox> playerListSupplier, Supplier<VBox> enemyListSupplier, Supplier<HBox> timelineSupplier) {
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

	public void atualizarTimelineTU() {
		HBox timelineContainer = timelineSupplier.get();
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

			int startingTU = personagem.getContadorTU() + tempoParaAvancar;
			VBox token = criarTokenVisivel(personagem, startingTU, false);
			timelineContainer.getChildren().add(token);
		}

		if (tempoParaAvancar > 0) {
			activeCountdownTransition = new javafx.animation.Transition() {
				{
					setCycleDuration(Duration.millis(800));
				}
				@Override
				protected void interpolate(double frac) {
					for (Node node : timelineContainer.getChildren()) {
						if (node instanceof VBox) {
							VBox token = (VBox) node;
							if (token.getUserData() instanceof Personagem) {
								Personagem p = (Personagem) token.getUserData();
								int startTU = p.getContadorTU() + tempoParaAvancar;
								int currentTU = startTU - (int)(frac * tempoParaAvancar);
								if (token.getChildren().size() > 1 && token.getChildren().get(1) instanceof StackPane) {
									StackPane balloon = (StackPane) token.getChildren().get(1);
									if (!balloon.getChildren().isEmpty() && balloon.getChildren().get(0) instanceof Label) {
										Label lbl = (Label) balloon.getChildren().get(0);
										lbl.setText(String.valueOf(currentTU));
									}
								}
							}
						}
					}
				}
			};
			activeCountdownTransition.setOnFinished(e -> {
				for (Node node : timelineContainer.getChildren()) {
					if (node instanceof VBox) {
						VBox token = (VBox) node;
						if (token.getUserData() instanceof Personagem) {
							Personagem p = (Personagem) token.getUserData();
							if (token.getChildren().size() > 1 && token.getChildren().get(1) instanceof StackPane) {
								StackPane balloon = (StackPane) token.getChildren().get(1);
								if (!balloon.getChildren().isEmpty() && balloon.getChildren().get(0) instanceof Label) {
									Label lbl = (Label) balloon.getChildren().get(0);
									lbl.setText(String.valueOf(p.getContadorTU()));
								}
							}
						}
					}
				}
			});
			activeCountdownTransition.play();
		}
	}

	public void mostrarTUPreview(Personagem ator, int tuPrevisto) {
		HBox timelineContainer = timelineSupplier.get();
		if (timelineContainer == null) {
			return;
		}

		limparTUPreview();

		tuPreviewNode = criarTokenVisivel(ator, tuPrevisto, true);

		int insertIndex = 0;
		for (int i = 0; i < timelineContainer.getChildren().size(); i++) {
			Node child = timelineContainer.getChildren().get(i);
			if (child.getUserData() instanceof Personagem) {
				Personagem p = (Personagem) child.getUserData();
				if (tuPrevisto > p.getContadorTU()) {
					insertIndex = i + 1;
				}
			}
		}

		timelineContainer.getChildren().add(insertIndex, tuPreviewNode);

		// Animate the inserted preview token
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

		// Slide subsequent tokens to the right to open space
		for (int i = insertIndex + 1; i < timelineContainer.getChildren().size(); i++) {
			Node child = timelineContainer.getChildren().get(i);
			child.setTranslateX(-SHIFT_AMOUNT);

			TranslateTransition tt = new TranslateTransition(Duration.millis(250), child);
			tt.setToX(0.0);
			activeSlideTransitions.add(tt);
			tt.play();
		}
	}

	public void limparTUPreview() {
		HBox timelineContainer = timelineSupplier.get();
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
