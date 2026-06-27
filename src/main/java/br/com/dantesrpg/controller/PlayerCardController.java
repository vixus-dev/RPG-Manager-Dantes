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
import br.com.dantesrpg.model.util.ImageCache;
import javafx.scene.image.Image;

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
	private Label labelContractCount;
	@FXML
	private Pane hpBarContainer;

	private Personagem personagem;

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

			// Redução total do teto (Contrato de Vida + Escudo Infernal)
			double dividaContrato = ContratoDeVidaUtils.getReducaoHpMaximoTotal(personagem);
			double reducaoInfernal = personagem.getEscudoInfernalAtual();
			double reducaoTotal = dividaContrato + reducaoInfernal;

			if (reducaoTotal > 0) {
				if (personagem.getEscudoAtual() > 0) {
					String escudoTexto = formatarNumero(personagem.getEscudoAtual());
					labelHPShieldText.setText(escudoTexto + " / " + hpAtualTexto + "/" + hpMaxTexto + " (-" + (int) reducaoTotal + ")");
				} else {
					labelHPShieldText.setText(hpAtualTexto + "/" + hpMaxTexto + " (-" + (int) reducaoTotal + ")");
				}
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

		if (contractBarPolygon != null) {
			contractBarPolygon.setVisible(false);
			if (labelContractCount != null)
				labelContractCount.setVisible(false); // Reset

			// Barra vermelha unificada: bloqueia HP máximo perdido (Contratos + Escudo Infernal)
			double dividaContrato = ContratoDeVidaUtils.getReducaoHpMaximoTotal(personagem);
			double reducaoInfernal = personagem.getEscudoInfernalAtual();
			double reducaoTotal = dividaContrato + reducaoInfernal;
			double vidaBase = personagem.getVidaMaximaBase();
			if (reducaoTotal > 0 && vidaBase > 0) {
				contractBarPolygon.setVisible(true);

				// Porcentagem bloqueada (em relação ao HP base).
				double pctBlocked = Math.min(1.0, reducaoTotal / vidaBase);
				double pctStart = 1.0 - pctBlocked; // Desenha da direita para esquerda

				double x1 = startX + (fullWidthRect * pctStart);
				double x2 = startX + (fullWidthAngled * pctStart);
				double xEndRect = startX + fullWidthRect;
				double xEndAngled = startX + fullWidthAngled;

				contractBarPolygon.getPoints().setAll(x1, topY, xEndRect, topY, xEndAngled, bottomY, x2, bottomY);

				// Tooltip com detalhamento por fonte
				StringBuilder info = new StringBuilder();
				info.append("HP Máximo Reduzido: -").append((int) reducaoTotal).append(" HP Máx\n");
				info.append("Fontes:\n");
				
				if (dividaContrato > 0) {
					boolean humanoAtivoVisto = false;
					int humanosNaFila = 0;
					for (ContratoDeVida c : personagem.getContratosDeVida()) {
						if (c.isHumano()) {
							if (!humanoAtivoVisto) {
								info.append("  • ").append(c.getFonte()).append(": -")
										.append((int) c.getDividaRestante()).append("\n");
								humanoAtivoVisto = true;
							} else {
								humanosNaFila++;
							}
						} else {
							info.append("  • ").append(c.getFonte()).append(": -")
									.append((int) c.getDividaRestante()).append("\n");
						}
					}
					if (humanosNaFila > 0) {
						info.append("Contratos Humanos na Fila: ").append(humanosNaFila).append("\n");
					}
				}
				if (reducaoInfernal > 0) {
					info.append("  • Escudo Infernal: -").append((int) reducaoInfernal).append("\n");
				}
				if (ContratoDeVidaUtils.estaSobrecarregado(personagem)) {
					info.append("⚠ SOBRECARGA: qualquer dano é letal!\n");
				}
				Tooltip.install(contractBarPolygon, new Tooltip(info.toString().trim()));

				if (labelContractCount != null) {
					int humanosNaFila = 0;
					boolean humanoAtivoVisto = false;
					for (ContratoDeVida c : personagem.getContratosDeVida()) {
						if (c.isHumano()) {
							if (!humanoAtivoVisto) {
								humanoAtivoVisto = true;
							} else {
								humanosNaFila++;
							}
						}
					}
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
			hpBarPolygon.getStyleClass().add("hp-bar-fill");
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
			infernalShieldBarPolygon.setStyle("-fx-fill: linear-gradient(to right, #ff4500, #ff8c00);");
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

		// Limpa a lista atual
		cardEffectsContainer.getChildren().clear();

		if (personagem.getEfeitosAtivos() != null) {
			int maxIcons = 5;
			int count = 0;

			// Cria uma lista temporária para evitar modificação concorrente se houver
			java.util.List<Efeito> efeitosParaMostrar = new java.util.ArrayList<>(
					personagem.getEfeitosAtivos().values());

			for (Efeito efeito : efeitosParaMostrar) {
				if (efeito != null && count < maxIcons) {

					// Lógica de Texto do Ícone
					String textoIcone = efeito.getNome().substring(0, Math.min(efeito.getNome().length(), 2))
							.toUpperCase();
					if (efeito.getStacks() > 0) {
						textoIcone += " " + efeito.getStacks();
					}

					// --- CRIAÇÃO DO LABEL---
					Label effectIcon = new Label(textoIcone);
					effectIcon.setMinWidth(18);
					effectIcon.setMaxWidth(18);
					effectIcon.setMinHeight(18);
					effectIcon.setMaxHeight(18);
					effectIcon.setAlignment(javafx.geometry.Pos.CENTER);

					// Estilos
					effectIcon.getStyleClass().clear();
					effectIcon.getStyleClass().add("effect-icon");
					if (efeito.getTipo() == TipoEfeito.BUFF)
						effectIcon.getStyleClass().add("effect-icon-buff");
					else if (efeito.getTipo() == TipoEfeito.DEBUFF)
						effectIcon.getStyleClass().add("effect-icon-debuff");
					else
						effectIcon.getStyleClass().add("effect-icon-dot");

					// Tooltip detalhado
					Tooltip tip = new Tooltip(EffectTooltipBuilder.buildTooltip(efeito));
					tip.setStyle("-fx-font-size: 11px; -fx-font-family: 'Consolas'; -fx-background-color: #1a1a2e; -fx-text-fill: #e0e0e0; -fx-border-color: #444; -fx-border-width: 1; -fx-padding: 6;");
					tip.setShowDelay(javafx.util.Duration.millis(200));
					tip.setMaxWidth(320);
					tip.setWrapText(true);
					Tooltip.install(effectIcon, tip);

					// Adiciona ao container
					cardEffectsContainer.getChildren().add(effectIcon);

					count++;
				}
			}
		}
	}

	private void configurarImagem(Personagem personagem, String cardType) {
		if (imgPersonagem == null)
			return;
		String nomeBase;
		if ("player".equals(cardType)) {
			nomeBase = personagem.getNome().toLowerCase().replace(" ", "_");
		} else {
			String nomeLimpo = personagem.getNome().toLowerCase().replace("servo: ", "").replaceAll("\\s*\\d+$", "");
			nomeBase = nomeLimpo.replace(" ", "_");
		}
		String imagePath = "/portraits/" + nomeBase + ".png";
		try {
			Image portraitImage = ImageCache.get(imagePath, 120, 120);
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