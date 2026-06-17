package br.com.dantesrpg.controller.map;

import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.fantasmasnobres.TheMastersCall;
import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.util.ImageCache;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.*;

/**
 * Renderiza peões (tokens), barras de vida, hitboxes e auras no grid do mapa.
 * Extraído de MapController para isolar toda a lógica de renderização de tokens.
 * Detém o estado de mapeamento Node→Personagem e as listas de aura.
 */
public class MapTokenRenderer {

	private final GridPane mapGrid;
	private final CombatController mainController;
	private final Pane[][] celulasDoGrid;
	private final int gridLargura;
	private final int gridAltura;
	private final int CELL_SIZE;

	// Estado próprio (movido de MapController)
	private final Map<Node, Personagem> peaoParaPersonagem = new HashMap<>();
	private final List<Node> peoesAtuais = new ArrayList<>();
	private final List<Personagem> peoesAtualmenteDestacados = new ArrayList<>();
	private final List<Pane> celulasAuraDarrell = new ArrayList<>();
	private final List<Pane> celulasAuraZero = new ArrayList<>();
	private final List<Pane> celulasAuraBadOmen = new ArrayList<>();
	private final List<Pane> celulasAuraSangue = new ArrayList<>();
	private final List<Pane> celulasAuraTheMastersCallEcstasy = new ArrayList<>();
	private final List<Pane> celulasAuraTheMastersCallCrash = new ArrayList<>();
	private static final List<String> CLASSES_AURA_ECSTASY = List.of(
			"zona-aura-master-call-green-10",
			"zona-aura-master-call-green-20",
			"zona-aura-master-call-green-35",
			"zona-aura-master-call-green-50",
			"zona-aura-master-call-green-60",
			"zona-aura-master-call-green-75");

	public MapTokenRenderer(GridPane mapGrid, CombatController mainController,
			Pane[][] celulasDoGrid, int gridLargura, int gridAltura, int cellSize) {
		this.mapGrid = mapGrid;
		this.mainController = mainController;
		this.celulasDoGrid = celulasDoGrid;
		this.gridLargura = gridLargura;
		this.gridAltura = gridAltura;
		this.CELL_SIZE = cellSize;
	}

	// ========== API PÚBLICA ==========

	public Map<Node, Personagem> getPeaoParaPersonagem() {
		return peaoParaPersonagem;
	}

	/** Redesenha todos os peões no grid e atualiza auras e barras de vida de objetos. */
	public void desenharPeoes(List<Personagem> combatentes) {
		// Limpeza
		for (Node peaoNode : peoesAtuais) {
			mapGrid.getChildren().remove(peaoNode);
		}
		peoesAtuais.clear();
		peaoParaPersonagem.clear();

		for (Personagem p : combatentes) {
			if (p != null && p.isAtivoNoCombate()) {
				int larguraTiles = p.getTamanhoX();
				int alturaTiles = p.getTamanhoY();
				int menorDimensao = Math.min(larguraTiles, alturaTiles);
				double margem = 2.0;
				double tamanhoTotalPixels = (menorDimensao * CELL_SIZE) - (margem * 2);
				double raioClip = tamanhoTotalPixels / 2.0;

				Pane peaoContainer = new Pane();
				peaoContainer.setPrefSize(tamanhoTotalPixels, tamanhoTotalPixels);
				peaoContainer.setMinSize(tamanhoTotalPixels, tamanhoTotalPixels);
				peaoContainer.setMaxSize(tamanhoTotalPixels, tamanhoTotalPixels);

				String nomeToken = "";
				String numeroOverlay = "";
				boolean isPlayer = mainController.isPlayer(p);
				if (isPlayer) {
					nomeToken = p.getNome().toLowerCase().replace(" ", "_") + ".png";
					numeroOverlay = "";
				} else {
					String nomeOriginal = p.getNome().toLowerCase();
					if (nomeOriginal.startsWith("servo: "))
						nomeOriginal = nomeOriginal.replace("servo: ", "");
					String nomeLimpo = nomeOriginal.replaceAll("\\s*\\d+$", "");
					nomeToken = nomeLimpo.replace(" ", "_") + ".png";
					if (p.getNome().matches(".*\\d+$")) {
						String[] partes = p.getNome().split(" ");
						numeroOverlay = partes[partes.length - 1];
					} else {
						numeroOverlay = "";
					}
				}

				String imagePath = "/tokens/" + nomeToken;
				try {
					Image tokenImage = ImageCache.get(imagePath, tamanhoTotalPixels, tamanhoTotalPixels);
					if (tokenImage == null || tokenImage.isError()) throw new Exception("Erro imagem");

					javafx.scene.image.ImageView tokenView = new javafx.scene.image.ImageView(tokenImage);
					tokenView.setFitWidth(tamanhoTotalPixels);
					tokenView.setFitHeight(tamanhoTotalPixels);
					tokenView.setPreserveRatio(true);

					Circle clip = new Circle(raioClip);
					clip.setCenterX(tamanhoTotalPixels / 2.0);
					clip.setCenterY(tamanhoTotalPixels / 2.0);
					tokenView.setClip(clip);

					Circle borda = new Circle(raioClip, Color.TRANSPARENT);
					borda.setStrokeWidth(3.0);
					borda.setStroke(isPlayer ? Color.CYAN : Color.RED);
					borda.setCenterX(tamanhoTotalPixels / 2.0);
					borda.setCenterY(tamanhoTotalPixels / 2.0);

					peaoContainer.getChildren().addAll(tokenView, borda);

					if (!numeroOverlay.isEmpty()) {
						Label numeroLabel = new Label(numeroOverlay);
						numeroLabel.setStyle("-fx-font-size: " + (14 + (menorDimensao * 2))
								+ "px; -fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, black, 2, 0.8, 0, 0);");
						peaoContainer.getChildren().add(numeroLabel);
						numeroLabel.layoutXProperty()
								.bind(peaoContainer.widthProperty().subtract(numeroLabel.widthProperty()).subtract(5));
						numeroLabel.layoutYProperty().bind(
								peaoContainer.heightProperty().subtract(numeroLabel.heightProperty()).subtract(2));
					}
				} catch (Exception e) {
					Circle peaoCirculo = new Circle(raioClip);
					peaoCirculo.setStroke(Color.WHITE);
					peaoCirculo.setFill(isPlayer ? Color.CYAN : Color.RED);
					peaoCirculo.setCenterX(tamanhoTotalPixels / 2.0);
					peaoCirculo.setCenterY(tamanhoTotalPixels / 2.0);

					Label nomeLabel = new Label(numeroOverlay.isEmpty() ? p.getNome().substring(0, 1) : numeroOverlay);
					nomeLabel.setStyle("-fx-text-fill: black; -fx-font-weight: bold; -fx-font-size: 20px;");

					peaoContainer.getChildren().addAll(peaoCirculo, nomeLabel);
					nomeLabel.layoutXProperty().bind(
							peaoContainer.widthProperty().divide(2).subtract(nomeLabel.widthProperty().divide(2)));
					nomeLabel.layoutYProperty().bind(
							peaoContainer.heightProperty().divide(2).subtract(nomeLabel.heightProperty().divide(2)));
				}

				peaoContainer.setMouseTransparent(true);
				peaoParaPersonagem.put(peaoContainer, p);

				mapGrid.add(peaoContainer, p.getPosX(), p.getPosY());

				if (menorDimensao > 1) {
					GridPane.setColumnSpan(peaoContainer, menorDimensao);
					GridPane.setRowSpan(peaoContainer, menorDimensao);
				}
				GridPane.setHalignment(peaoContainer, javafx.geometry.HPos.CENTER);
				GridPane.setValignment(peaoContainer, javafx.geometry.VPos.CENTER);

				peoesAtuais.add(peaoContainer);

				// Hitboxes extras (para tokens não-quadrados)
				if (larguraTiles != alturaTiles || larguraTiles > menorDimensao || alturaTiles > menorDimensao) {
					for (int dy = 0; dy < alturaTiles; dy++) {
						for (int dx = 0; dx < larguraTiles; dx++) {
							boolean cobertoPelaImagem = (dx < menorDimensao && dy < menorDimensao);
							if (!cobertoPelaImagem) {
								int tileRealX = p.getPosX() + dx;
								int tileRealY = p.getPosY() + dy;
								if (tileRealX >= 0 && tileRealX < gridLargura && tileRealY >= 0 && tileRealY < gridAltura) {
									Pane cell = celulasDoGrid[tileRealX][tileRealY];
									if (!cell.getStyleClass().contains("enemy-hitbox-extra")) {
										cell.getStyleClass().add("enemy-hitbox-extra");
									}
								}
							}
						}
					}
				}
			}
		}

		// Auras e barras de vida
		for (Personagem p : combatentes) {
			if (p.isAtivoNoCombate() && p.getEfeitosAtivos().containsKey("Modo Justiça")) {
				desenharAuraDarrell(p);
				break;
			}
		}
		boolean ninguemComAura = combatentes.stream().noneMatch(p -> p.getEfeitosAtivos().containsKey("Modo Justiça"));
		if (ninguemComAura && !celulasAuraDarrell.isEmpty()) {
			for (Pane cell : celulasAuraDarrell) cell.getStyleClass().remove("zona-aura-darrell");
			celulasAuraDarrell.clear();
		}

		for (Personagem p : combatentes) {
			if (p.isAtivoNoCombate() && p.getEfeitosAtivos().containsKey("Aura do Zero")) {
				desenharAuraZero(p);
				break;
			}
		}
		boolean ninguemComAuraZero = combatentes.stream().noneMatch(p -> p.getEfeitosAtivos().containsKey("Aura do Zero"));
		if (ninguemComAuraZero && !celulasAuraZero.isEmpty()) {
			for (Pane cell : celulasAuraZero) cell.getStyleClass().remove("zona-aura-zero");
			celulasAuraZero.clear();
		}

		for (Personagem p : combatentes) {
			if (p.isAtivoNoCombate() && p.getEfeitosAtivos().containsKey("Forma de Cobra")
					&& p.getRaca() instanceof br.com.dantesrpg.model.racas.Vampiro
					&& p.getRaca().isV2()) {
				desenharAuraBadOmen(p);
				break;
			}
		}
		boolean ninguemComBadOmen = combatentes.stream().noneMatch(p ->
				p.isAtivoNoCombate() && p.getEfeitosAtivos().containsKey("Forma de Cobra")
				&& p.getRaca() instanceof br.com.dantesrpg.model.racas.Vampiro
				&& p.getRaca().isV2());
		if (ninguemComBadOmen && !celulasAuraBadOmen.isEmpty()) {
			for (Pane cell : celulasAuraBadOmen) cell.getStyleClass().remove("zona-aura-bad-omen");
			celulasAuraBadOmen.clear();
		}

		// --- AURA DE SANGUE (Lillith — Mergulho) ---
		for (Personagem p : combatentes) {
			if (p.isAtivoNoCombate() && p.getEfeitosAtivos().containsKey("Aura de Sangue")) {
				desenharAuraSangue(p);
				break;
			}
		}
		boolean ninguemComAuraSangue = combatentes.stream().noneMatch(
				p -> p.isAtivoNoCombate() && p.getEfeitosAtivos().containsKey("Aura de Sangue"));
		if (ninguemComAuraSangue && !celulasAuraSangue.isEmpty()) {
			for (Pane cell : celulasAuraSangue) cell.getStyleClass().remove("zona-aura-sangue");
			celulasAuraSangue.clear();
		}

		for (Personagem p : combatentes) {
			if (p.isAtivoNoCombate() && p.getEfeitosAtivos().containsKey(TheMastersCall.EFEITO_ECSTASY)) {
				desenharAuraTheMastersCallEcstasy(p);
				break;
			}
		}
		boolean ninguemComEcstasy = combatentes.stream().noneMatch(
				p -> p.isAtivoNoCombate() && p.getEfeitosAtivos().containsKey(TheMastersCall.EFEITO_ECSTASY));
		if (ninguemComEcstasy && !celulasAuraTheMastersCallEcstasy.isEmpty()) {
			limparAuraTheMastersCallEcstasy();
		}

		for (Personagem p : combatentes) {
			if (p.isAtivoNoCombate() && p.getEfeitosAtivos().containsKey(TheMastersCall.EFEITO_CRASH)) {
				desenharAuraTheMastersCallCrash(p);
				break;
			}
		}
		boolean ninguemComCrash = combatentes.stream().noneMatch(
				p -> p.isAtivoNoCombate() && p.getEfeitosAtivos().containsKey(TheMastersCall.EFEITO_CRASH));
		if (ninguemComCrash && !celulasAuraTheMastersCallCrash.isEmpty()) {
			limparAuraTheMastersCallCrash();
		}

		desenharBarrasDeVidaObjetos(combatentes);
	}

	/** Adiciona borda vermelha nos peões dos alvos informados. */
	public void destacarPeoesAlvo(List<Personagem> alvos) {
		for (Personagem p : peoesAtualmenteDestacados) {
			Node peaoNode = getPeaoNode(p);
			if (peaoNode != null) {
				peaoNode.setStyle("");
				Circle borda = (Circle) peaoNode.lookup(".peao-borda-aoe");
				if (borda != null) {
					((Pane) peaoNode).getChildren().remove(borda);
				}
			}
		}
		peoesAtualmenteDestacados.clear();

		for (Personagem alvo : alvos) {
			Node peaoNode = getPeaoNode(alvo);
			if (peaoNode != null) {
				if (peaoNode instanceof Pane) {
					Circle borda = new Circle(CELL_SIZE / 2.0 - 2);
					borda.setStroke(Color.RED);
					borda.setStrokeWidth(3);
					borda.setFill(Color.TRANSPARENT);
					borda.getStyleClass().add("peao-borda-aoe");
					borda.setCenterX(CELL_SIZE / 2.0);
					borda.setCenterY(CELL_SIZE / 2.0);
					((Pane) peaoNode).getChildren().add(borda);
				}
				peoesAtualmenteDestacados.add(alvo);
			} else {
				if (alvo instanceof br.com.dantesrpg.model.elementos.ObjetoDestrutivel) {
					Pane cell = celulasDoGrid[alvo.getPosX()][alvo.getPosY()];
					if (cell != null) {
						cell.setStyle("-fx-border-color: red; -fx-border-width: 3;");
					}
				}
			}
		}
	}

	public void limparDestaquesPeoes() {
		destacarPeoesAlvo(Collections.emptyList());
	}

	public Node getPeaoNode(Personagem p) {
		for (Map.Entry<Node, Personagem> entry : peaoParaPersonagem.entrySet()) {
			if (entry.getValue().equals(p)) return entry.getKey();
		}
		return null;
	}

	// ========== AURAS ==========

	public void desenharAuraDarrell(Personagem centro) {
		for (Pane cell : celulasAuraDarrell) cell.getStyleClass().remove("zona-aura-darrell");
		celulasAuraDarrell.clear();
		int raio = 3;
		for (int y = centro.getPosY() - raio; y <= centro.getPosY() + raio; y++) {
			for (int x = centro.getPosX() - raio; x <= centro.getPosX() + raio; x++) {
				if (x >= 0 && x < gridLargura && y >= 0 && y < gridAltura) {
					Pane cell = celulasDoGrid[x][y];
					if (cell != null) {
						cell.getStyleClass().add("zona-aura-darrell");
						celulasAuraDarrell.add(cell);
					}
				}
			}
		}
	}

	public void desenharAuraZero(Personagem centro) {
		for (Pane cell : celulasAuraZero) cell.getStyleClass().remove("zona-aura-zero");
		celulasAuraZero.clear();
		int raio = 3;
		for (int y = centro.getPosY() - raio; y <= centro.getPosY() + raio; y++) {
			for (int x = centro.getPosX() - raio; x <= centro.getPosX() + raio; x++) {
				if (x >= 0 && x < gridLargura && y >= 0 && y < gridAltura) {
					Pane cell = celulasDoGrid[x][y];
					if (cell != null) {
						cell.getStyleClass().add("zona-aura-zero");
						celulasAuraZero.add(cell);
					}
				}
			}
		}
	}

	public void desenharAuraBadOmen(Personagem centro) {
		for (Pane cell : celulasAuraBadOmen) cell.getStyleClass().remove("zona-aura-bad-omen");
		celulasAuraBadOmen.clear();
		int raio = 2;
		for (int y = centro.getPosY() - raio; y <= centro.getPosY() + raio; y++) {
			for (int x = centro.getPosX() - raio; x <= centro.getPosX() + raio; x++) {
				if (x >= 0 && x < gridLargura && y >= 0 && y < gridAltura) {
					Pane cell = celulasDoGrid[x][y];
					if (cell != null) {
						cell.getStyleClass().add("zona-aura-bad-omen");
						celulasAuraBadOmen.add(cell);
					}
				}
			}
		}
	}

	public void desenharAuraSangue(Personagem centro) {
		for (Pane cell : celulasAuraSangue) cell.getStyleClass().remove("zona-aura-sangue");
		celulasAuraSangue.clear();
		int raio = 3;
		for (int y = centro.getPosY() - raio; y <= centro.getPosY() + raio; y++) {
			for (int x = centro.getPosX() - raio; x <= centro.getPosX() + raio; x++) {
				if (x >= 0 && x < gridLargura && y >= 0 && y < gridAltura) {
					Pane cell = celulasDoGrid[x][y];
					if (cell != null) {
						cell.getStyleClass().add("zona-aura-sangue");
						celulasAuraSangue.add(cell);
					}
				}
			}
		}
	}

	public void desenharAuraTheMastersCallEcstasy(Personagem centro) {
		limparAuraTheMastersCallEcstasy();
		int raio = TheMastersCall.calcularRaio(centro);
		for (int y = centro.getPosY() - raio; y <= centro.getPosY() + raio; y++) {
			for (int x = centro.getPosX() - raio; x <= centro.getPosX() + raio; x++) {
				if (x >= 0 && x < gridLargura && y >= 0 && y < gridAltura) {
					Pane cell = celulasDoGrid[x][y];
					if (cell != null) {
						int distancia = Math.max(Math.abs(x - centro.getPosX()), Math.abs(y - centro.getPosY()));
						String classe = resolverClasseEcstasy(distancia);
						cell.getStyleClass().add(classe);
						celulasAuraTheMastersCallEcstasy.add(cell);
					}
				}
			}
		}
	}

	public void desenharAuraTheMastersCallCrash(Personagem centro) {
		limparAuraTheMastersCallCrash();
		int raio = TheMastersCall.calcularRaio(centro);
		for (int y = centro.getPosY() - raio; y <= centro.getPosY() + raio; y++) {
			for (int x = centro.getPosX() - raio; x <= centro.getPosX() + raio; x++) {
				if (x >= 0 && x < gridLargura && y >= 0 && y < gridAltura) {
					Pane cell = celulasDoGrid[x][y];
					if (cell != null) {
						cell.getStyleClass().add("zona-aura-master-call-crash");
						celulasAuraTheMastersCallCrash.add(cell);
					}
				}
			}
		}
	}

	private void limparAuraTheMastersCallEcstasy() {
		for (Pane cell : celulasAuraTheMastersCallEcstasy) {
			cell.getStyleClass().removeAll(CLASSES_AURA_ECSTASY);
		}
		celulasAuraTheMastersCallEcstasy.clear();
	}

	private void limparAuraTheMastersCallCrash() {
		for (Pane cell : celulasAuraTheMastersCallCrash) {
			cell.getStyleClass().remove("zona-aura-master-call-crash");
		}
		celulasAuraTheMastersCallCrash.clear();
	}

	private String resolverClasseEcstasy(int distancia) {
		if (distancia <= 2) {
			return "zona-aura-master-call-green-75";
		}
		if (distancia == 3) {
			return "zona-aura-master-call-green-60";
		}
		if (distancia <= 5) {
			return "zona-aura-master-call-green-50";
		}
		if (distancia <= 7) {
			return "zona-aura-master-call-green-35";
		}
		if (distancia <= 9) {
			return "zona-aura-master-call-green-20";
		}
		return "zona-aura-master-call-green-10";
	}

	// ========== BARRAS DE VIDA ==========

	private void desenharBarrasDeVidaObjetos(List<Personagem> combatentes) {
		for (int x = 0; x < gridLargura; x++) {
			for (int y = 0; y < gridAltura; y++) {
				Pane cell = celulasDoGrid[x][y];
				if (cell != null) {
					cell.getChildren().removeIf(node -> node.getStyleClass().contains("obj-hp-bar"));
				}
			}
		}
		for (Personagem p : combatentes) {
			if (p instanceof br.com.dantesrpg.model.elementos.ObjetoDestrutivel) {
				if (p.getVidaAtual() < p.getVidaMaxima() && p.isVivo()) {
					Pane cell = celulasDoGrid[p.getPosX()][p.getPosY()];
					if (cell != null) {
						double pct = (double) p.getVidaAtual() / (double) p.getVidaMaxima();

						javafx.scene.shape.Rectangle bgBar = new javafx.scene.shape.Rectangle(5, 5, CELL_SIZE - 10, 6);
						bgBar.setFill(Color.DARKRED);
						bgBar.getStyleClass().add("obj-hp-bar");

						javafx.scene.shape.Rectangle hpBar = new javafx.scene.shape.Rectangle(5, 5,
								(CELL_SIZE - 10) * pct, 6);
						if (pct > 0.5) hpBar.setFill(Color.LIME);
						else if (pct > 0.25) hpBar.setFill(Color.ORANGE);
						else hpBar.setFill(Color.RED);
						hpBar.getStyleClass().add("obj-hp-bar");

						cell.getChildren().addAll(bgBar, hpBar);
					}
				}
			}
		}
	}

	// ========== LIMPEZA ==========

	public void limparHitboxesExtras() {
		for (int x = 0; x < gridLargura; x++) {
			for (int y = 0; y < gridAltura; y++) {
				if (celulasDoGrid[x][y] != null) {
					celulasDoGrid[x][y].getStyleClass().remove("enemy-hitbox-extra");
				}
			}
		}
	}
}
