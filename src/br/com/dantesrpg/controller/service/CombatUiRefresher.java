package br.com.dantesrpg.controller.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.controller.MapController;
import br.com.dantesrpg.controller.PlayerCardController;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Personagem;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class CombatUiRefresher {

	private final CombatController controller;
	private final Supplier<EstadoCombate> estadoSupplier;
	private final Supplier<VBox> playerListSupplier;
	private final Supplier<VBox> enemyListSupplier;
	private final Supplier<HBox> timelineSupplier;

	private Label tuPreviewLabel;
	private FadeTransition tuPreviewAnimation;

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

	public void atualizarTimelineTU() {
		HBox timelineContainer = timelineSupplier.get();
		EstadoCombate estado = estadoSupplier.get();
		if (timelineContainer == null || estado == null || estado.getCombatentes() == null) {
			return;
		}
		if (tuPreviewAnimation != null) {
			tuPreviewAnimation.stop();
			tuPreviewAnimation = null;
		}
		tuPreviewLabel = null;
		timelineContainer.getChildren().clear();

		List<Personagem> ordenadosPorTU = new ArrayList<>(estado.getCombatentes());
		ordenadosPorTU.sort(Comparator.comparingInt(Personagem::getContadorTU));
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

				Label marcador = new Label("Clones (" + criador.getNome() + ") [" + personagem.getContadorTU() + "]");
				marcador.setStyle("-fx-text-fill: violet; -fx-font-weight: bold;");
				timelineContainer.getChildren().add(marcador);
				continue;
			}

			Label marcador = new Label(personagem.getNome() + " [" + personagem.getContadorTU() + "]");
			marcador.setStyle(controller.isPlayer(personagem)
					? "-fx-text-fill: cyan; -fx-font-weight: bold;"
					: "-fx-text-fill: lightcoral;");
			timelineContainer.getChildren().add(marcador);
		}
	}

	public void mostrarTUPreview(Personagem ator, int tuPrevisto) {
		HBox timelineContainer = timelineSupplier.get();
		if (timelineContainer == null) {
			return;
		}

		limparTUPreview();
		tuPreviewLabel = new Label(ator.getNome() + " [" + tuPrevisto + "]");
		tuPreviewLabel.setStyle(
				"-fx-text-fill: #00ff88; -fx-font-weight: bold; -fx-font-size: 12px; "
						+ "-fx-border-color: #00ff88; -fx-border-width: 1; -fx-border-style: dashed; "
						+ "-fx-padding: 2 6; -fx-border-radius: 3; -fx-background-radius: 3;");

		int insertIndex = 0;
		for (int i = 0; i < timelineContainer.getChildren().size(); i++) {
			Node child = timelineContainer.getChildren().get(i);
			if (child instanceof Label) {
				String text = ((Label) child).getText();
				int bracketStart = text.lastIndexOf('[');
				int bracketEnd = text.lastIndexOf(']');
				if (bracketStart != -1 && bracketEnd != -1) {
					try {
						int tuDoMarcador = Integer.parseInt(text.substring(bracketStart + 1, bracketEnd));
						if (tuPrevisto > tuDoMarcador) {
							insertIndex = i + 1;
						}
					} catch (NumberFormatException ignored) {
					}
				}
			}
		}

		timelineContainer.getChildren().add(insertIndex, tuPreviewLabel);
		tuPreviewAnimation = new FadeTransition(Duration.millis(600), tuPreviewLabel);
		tuPreviewAnimation.setFromValue(1.0);
		tuPreviewAnimation.setToValue(0.3);
		tuPreviewAnimation.setCycleCount(FadeTransition.INDEFINITE);
		tuPreviewAnimation.setAutoReverse(true);
		tuPreviewAnimation.play();
	}

	public void limparTUPreview() {
		HBox timelineContainer = timelineSupplier.get();
		if (tuPreviewAnimation != null) {
			tuPreviewAnimation.stop();
			tuPreviewAnimation = null;
		}
		if (tuPreviewLabel != null && timelineContainer != null) {
			timelineContainer.getChildren().remove(tuPreviewLabel);
			tuPreviewLabel = null;
		}
	}

	public Personagem getProximoAtorCalculado() {
		EstadoCombate estado = estadoSupplier.get();
		if (estado == null || estado.getCombatentes() == null || estado.getCombatentes().isEmpty()) {
			return null;
		}
		return estado.getCombatentes().stream()
				.filter(Personagem::isAtivoNoCombate)
				.min(Comparator.comparingInt(Personagem::getContadorTU))
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
