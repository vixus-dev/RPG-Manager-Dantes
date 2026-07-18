package br.com.dantesrpg.controller;

import br.com.dantesrpg.model.Personagem;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Polygon;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.util.ContratoDeVida;
import br.com.dantesrpg.model.util.ContratoDeVidaUtils;
import br.com.dantesrpg.model.util.EffectTooltipBuilder;
import br.com.dantesrpg.model.util.CharacterImageResolver;
import br.com.dantesrpg.model.util.ImageCache;
import br.com.dantesrpg.model.util.EffectIconResolver;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;

public class PlayerCardController {

	@FXML
	private Polygon diamondShape;
	@FXML
	private ImageView imgPersonagem;
	@FXML
	private Label labelNome;
	@FXML
	private Polygon hpTrack;
	@FXML
	private Polygon hpBarPolygon;
	@FXML
	private Polygon mpTrack;
	@FXML
	private Polygon mpBarPolygon;
	@FXML
	private Label labelMPText;
	@FXML
	private Polygon passiveTrack;
	@FXML
	private Polygon passiveBarPolygon;
	@FXML
	private HBox cardEffectsContainer;
	@FXML
	private VBox expandedEffectsContainer;

	@FXML
	private Label labelHPShieldText;
	@FXML
	private Polygon shieldTrack;
	@FXML
	private Polygon shieldBarPolygon;
	@FXML
	private Polygon bloodShieldBarPolygon;
	@FXML
	private Polygon divineShieldBarPolygon;
	private Polygon infernalShieldBarPolygon;
	@FXML
	private Polygon contractBarPolygon;
	@FXML
	private Polygon curseBarPolygon;
	@FXML
	private Label labelContractCount;
	@FXML
	private Pane hpBarContainer;

	private Personagem personagem;

	/**
	 * Libera recursos visuais que continuam registrados no motor de animação do JavaFX.
	 * Deve ser chamado antes de o card ser removido da cena.
	 */
	public void descartar() {
		// Mantido como ponto de ciclo de vida para os controladores que descartam os cards.
	}

	public void setPersonagem(Personagem personagem, String cardType) {
		this.personagem = personagem;
		if (personagem == null) {
			return;
		}

		labelNome.setText(personagem.getNome());
		configurarImagem(personagem, cardType);

		if (personagem.isProtagonista()) {
			labelHPShieldText.setText("? / ?");
			labelMPText.setText("? / ?");
		} else if (personagem.isPoderoso()) {
			labelHPShieldText.setText("? / ?");
			labelMPText.setText((int) personagem.getManaAtual() + "/" + (int) personagem.getManaMaxima());
		} else {
			String hpAtualTexto = formatarNumero(personagem.getVidaAtual());
			String hpMaxTexto = formatarNumero(personagem.getVidaMaxima());

			double dividaContrato = ContratoDeVidaUtils.getReducaoHpMaximoTotal(personagem);
			double pctMaldicao = br.com.dantesrpg.model.util.MaldicaoUtils.getReducaoPercentualTotal(personagem);

			if (dividaContrato > 0 || pctMaldicao > 0) {
				StringBuilder text = new StringBuilder();
				if (personagem.getEscudoAtual() > 0) {
					text.append(formatarNumero(personagem.getEscudoAtual())).append(" / ");
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
				labelHPShieldText.setText(text.toString());
				labelHPShieldText.setStyle("-fx-text-fill: #ffaaaa; -fx-font-size: 10px; -fx-font-weight: bold;");
			} else if (personagem.getEscudoAtual() > 0) {
				String escudoTexto = formatarNumero(personagem.getEscudoAtual());
				labelHPShieldText.setText(escudoTexto + " / " + hpAtualTexto);
				labelHPShieldText.setStyle(""); // Reset estilo
			} else {
				labelHPShieldText.setText(hpAtualTexto + "/" + hpMaxTexto);
				labelHPShieldText.setStyle("");
			}

			labelMPText.setText((int) personagem.getManaAtual() + "/" + (int) personagem.getManaMaxima());
		}

		updateHpBarShape(); // Lógica principal do Contrato está aqui dentro
		updateMpBarShape();
		updatePassiveBarShape();
		updateShieldBarShape();
		updateEffects();

		// Estilo do Diamante
		if (diamondShape != null) {
			diamondShape.getStyleClass().clear();
			diamondShape.getStyleClass().add("diamond-shape");
			if ("player".equals(cardType))
				diamondShape.getStyleClass().add("diamond-shape-player");
			else
				diamondShape.getStyleClass().add("diamond-shape-enemy");
		}

		// Garante que o retrato, a moldura e os labels fiquem sempre no topo do Z-index (à frente de quaisquer barras de status/escudos)
		if (diamondShape != null) {
			diamondShape.toFront();
		}
		if (imgPersonagem != null && imgPersonagem.getParent() != null) {
			imgPersonagem.getParent().toFront();
		}
		if (labelNome != null) {
			labelNome.toFront();
		}
		if (labelHPShieldText != null) {
			labelHPShieldText.toFront();
		}
		if (labelMPText != null) {
			labelMPText.toFront();
		}
		if (cardEffectsContainer != null) {
			cardEffectsContainer.toFront();
		}

		javafx.application.Platform.runLater(() -> {
			desenharSegmentosVida(personagem.getSegmentosVida());
		});
	}

	private void desenharSegmentosVida(int qtdSegmentos) {

		if (personagem != null && personagem.getNome().equals("Swordsmachine")) {
			System.out.println("DEBUG UI: Swordsmachine tem " + qtdSegmentos + " segmentos de vida. Largura Container: "
					+ hpBarContainer.getPrefWidth());
		}

		// Limpa linhas anteriores
		hpBarContainer.getChildren().clear();

		if (qtdSegmentos <= 1)
			return;

		double larguraTotal = hpBarContainer.getPrefWidth();
		double altura = hpBarContainer.getPrefHeight(); // Deve ser 18.0 conforme o FXML

		// A inclinação baseada no seu Polygon (250 -> 260) é de +10px
		double inclinacao = 10.0;

		double tamanhoSegmento = larguraTotal / (double) qtdSegmentos;

		for (int i = 1; i < qtdSegmentos; i++) {
			// Posição X base (no topo)
			double xTopo = tamanhoSegmento * i;

			// Posição X final (no fundo) = X base + inclinação
			double xFundo = xTopo + inclinacao;

			// Cria a linha diagonal
			javafx.scene.shape.Line linha = new javafx.scene.shape.Line(xTopo, 0, xFundo, altura);

			linha.setStroke(javafx.scene.paint.Color.BLACK);
			linha.setStrokeWidth(2.0);
			linha.setOpacity(0.8);

			hpBarContainer.getChildren().add(linha);
		}
	}

	private void updateHpBarShape() {
		if (personagem == null || hpBarPolygon == null)
			return;

		double vida = personagem.getVidaAtual();
		double vidaMaxBase = personagem.getVidaMaxima();

		double startX = 95.0;
		double topY = 25.0;
		double bottomY = 43.0;
		double fullWidthRect = 155.0; // (250 - 95)
		double fullWidthAngled = 165.0;

		if (curseBarPolygon != null) {
			curseBarPolygon.setVisible(false);
		}
		if (contractBarPolygon != null) {
			contractBarPolygon.setVisible(false);
			if (labelContractCount != null)
				labelContractCount.setVisible(false); // Reset
		}

		double vidaBase = personagem.getVidaMaximaBase();
		if (vidaBase > 0) {
			double pctMaldicao = Math.min(1.0, br.com.dantesrpg.model.util.MaldicaoUtils.getReducaoPercentualTotal(personagem));
			double dividaContrato = ContratoDeVidaUtils.getReducaoHpMaximoTotal(personagem);
			double pctContrato = Math.min(1.0 - pctMaldicao, dividaContrato / vidaBase);

			// 1) Desenha Maldição na extrema direita (de 1.0 - pctMaldicao até 1.0)
			if (pctMaldicao > 0 && curseBarPolygon != null) {
				curseBarPolygon.setVisible(true);
				double pctStart = 1.0 - pctMaldicao;
				double x1 = startX + (fullWidthRect * pctStart);
				double x2 = startX + (fullWidthAngled * pctStart);
				double xEndRect = startX + fullWidthRect;
				double xEndAngled = startX + fullWidthAngled;
				curseBarPolygon.getPoints().setAll(x1, topY, xEndRect, topY, xEndAngled, bottomY, x2, bottomY);

				// Tooltip da Maldição
				StringBuilder info = new StringBuilder();
				info.append("Maldição (Reduz HP Máx): ").append((int) Math.round(pctMaldicao * 100)).append("%\n");
				info.append("Fontes:\n");
				for (br.com.dantesrpg.model.util.Maldicao m : personagem.getMaldicoes()) {
					info.append("  • ").append(m.getFonte()).append(": ").append((int) Math.round(m.getPercentual() * 100))
							.append("% (").append(m.getDuracaoTURestante()).append(" TU)\n");
				}
				Tooltip.install(curseBarPolygon, new Tooltip(info.toString().trim()));

			} else {
				if (curseBarPolygon != null) {
					curseBarPolygon.setOpacity(1.0);
				}
			}

			// 2) Desenha Contrato de Vida à esquerda da Maldição (de 1.0 - pctMaldicao - pctContrato até 1.0 - pctMaldicao)
			if (pctContrato > 0 && contractBarPolygon != null) {
				contractBarPolygon.setVisible(true);
				double pctStart = 1.0 - pctMaldicao - pctContrato;
				double pctEnd = 1.0 - pctMaldicao;
				double x1 = startX + (fullWidthRect * pctStart);
				double x2 = startX + (fullWidthAngled * pctStart);
				double xEndRect = startX + (fullWidthRect * pctEnd);
				double xEndAngled = startX + (fullWidthAngled * pctEnd);
				contractBarPolygon.getPoints().setAll(x1, topY, xEndRect, topY, xEndAngled, bottomY, x2, bottomY);

				// Tooltip do Contrato de Vida
				StringBuilder info = new StringBuilder();
				info.append("HP Máximo Reduzido: -").append((int) dividaContrato).append(" HP Máx\n");
				info.append("Fontes:\n");
				boolean humanoAtivoVisto = false;
				int humanosNaFila = 0;
				for (ContratoDeVida c : personagem.getContratosDeVida()) {
					if (c.isHumano()) {
						if (!humanoAtivoVisto) {
							info.append("  • ").append(c.getFonte()).append(": -").append((int) c.getDividaRestante()).append("\n");
							humanoAtivoVisto = true;
						} else {
							humanosNaFila++;
						}
					} else {
						info.append("  • ").append(c.getFonte()).append(": -").append((int) c.getDividaRestante()).append("\n");
					}
				}
				if (humanosNaFila > 0) {
					info.append("Contratos Humanos na Fila: ").append(humanosNaFila).append("\n");
				}
				if (ContratoDeVidaUtils.estaSobrecarregado(personagem)) {
					info.append("⚠ SOBRECARGA: qualquer dano é letal!\n");
				}
				Tooltip.install(contractBarPolygon, new Tooltip(info.toString().trim()));

				if (labelContractCount != null) {
					if (humanosNaFila > 0) {
						labelContractCount.setText("+" + humanosNaFila);
						labelContractCount.setVisible(true);
					}
				}
			}
		}

		// BARRA DE VIDA ---

		if (vida < 0) {
			// Empréstimo (Dívida de Vida)
			double dividaPercent = Math.min(1.0, Math.abs(vida) / vidaMaxBase);
			double currentEndXRect = Math.max(startX, startX + fullWidthRect * dividaPercent);
			double currentEndXAngled = Math.max(startX, startX + fullWidthAngled * dividaPercent);

			hpBarPolygon.getPoints().setAll(startX, topY, currentEndXRect, topY, currentEndXAngled, bottomY, startX,
					bottomY);
			hpBarPolygon.getStyleClass().clear();
			hpBarPolygon.getStyleClass().add("debt-bar-fill");

		} else {
			// Vida Normal
			double progress = (vidaMaxBase <= 0) ? 0 : Math.max(0, Math.min(1, vida / vidaMaxBase));

			// Se houver contrato, o progress nunca vai chegar a 1.0, vai parar antes do
			// vermelho
			double currentEndXRect = Math.max(startX, startX + fullWidthRect * progress);
			double currentEndXAngled = Math.max(startX, startX + fullWidthAngled * progress);

			if (progress <= 1e-6) {
				hpBarPolygon.getPoints().clear();
			} else {
				hpBarPolygon.getPoints().setAll(startX, topY, currentEndXRect, topY, currentEndXAngled, bottomY, startX,
						bottomY);
			}

			hpBarPolygon.getStyleClass().clear();
			hpBarPolygon.getStyleClass().add(personagem.getValorPropriedade("MALDITO") > 0
					? "hp-bar-fill-cursed"
					: "hp-bar-fill");
		}
	}

	private void updateShieldBarShape() {
		if (personagem == null || shieldBarPolygon == null || shieldTrack == null)
			return;

		double sangueAtual = personagem.getEscudoSangueAtual();
		double sangueMax = personagem.getEscudoSangueMaximo();
		double normalAtual = personagem.getEscudoNormalAtual();
		double normalMax = personagem.getEscudoNormalMaximo();
		double divineAtual = personagem.getEscudoDivinoAtual();
		double divineMax = personagem.getEscudoDivinoMaximo();

		double infernalAtual = personagem.getEscudoInfernalAtual();
		double capTotal = divineMax + sangueMax + normalMax + infernalAtual; // "Limite flexível" combinado

		if (capTotal <= 0) {
			shieldTrack.setVisible(false);
			shieldBarPolygon.setVisible(false);
			shieldBarPolygon.getPoints().clear();
			if (bloodShieldBarPolygon != null) {
				bloodShieldBarPolygon.setVisible(false);
				bloodShieldBarPolygon.getPoints().clear();
			}
			if (divineShieldBarPolygon != null) {
				divineShieldBarPolygon.setVisible(false);
				divineShieldBarPolygon.getPoints().clear();
			}
			if (infernalShieldBarPolygon != null) {
				infernalShieldBarPolygon.setVisible(false);
				infernalShieldBarPolygon.getPoints().clear();
			}
			return;
		}

		shieldTrack.setVisible(true);

		// Geometria do track: inclinação +10px (retângulo 95..250 → ângulo 95..260)
		double startX = 95.0;
		double topY = 25.0;
		double bottomY = 43.0;
		double endXRectBase = 250.0;
		double endXAngledBase = 260.0;
		double maxWidthRect = endXRectBase - startX;
		double maxWidthAngled = endXAngledBase - startX;

		// 0. INFERNAL (Laranja/Fogo)
		if (infernalShieldBarPolygon == null && shieldBarPolygon != null && shieldBarPolygon.getParent() instanceof javafx.scene.layout.Pane) {
			infernalShieldBarPolygon = new Polygon();
			infernalShieldBarPolygon.getStyleClass().add("infernal-shield-bar-fill");
			((javafx.scene.layout.Pane) shieldBarPolygon.getParent()).getChildren().add(infernalShieldBarPolygon);
			infernalShieldBarPolygon.toFront();
		}
		
		if (infernalShieldBarPolygon != null) {
			if (infernalAtual > 0) {
				double progress = infernalAtual / capTotal;
				double endRect = startX + maxWidthRect * progress;
				double endAngled = startX + maxWidthAngled * progress;
				infernalShieldBarPolygon.getPoints().setAll(startX, topY, endRect, topY, endAngled, bottomY, startX, bottomY);
				infernalShieldBarPolygon.setVisible(true);
			} else {
				infernalShieldBarPolygon.setVisible(false);
				infernalShieldBarPolygon.getPoints().clear();
			}
		}

		// 1. DIVINO (cor #cdf8fa) — posicionado após o espaço do infernal
		if (divineShieldBarPolygon != null) {
			if (divineAtual > 0) {
				double offsetPct = (capTotal > 0) ? (infernalAtual / capTotal) : 0.0;
				double widthPct = divineAtual / capTotal;
				double startPct = offsetPct;
				double endPct = offsetPct + widthPct;
				double segStartRect = startX + maxWidthRect * startPct;
				double segStartAngled = startX + maxWidthAngled * startPct;
				double segEndRect = startX + maxWidthRect * endPct;
				double segEndAngled = startX + maxWidthAngled * endPct;
				divineShieldBarPolygon.getPoints().setAll(segStartRect, topY, segEndRect, topY, segEndAngled, bottomY, segStartAngled, bottomY);
				divineShieldBarPolygon.setVisible(true);
			} else {
				divineShieldBarPolygon.setVisible(false);
				divineShieldBarPolygon.getPoints().clear();
			}
		}

		// 2. SANGUE (vermelho) — posicionado após o espaço reservado do infernal + divino
		if (bloodShieldBarPolygon != null) {
			if (sangueAtual > 0) {
				double offsetPct = (capTotal > 0) ? ((infernalAtual + divineMax) / capTotal) : 0.0;
				double widthPct = sangueAtual / capTotal;
				double startPct = offsetPct;
				double endPct = offsetPct + widthPct;
				double segStartRect = startX + maxWidthRect * startPct;
				double segStartAngled = startX + maxWidthAngled * startPct;
				double segEndRect = startX + maxWidthRect * endPct;
				double segEndAngled = startX + maxWidthAngled * endPct;
				bloodShieldBarPolygon.getPoints().setAll(segStartRect, topY, segEndRect, topY, segEndAngled, bottomY, segStartAngled, bottomY);
				bloodShieldBarPolygon.setVisible(true);
			} else {
				bloodShieldBarPolygon.setVisible(false);
				bloodShieldBarPolygon.getPoints().clear();
			}
		}

		// 3. NORMAL (azul) — posicionado após o espaço reservado
		if (normalAtual > 0) {
			double offsetPct = (capTotal > 0) ? ((infernalAtual + divineMax + sangueMax) / capTotal) : 0.0;
			double widthPct = normalAtual / capTotal;
			double startPct = offsetPct;
			double endPct = offsetPct + widthPct;
			double segStartRect = startX + maxWidthRect * startPct;
			double segStartAngled = startX + maxWidthAngled * startPct;
			double segEndRect = startX + maxWidthRect * endPct;
			double segEndAngled = startX + maxWidthAngled * endPct;
			shieldBarPolygon.getPoints().setAll(segStartRect, topY, segEndRect, topY, segEndAngled, bottomY, segStartAngled, bottomY);
			shieldBarPolygon.setVisible(true);
		} else {
			shieldBarPolygon.setVisible(false);
			shieldBarPolygon.getPoints().clear();
		}
	}

	private void updateMpBarShape() {
		if (personagem == null || mpBarPolygon == null)
			return;
		double progress = (personagem.getManaMaxima() <= 0) ? 0
				: Math.max(0, Math.min(1, (double) personagem.getManaAtual() / personagem.getManaMaxima()));
		double startX = 95.0;
		double topY = 47.0;
		double bottomY = 65.0;
		double endXRectBase = 250.0;
		double endXAngledBase = 260.0;
		double maxWidthRect = endXRectBase - startX;
		double maxWidthAngled = endXAngledBase - startX;
		double currentEndXRect = Math.max(startX, startX + maxWidthRect * progress);
		double currentEndXAngled = Math.max(startX, startX + maxWidthAngled * progress);
		if (progress <= 0) {
			mpBarPolygon.getPoints().clear();
		} else {
			mpBarPolygon.getPoints().setAll(startX, topY, currentEndXAngled, topY, currentEndXRect, bottomY, startX,
					bottomY);
		}
	}

	private void updatePassiveBarShape() {
		if (personagem == null || passiveBarPolygon == null || personagem.getRaca() == null) {
			if (passiveTrack != null)
				passiveTrack.setVisible(false);
			if (passiveBarPolygon != null)
				passiveBarPolygon.setVisible(false);
			return;
		}
		int max = personagem.getRaca().getMaxStacks();
		boolean usarBarra = max > 0;
		if (passiveTrack != null)
			passiveTrack.setVisible(usarBarra);
		if (passiveBarPolygon != null)
			passiveBarPolygon.setVisible(usarBarra);

		if (usarBarra) {
			aplicarEstiloBarraRacial();
			int current = personagem.getRaca().getCurrentStacks();
			double progress = (max <= 0) ? 0 : Math.max(0, Math.min(1, (double) current / max));
			double startX = 95.0;
			double topY = 69.0;
			double bottomY = 80.0;
			double endXRectBase = 250.0;
			double endXAngledBase = 240.0;
			double maxWidthRect = endXRectBase - startX;
			double maxWidthAngled = endXAngledBase - startX;
			double currentEndXRect = Math.max(startX, startX + maxWidthRect * progress);
			double currentEndXAngled = Math.max(startX, startX + maxWidthAngled * progress);
			if (progress <= 0) {
				passiveBarPolygon.getPoints().clear();
			} else {
				passiveBarPolygon.getPoints().setAll(startX, topY, currentEndXRect, topY, currentEndXAngled, bottomY,
						startX, bottomY);
			}
		} else {
			if (passiveBarPolygon != null)
				passiveBarPolygon.getPoints().clear();
		}
	}

	private void aplicarEstiloBarraRacial() {
		passiveBarPolygon.getStyleClass().removeAll(
				"race-elf", "race-half-angel", "race-fallen-angel", "race-half-demon", "race-werewolf");

		String nomeDaRaca = personagem.getRaca().getClass().getSimpleName();
		String classeRacial = switch (nomeDaRaca) {
		case "Elfo" -> "race-elf";
		case "HalfAngel" -> "race-half-angel";
		case "AnjoCaido" -> "race-fallen-angel";
		case "HalfDemon" -> "race-half-demon";
		case "Lobisomem" -> "race-werewolf";
		default -> null;
		};

		if (classeRacial != null) {
			passiveBarPolygon.getStyleClass().add(classeRacial);
		}
	}

	public void setHighlight(boolean highlighted) {
		if (diamondShape == null)
			return;
		String highlightClass = "diamond-shape-highlighted";
		diamondShape.getStyleClass().remove(highlightClass); // Remove primeiro
		if (highlighted) {
			diamondShape.getStyleClass().add(highlightClass);
		}
	}

	private void updateEffects() {
		if (cardEffectsContainer == null)
			return;

		cardEffectsContainer.getChildren().clear();
		if (expandedEffectsContainer != null) {
			expandedEffectsContainer.getChildren().clear();
			expandedEffectsContainer.setVisible(false);
			expandedEffectsContainer.setManaged(false);
			expandedEffectsContainer.setOnMouseExited(e -> expandedEffectsContainer.setVisible(false));
		}

		if (personagem.getEfeitosAtivos() == null || personagem.getEfeitosAtivos().isEmpty()) {
			return;
		}

		// Cria uma lista temporária para evitar modificação concorrente se houver
		java.util.List<Efeito> efeitosParaMostrar = new java.util.ArrayList<>(
				personagem.getEfeitosAtivos().values());

		int totalEfeitos = efeitosParaMostrar.size();

		if (totalEfeitos >= 4) {
			// Mostra os 3 primeiros efeitos como ícones
			for (int i = 0; i < 3; i++) {
				Efeito efeito = efeitosParaMostrar.get(i);
				if (efeito != null) {
					cardEffectsContainer.getChildren().add(criarIconeEfeitoComStacks(efeito, 18.0));
				}
			}

			// Cria o quadradinho cinza "..."
			Label moreLabel = new Label("...");
			moreLabel.getStyleClass().clear();
			moreLabel.getStyleClass().add("effect-icon-more");
			Tooltip tipMore = new Tooltip("Clique para ver todos os efeitos ativos");
			tipMore.setStyle("-fx-font-size: 11px; -fx-font-family: 'Consolas'; -fx-background-color: #1a1a2e; -fx-text-fill: #e0e0e0; -fx-border-color: #444; -fx-border-width: 1; -fx-padding: 6;");
			tipMore.setShowDelay(javafx.util.Duration.millis(200));
			Tooltip.install(moreLabel, tipMore);

			moreLabel.setOnMouseClicked(event -> {
				if (expandedEffectsContainer != null) {
					expandedEffectsContainer.setVisible(!expandedEffectsContainer.isVisible());
					if (expandedEffectsContainer.isVisible()) {
						expandedEffectsContainer.toFront();
					}
				}
			});

			cardEffectsContainer.getChildren().add(moreLabel);

			// Popula o container expandido com TODOS os efeitos
			if (expandedEffectsContainer != null) {
				for (Efeito efeito : efeitosParaMostrar) {
					if (efeito != null) {
						expandedEffectsContainer.getChildren().add(criarIconeEfeitoComStacks(efeito, 18.0));
					}
				}
			}

		} else {
			// Mostra todos os efeitos (máximo de 3)
			for (Efeito efeito : efeitosParaMostrar) {
				if (efeito != null) {
					cardEffectsContainer.getChildren().add(criarIconeEfeitoComStacks(efeito, 18.0));
				}
			}
		}

		cardEffectsContainer.toFront();
		if (expandedEffectsContainer != null) {
			expandedEffectsContainer.toFront();
		}
	}

	private Node criarIconeEfeitoComStacks(Efeito efeito, double size) {
		ImageView view = criarIconeEfeito(efeito, size);

		if (efeito.getStacks() > 0) {
			Label stackLabel = new Label(String.valueOf(efeito.getStacks()));
			stackLabel.setStyle("-fx-font-family: 'Oxanium'; -fx-font-size: 8px; -fx-font-weight: bold; "
					+ "-fx-text-fill: white; -fx-background-color: rgba(0,0,0,0.6); -fx-padding: 0 2; "
					+ "-fx-background-radius: 2;");
			StackPane stack = new StackPane(view, stackLabel);
			StackPane.setAlignment(stackLabel, javafx.geometry.Pos.BOTTOM_RIGHT);
			return stack;
		}

		return view;
	}

	private ImageView criarIconeEfeito(Efeito efeito, double size) {
		String imagePath = EffectIconResolver.getIconPath(efeito.getNome(), efeito.getTipo());
		Image img = ImageCache.get(imagePath, size, size);

		ImageView view = new ImageView();
		view.setFitWidth(size);
		view.setFitHeight(size);
		view.setPreserveRatio(true);

		if (img != null && !img.isError()) {
			view.setImage(img);
		}

		// Tooltip
		Tooltip tip = new Tooltip(EffectTooltipBuilder.buildTooltip(efeito));
		tip.setStyle("-fx-font-size: 11px; -fx-font-family: 'Consolas'; -fx-background-color: #1a1a2e; "
				+ "-fx-text-fill: #e0e0e0; -fx-border-color: #444; -fx-border-width: 1; -fx-padding: 6;");
		tip.setShowDelay(javafx.util.Duration.millis(200));
		tip.setMaxWidth(320);
		tip.setWrapText(true);
		Tooltip.install(view, tip);

		return view;
	}

	private void configurarImagem(Personagem personagem, String cardType) {
		if (imgPersonagem == null)
			return;
		try {
			Image portraitImage = CharacterImageResolver.getPortrait(personagem, 120, 120);
			if (portraitImage == null || portraitImage.isError())
				throw new Exception();
			imgPersonagem.setImage(portraitImage);
		} catch (Exception e) {
			imgPersonagem.setImage(null);
		}
	}

	private String formatarNumero(double valor) {
		// Arredonda para 1 casa decimal (Ex: 10.01 -> 10.0, 10.19 -> 10.2)
		double valorArredondado = Math.round(valor * 10.0) / 10.0;

		// Verifica se é inteiro (Ex: 10.0 == 10)
		if (valorArredondado == (long) valorArredondado) {
			return String.format("%d", (long) valorArredondado); // Retorna "10"
		} else {
			return String.format("%s", valorArredondado); // Retorna "10.1" ou "10.2"
		}
	}

	public Personagem getPersonagem() {
		return this.personagem;
	}
}
