package br.com.dantesrpg.controller;

import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.util.FileLoader;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.util.Pair;
import javafx.scene.paint.Color;
import java.util.List;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import br.com.dantesrpg.model.map.TerrainData;
import br.com.dantesrpg.model.map.TerrainData.*;

import java.util.HashMap;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import javafx.scene.input.MouseEvent;

import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.io.FileInputStream;

public class MapController {

	@FXML
	private GridPane mapGrid;
	@FXML
	private ToolBar topToolbar;
	@FXML
	private ToggleButton toggleMover;
	@FXML
	private ToggleButton toggleMirar;
	@FXML
	private ToggleGroup selectionToggleGroup;
	@FXML
	private Canvas aoeCanvas;
	@FXML
	private ToggleButton btnMovimentoLivre;
	@FXML
	private ToggleButton btnEditorMapa;
	@FXML
	private Button btnPularSquad;

	private CombatController mainController;

	private int gridLargura;
	private int gridAltura;
	private final int CELL_SIZE = 40;

	private boolean modoSelecaoAlvo = false;
	private Habilidade habilidadeAtual;
	private boolean modoSelecaoMultipla = false;
	private int alvosRestantes;
	private List<Personagem> alvosSelecionadosMulti = new ArrayList<>();
	private Label labelContadorAlvos;

	private boolean modoMovimentoLivre = false;
	private Personagem peaoSelecionadoParaMover = null;

	private boolean modoSpawnInimigo = false;
	private String idMonstroEmSpawn = null;

	private boolean modoEditor = false;

	private Personagem atorAtual;
	private Map<Node, Personagem> peaoParaPersonagem = new HashMap<>();
	private List<Node> peoesAtuais = new ArrayList<>();
	private Set<Pane> celulasAlcanceMovimento = new HashSet<>();

	private Pane[][] celulasDoGrid = new Pane[gridLargura][gridAltura];
	private boolean[][] paredesGrid = new boolean[gridLargura][gridAltura];

	private TipoTerreno[][] gridTerreno;
	private EfeitoInstance[][] gridEfeitos;

	private final String CSS_ALCANCE_MOVIMENTO = "movimento-alcance";
	private final String CSS_ALCANCE_ATAQUE_MELEE = "ataque-alcance-melee";
	private final String CSS_ALCANCE_ATAQUE_RANGED = "ataque-alcance-ranged";

	// dominios a serem expandidos
	private List<Pane> celulasRingueAlexei = new ArrayList<>();
	private List<Pane> celulasDominioLyria = new ArrayList<>();
	private List<Pane> celulasAuraDarrell = new ArrayList<>();

	private int rolagemSquadGlobal;
	private int cargasSpawnRestantes = 0;
	private List<Pane> celulasGuardiaoDestacadas = new ArrayList<>();

	private List<Personagem> peoesAtualmenteDestacados = new ArrayList<>();

	private Queue<Personagem> filaClonesSquad = new LinkedList<>();
	private Map<Personagem, Personagem> ataquesDeclaradosSquad = new HashMap<>();
	private boolean modoSquad = false;

	@FXML
	public void initialize() {
		if (selectionToggleGroup != null) {
			selectionToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
				if (newToggle == null) {
					if (oldToggle != null)
						oldToggle.setSelected(true);
				} else {
					onToggleModo();
				}
			});
		}

		mapGrid.setOnMouseMoved(this::onMouseMovedNoGrid);

		mapGrid.setOnMouseClicked(event -> {
			if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
				if (modoSpawnInimigo) {
					this.modoSpawnInimigo = false;
					this.idMonstroEmSpawn = null;
					mapGrid.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
					System.out.println("GM: Modo SPAWN cancelado.");
					event.consume();
				}
			}
		});
	}

	@FXML
	private void onMovimentoLivreClick() {
		toggleModoMovimentoLivre();
		// Sincroniza o estado visual do botão caso tenha sido chamado via código
		btnMovimentoLivre.setSelected(this.modoMovimentoLivre);

		// Se ativar um, desativa o outro visualmente
		if (this.modoMovimentoLivre) {
			btnEditorMapa.setSelected(false);
			this.modoEditor = false; // Garante integridade lógica
		}
	}

	@FXML
	private void onEditorMapaClick() {
		toggleModoEditor();
		btnEditorMapa.setSelected(this.modoEditor);

		if (this.modoEditor) {
			btnMovimentoLivre.setSelected(false);
			this.modoMovimentoLivre = false;
		}
	}

	@FXML
	private void onBestiarioClick() {
		if (mainController != null) {
			// O MapController pede para o Main abrir a janela do Bestiário
			mainController.abrirJanelaBestiario();
		}
	}

	private void onMouseMovedNoGrid(MouseEvent event) {
		if (!modoSelecaoAlvo || toggleMover.isSelected() || habilidadeAtual == null) {
			limparCanvas();
			return;
		}
		desenharVisualAoE(event.getX(), event.getY());
	}

	private void limparCanvas() {
		if (aoeCanvas != null) {
			GraphicsContext gc = aoeCanvas.getGraphicsContext2D();
			gc.clearRect(0, 0, aoeCanvas.getWidth(), aoeCanvas.getHeight());
		}
	}

	private void desenharVisualAoE(double mouseX, double mouseY) {
		limparCanvas();
		if (atorAtual == null)
			return;

		GraphicsContext gc = aoeCanvas.getGraphicsContext2D();
		gc.setFill(Color.rgb(148, 0, 211, 0.3)); // Roxo translúcido
		gc.setStroke(Color.rgb(148, 0, 211, 0.8));
		gc.setLineWidth(2);

		double atorPixelX = (atorAtual.getPosX() * CELL_SIZE) + (CELL_SIZE / 2.0);
		double atorPixelY = (atorAtual.getPosY() * CELL_SIZE) + (CELL_SIZE / 2.0);

		double alcancePixels = habilidadeAtual.getAlcanceMaximo() * CELL_SIZE + (CELL_SIZE / 2.0);

		TipoAlvo tipo = habilidadeAtual.getTipoAlvo();
		if (tipo == TipoAlvo.AREA) {
			// Lógica "Cone 360": Círculo centrado no ATOR
			// TamanhoArea é considerado o Diâmetro (ex: 5 = raio 2.5)
			double raioPixels = (habilidadeAtual.getTamanhoArea() / 1.5) * CELL_SIZE;

			// Desenha o círculo em volta do ator (ignorando o mouse X/Y para o centro)
			gc.fillOval(atorPixelX - raioPixels, atorPixelY - raioPixels, raioPixels * 2, raioPixels * 2);
			gc.strokeOval(atorPixelX - raioPixels, atorPixelY - raioPixels, raioPixels * 2, raioPixels * 2);

		} else if (tipo == TipoAlvo.AREA_QUADRADA) {
			int tamanho = habilidadeAtual.getTamanhoArea(); // Ex: 3 (3x3)
			int raio = (tamanho - 1) / 2;

			// Pega a célula onde o mouse está (Snap to Grid)
			int gridX = (int) (mouseX / CELL_SIZE);
			int gridY = (int) (mouseY / CELL_SIZE);

			// Calcula o canto superior esquerdo do quadrado em pixels
			double drawX = (gridX - raio) * CELL_SIZE;
			double drawY = (gridY - raio) * CELL_SIZE;
			double drawSize = tamanho * CELL_SIZE;

			gc.fillRect(drawX, drawY, drawSize, drawSize);
			gc.strokeRect(drawX, drawY, drawSize, drawSize);

		} else if (tipo == TipoAlvo.AREA_CIRCULAR) {
			double raio = (habilidadeAtual.getTamanhoArea() / 2.0) * CELL_SIZE;
			int gridX = (int) (mouseX / CELL_SIZE);
			int gridY = (int) (mouseY / CELL_SIZE);
			double snapX = gridX * CELL_SIZE + (CELL_SIZE / 2.0);
			double snapY = gridY * CELL_SIZE + (CELL_SIZE / 2.0);

			gc.fillOval(snapX - raio, snapY - raio, raio * 2, raio * 2);
			gc.strokeOval(snapX - raio, snapY - raio, raio * 2, raio * 2);

		} else if (tipo == TipoAlvo.CONE) {
			double anguloCone = habilidadeAtual.getAnguloCone();
			double deltaX = mouseX - atorPixelX;
			double deltaY = mouseY - atorPixelY; // Y cresce para baixo na tela

			// Invertendo o Y para a matemática bater com o fillArc
			double anguloMouseGraus = Math.toDegrees(Math.atan2(-deltaY, deltaX));

			for (int desvio : habilidadeAtual.getAngulosDesvio()) {
				// Centraliza o cone no mouse
				double startAngle = anguloMouseGraus + desvio - (anguloCone / 2.0);

				gc.fillArc(atorPixelX - alcancePixels, atorPixelY - alcancePixels, alcancePixels * 2, alcancePixels * 2,
						startAngle, anguloCone, ArcType.ROUND);

				gc.strokeArc(atorPixelX - alcancePixels, atorPixelY - alcancePixels, alcancePixels * 2,
						alcancePixels * 2, startAngle, anguloCone, ArcType.ROUND);
			}
		} else if (tipo == TipoAlvo.LINHA) {
			double larguraLinha = CELL_SIZE;
			gc.save();
			gc.translate(atorPixelX, atorPixelY);
			double deltaX = mouseX - atorPixelX;
			double deltaY = mouseY - atorPixelY;
			double angle = Math.toDegrees(Math.atan2(deltaY, deltaX));
			gc.rotate(angle);
			gc.fillRect(0, -larguraLinha / 2, alcancePixels, larguraLinha);
			gc.strokeRect(0, -larguraLinha / 2, alcancePixels, larguraLinha);
			gc.restore();

		} else if (tipo == TipoAlvo.AREA_CIRCULAR) {
			double raio = (habilidadeAtual.getTamanhoArea() / 2.0) * CELL_SIZE;
			int gridX = (int) (mouseX / CELL_SIZE);
			int gridY = (int) (mouseY / CELL_SIZE);
			double snapX = gridX * CELL_SIZE + (CELL_SIZE / 2.0);
			double snapY = gridY * CELL_SIZE + (CELL_SIZE / 2.0);

			gc.fillOval(snapX - raio, snapY - raio, raio * 2, raio * 2);
			gc.strokeOval(snapX - raio, snapY - raio, raio * 2, raio * 2);
		}
	}

	private void onGridCellMouseEntered(Pane cell, int x, int y) {
		Personagem p = getPersonagemNaCelula(x, y);
		if (p != null && p.getEfeitosAtivos().containsKey("Guardião")) {
			desenharAlcanceGuardiao(x, y);
		} else {
			limparDestaqueGuardiao();
		}

		if (!modoSelecaoAlvo || toggleMover.isSelected() || habilidadeAtual == null) {
			mainController.limparSelecaoDeAlvo();
			return;
		}

		if (!celulasAlcanceMovimento.contains(cell)) {
			mainController.limparSelecaoDeAlvo();
			limparDestaquesPeoes();
			return;
		}

		switch (habilidadeAtual.getTipoAlvo()) {
		case AREA_QUADRADA:
			desenharPreviewQuadrado(x, y);
			break;
		case AREA:
		case AREA_CIRCULAR:
			desenharPreviewCircular(x, y);
			break;
		case LINHA:
			desenharPreviewLinha(x, y);
			break;
		case CONE:
			desenharPreviewCone(x, y);
			break;
		default: // INDIVIDUAL ou MULTIPLOS
			if (p != null && !p.equals(atorAtual)) {
				List<Personagem> lista = new ArrayList<>();
				lista.add(p);
				mainController.alvosIdentificadosNoMapa(lista);
				destacarPeoesAlvo(lista);
			} else {
				mainController.limparSelecaoDeAlvo();
				limparDestaquesPeoes();
			}
			break;
		}
	}

	private void desenharAlcanceGuardiao(int centroX, int centroY) {
		limparDestaqueGuardiao();

		int raio = 3;

		for (int y = centroY - raio; y <= centroY + raio; y++) {
			for (int x = centroX - raio; x <= centroX + raio; x++) {
				if (dentroDoGrid(x, y)) {
					// Opcional: Verificar linha de visão se o guardião não atravessa paredes
					if (!paredesGrid[x][y] || (x == centroX && y == centroY)) {
						Pane cell = celulasDoGrid[x][y];
						cell.getStyleClass().add("alcance-guardiao");
						celulasGuardiaoDestacadas.add(cell);
					}
				}
			}
		}
	}

	private void limparDestaqueGuardiao() {
		if (celulasGuardiaoDestacadas.isEmpty())
			return;
		for (Pane cell : celulasGuardiaoDestacadas) {
			cell.getStyleClass().remove("alcance-guardiao");
		}
		celulasGuardiaoDestacadas.clear();
	}

	private void onGridCellClicked(Pane cell, int x, int y) {
		// Modos Especiais
		if (modoEditor) {
			ciclarTerreno(cell, x, y);
			return;
		}

		if (modoSpawnInimigo) {
			if (paredesGrid[x][y]) {
				System.out.println("MAPA (SPAWN): Parede.");
				return;
			}

			// Spawna
			mainController.resolverSpawn(idMonstroEmSpawn, x, y);

			// Decrementa Cargas
			cargasSpawnRestantes--;
			System.out.println("MAPA: Spawn realizado. Restam: " + cargasSpawnRestantes);

			if (cargasSpawnRestantes <= 0) {
				// Sai do modo spawn
				this.modoSpawnInimigo = false;
				this.idMonstroEmSpawn = null;
				mapGrid.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
				System.out.println("MAPA: Cargas de spawn esgotadas.");
			}
			return;
		}

		if (modoMovimentoLivre) {
			if (peaoSelecionadoParaMover == null) {
				Personagem alvoNaCelula = getPersonagemNaCelula(x, y);

				if (alvoNaCelula != null) {
					peaoSelecionadoParaMover = alvoNaCelula;
					System.out.println("MAPA (GM): 'Pegou' o peão: " + alvoNaCelula.getNome());
					mapGrid.getScene().setCursor(javafx.scene.Cursor.CLOSED_HAND);
				} else {
					System.out.println("MAPA (GM): Clique em célula vazia.");
				}
				return;
			}

			if (paredesGrid[x][y]) {
				System.out.println("MAPA (GM): Não pode soltar em parede.");
				return;
			}

			// Move
			peaoSelecionadoParaMover.setPosX(x);
			peaoSelecionadoParaMover.setPosY(y);

			// Atualiza visual
			desenharPeoes(mainController.getCombatentes());

			// Solta
			System.out.println("MAPA (GM): Soltou " + peaoSelecionadoParaMover.getNome() + " em (" + x + "," + y + ")");
			peaoSelecionadoParaMover = null;
			mapGrid.getScene().setCursor(javafx.scene.Cursor.OPEN_HAND);
			return;
		}

		if (modoSquad && atorAtual != null) {
			tratarCliqueSquad(cell, x, y);
			return;
		}

		if (!modoSelecaoAlvo || atorAtual == null)
			return;

		// MODO MOVER
		if (toggleMover.isSelected()) {
			if (atorAtual.getRaca() != null && !atorAtual.getRaca().podeSeMover(atorAtual)) {
				System.out.println("MAPA: " + atorAtual.getNome() + " nao pode se mover enquanto estiver em postura.");
				return;
			}
			if (celulasAlcanceMovimento.contains(cell)) {
				// Prioridade: Saída
				if (cell.getStyleClass().contains("map-exit")) {
					mainController.acionarTransicaoDeMapa(atorAtual);
					return;
				}
				int custoMovimento = calcularDistancia(atorAtual.getPosX(), atorAtual.getPosY(), x, y);
				if (custoMovimento > atorAtual.getMovimentoRestanteTurno())
					return;

				br.com.dantesrpg.model.util.SessionLogger
						.log(atorAtual.getNome() + " moveu-se para (" + x + "," + y + ").");
				atorAtual.setMovimentoRestanteTurno(atorAtual.getMovimentoRestanteTurno() - custoMovimento);
				atorAtual.setPosX(x);
				atorAtual.setPosY(y);

				desenharPeoes(mainController.getCombatentes());

				if (mainController != null) {
					mainController.verificarInteracaoTerreno(atorAtual);
					mainController.notificarMovimentoRealizado();
				}

				if (modoSelecaoAlvo && habilidadeAtual != null) {
					toggleMirar.setSelected(true);
					calcularEExibirAtaqueRange(atorAtual, habilidadeAtual);
					limparCanvas();
				} else {
					calcularEExibirMovimento(atorAtual);
				}
			}
		}
		// MODO MIRAR
		else {

			// Verifica AoE
			if (habilidadeAtual != null) {
				TipoAlvo tipo = habilidadeAtual.getTipoAlvo();
				if (tipo == TipoAlvo.AREA_QUADRADA || tipo == TipoAlvo.AREA_CIRCULAR || tipo == TipoAlvo.LINHA
						|| tipo == TipoAlvo.CONE || tipo == TipoAlvo.AREA) {
					selecionarAreaComEpicentro(x, y);
					return;
				}
			}

			// Verifica Single Target (Peão ou Objeto)
			if (celulasAlcanceMovimento.contains(cell)) {
				Personagem alvo = getPersonagemNaCelula(x, y);

				if (alvo != null && !alvo.equals(atorAtual)) {
					if (atorAtual.isClone() && alvo.isClone() && atorAtual.getCriador() == alvo.getCriador()) {
						System.out.println("MAPA: Clone não pode atacar seu aliado clone!");
						return;
					}
					if (modoSelecaoMultipla) {
						alvosSelecionadosMulti.add(alvo);
						alvosRestantes--;
						labelContadorAlvos.setText("Alvos restantes: " + alvosRestantes);
						if (alvosRestantes <= 0) {
							mainController.adicionarAlvosSelecionados(alvosSelecionadosMulti);
							sairModoSelecao();
						}
					} else {
						mainController.adicionarAlvoSelecionado(alvo);
						sairModoSelecao();
					}
				}
			}
		}
	}

	private void calcularAlcanceBFS(int startX, int startY, int maxDist, String cssClass) {
		if (maxDist <= 0)
			return;
		limparDestaquesAlcance();

		if (cssClass.equals(CSS_ALCANCE_MOVIMENTO)) {
			// Lógica de Movimento (Bloqueia em Paredes e Objetos)
			Queue<Pair<Integer, Integer>> fila = new LinkedList<>();
			int[][] distancias = new int[gridLargura][gridAltura];
			for (int i = 0; i < gridLargura; i++)
				java.util.Arrays.fill(distancias[i], -1);

			fila.add(new Pair<>(startX, startY));
			distancias[startX][startY] = 0;
			int[] dx = { 0, 0, 1, -1 };
			int[] dy = { 1, -1, 0, 0 };

			while (!fila.isEmpty()) {
				Pair<Integer, Integer> atual = fila.poll();
				int x = atual.getKey();
				int y = atual.getValue();
				if (distancias[x][y] + 1 > maxDist)
					continue;

				for (int i = 0; i < 4; i++) {
					int novoX = x + dx[i];
					int novoY = y + dy[i];
					if (novoX >= 0 && novoX < gridLargura && novoY >= 0 && novoY < gridAltura) {
						// Verifica Auras (Domínios)
						boolean origemA = isCelulaNoDominio(x, y, celulasRingueAlexei);
						boolean destinoA = isCelulaNoDominio(novoX, novoY, celulasRingueAlexei);
						if (origemA != destinoA)
							continue;
						boolean origemL = isCelulaNoDominio(x, y, celulasDominioLyria);
						boolean destinoL = isCelulaNoDominio(novoX, novoY, celulasDominioLyria);
						if (origemL != destinoL)
							continue;

						// Bloqueia se for Parede física
						if (!paredesGrid[novoX][novoY] && distancias[novoX][novoY] == -1) {
							distancias[novoX][novoY] = distancias[x][y] + 1;
							fila.add(new Pair<>(novoX, novoY));
							Pane cell = celulasDoGrid[novoX][novoY];
							cell.getStyleClass().add(cssClass);
							celulasAlcanceMovimento.add(cell);
						}
					}
				}
			}
		} else {
			// Lógica de Ataque (LoS) - Permite mirar em Objetos
			for (int y = startY - maxDist; y <= startY + maxDist; y++) {
				for (int x = startX - maxDist; x <= startX + maxDist; x++) {
					if (x < 0 || x >= gridLargura || y < 0 || y >= gridAltura)
						continue;
					if (x == startX && y == startY)
						continue;

					boolean isParede = paredesGrid[x][y];
					boolean isObjeto = false;

					if (isParede) {
						Personagem p = getPersonagemNaCelula(x, y);
						if (p instanceof br.com.dantesrpg.model.elementos.ObjetoDestrutivel) {
							isObjeto = true;
						}
					}

					// Se for parede real (sem objeto), ignora. Se for objeto, permite mirar.
					if (isParede && !isObjeto)
						continue;

					int dist = Math.max(Math.abs(x - startX), Math.abs(y - startY));
					if (dist > maxDist)
						continue;

					if (temLinhaDeVisao(startX, startY, x, y)) {
						Pane cell = celulasDoGrid[x][y];
						cell.getStyleClass().add(cssClass);
						celulasAlcanceMovimento.add(cell);
					}
				}
			}
		}
	}

	private boolean temLinhaDeVisao(int x0, int y0, int x1, int y1) {
		int dx = Math.abs(x1 - x0);
		int dy = -Math.abs(y1 - y0);
		int sx = (x0 < x1) ? 1 : -1;
		int sy = (y0 < y1) ? 1 : -1;
		int err = dx + dy;
		int currentX = x0;
		int currentY = y0;

		while (true) {
			if ((currentX != x0 || currentY != y0) && paredesGrid[currentX][currentY]) {
				// Se bateu no destino (x1, y1), é válido (permite atirar na parede)
				if (currentX == x1 && currentY == y1)
					return true;
				return false; // Bateu em algo no caminho
			}
			if (currentX == x1 && currentY == y1)
				break;
			int e2 = 2 * err;
			if (e2 >= dy) {
				if (currentX == x1)
					break;
				err += dy;
				currentX += sx;
			}
			if (e2 <= dx) {
				if (currentY == y1)
					break;
				err += dx;
				currentY += sy;
			}
		}
		return true;
	}

	private void onToggleModo() {
		if (atorAtual == null)
			return;

		limparCanvas();

		if (toggleMover.isSelected()) {
			System.out.println("MAPA: Modo Mover (Squad/Normal).");
			calcularEExibirMovimento(atorAtual);
		} else {
			System.out.println("MAPA: Modo Mirar (Squad/Normal).");
			calcularEExibirAtaqueRange(atorAtual, habilidadeAtual);
		}
	}

	private void atualizarBotaoPularSquad() {
		if (btnPularSquad == null)
			return;

		boolean mostrar = modoSquad && !filaClonesSquad.isEmpty();
		btnPularSquad.setVisible(mostrar);
		btnPularSquad.setManaged(mostrar);
		btnPularSquad.setDisable(!mostrar);

		if (mostrar) {
			btnPularSquad.setText("Encerrar Fila de Clones (" + filaClonesSquad.size() + ")");
		}
	}

	public void setMainController(CombatController mainController) {
		this.mainController = mainController;
	}

	public void carregarMapaDeImagem(File mapaFile) {
		System.out.println("MAPA: Carregando mapa de arena: " + mapaFile.getName());

		try {
			// Carrega a imagem a partir de um ARQUIVO, não de um recurso
			Image mapaImage = new Image(new FileInputStream(mapaFile));
			if (mapaImage.isError())
				throw new Exception("Falha ao carregar imagem: " + mapaFile.getName());

			PixelReader pixelReader = mapaImage.getPixelReader();

			this.gridLargura = (int) mapaImage.getWidth();
			this.gridAltura = (int) mapaImage.getHeight();

			if (aoeCanvas != null) {
				aoeCanvas.setWidth(gridLargura * CELL_SIZE);
				aoeCanvas.setHeight(gridAltura * CELL_SIZE);
			}

			System.out.println("MAPA: Tamanho detectado " + gridLargura + "x" + gridAltura);

			paredesGrid = new boolean[gridLargura][gridAltura];
			celulasDoGrid = new Pane[gridLargura][gridAltura];

			gridTerreno = new TipoTerreno[gridLargura][gridAltura];
			gridEfeitos = new EfeitoInstance[gridLargura][gridAltura];

			mapGrid.getChildren().clear();
			mapGrid.getColumnConstraints().clear();
			mapGrid.getRowConstraints().clear();

			mapGrid.getChildren().clear();
			mapGrid.getColumnConstraints().clear();
			mapGrid.getRowConstraints().clear();

			for (int x = 0; x < gridLargura; x++) {
				mapGrid.getColumnConstraints().add(new ColumnConstraints(CELL_SIZE));
			}
			for (int y = 0; y < gridAltura; y++) {
				mapGrid.getRowConstraints().add(new RowConstraints(CELL_SIZE));
			}

			for (int y = 0; y < gridAltura; y++) {
				for (int x = 0; x < gridLargura; x++) {

					Pane cell = new Pane();
					Color corPixel = pixelReader.getColor(x, y);
					final int cellX = x;
					final int cellY = y;

					int r = (int) (corPixel.getRed() * 255);
					int g = (int) (corPixel.getGreen() * 255);
					int b = (int) (corPixel.getBlue() * 255);

					TipoTerreno tipoDetectado = TipoTerreno.PADRAO;

					if (r == 48 && g == 48 && b == 48) {
						cell.getStyleClass().add("map-fog");
						paredesGrid[x][y] = true;
						tipoDetectado = TipoTerreno.PAREDE;
					} else if (r == 240 && g == 240 && b == 240) {
						cell.getStyleClass().add("map-wall");
						paredesGrid[x][y] = true;
						tipoDetectado = TipoTerreno.PAREDE;
					}

					else if (r == 1 && g == 1 && b == 1) {
						cell.getStyleClass().add("map-wall2");
						paredesGrid[x][y] = true;
						tipoDetectado = TipoTerreno.PAREDE;
					} else if (r == 2 && g == 2 && b == 2) {
						cell.getStyleClass().add("map-wall3");
						paredesGrid[x][y] = true;
						tipoDetectado = TipoTerreno.PAREDE;
					}

					else if (r == 3 && g == 3 && b == 3) {
						cell.getStyleClass().add("map-wall4");
						paredesGrid[x][y] = true;
						tipoDetectado = TipoTerreno.PAREDE;
					}

					else if (r == 5 && g == 5 && b == 5) {
						cell.getStyleClass().add("map-wall5");
						paredesGrid[x][y] = true;
						tipoDetectado = TipoTerreno.PAREDE;
					}

					else if (r == 6 && g == 6 && b == 6) {
						cell.getStyleClass().add("map-wall6");
						paredesGrid[x][y] = true;
						tipoDetectado = TipoTerreno.PAREDE;
					}

					else if (r == 240 && g == 240 && b == 50) {
						cell.getStyleClass().add("map-object-light");
						paredesGrid[x][y] = true;
						tipoDetectado = TipoTerreno.OBJETO;
					} else if (r == 50 && g == 200 && b == 50) {
						cell.getStyleClass().add("map-exit");
						paredesGrid[x][y] = false;
						tipoDetectado = TipoTerreno.SAIDA;
					} else if (r == 40 && g == 40 && b == 40) {
						cell.getStyleClass().add("map-coal");
						paredesGrid[x][y] = false;
						tipoDetectado = TipoTerreno.CARVAO;
					}

					else if (r == 60 && g == 60 && b == 60) {
						cell.getStyleClass().add("map-floor2");
						paredesGrid[x][y] = false;
						tipoDetectado = TipoTerreno.PADRAO;
					} else if (r == 255 && g == 0 && b == 25) {
						cell.getStyleClass().add("map-floor3");
						paredesGrid[x][y] = false;
						tipoDetectado = TipoTerreno.PADRAO;
					} else if (r == 182 && g == 255 && b == 0) {
						cell.getStyleClass().add("map-grass1");
						paredesGrid[x][y] = false;
						tipoDetectado = TipoTerreno.PADRAO;
					}

					else if (r == 160 && g == 160 && b == 160) {
						cell.getStyleClass().add("map-floor4");
						paredesGrid[x][y] = false;
						tipoDetectado = TipoTerreno.PADRAO;
					}

					else if (r == 80 && g == 80 && b == 80) {
						cell.getStyleClass().add("map-floor5");
						paredesGrid[x][y] = false;
						tipoDetectado = TipoTerreno.PADRAO;
					}

					else if (r == 100 && g == 100 && b == 100) {
						cell.getStyleClass().add("map-floor6");
						paredesGrid[x][y] = false;
						tipoDetectado = TipoTerreno.PADRAO;
					}

					else if (r == 110 && g == 110 && b == 110) {
						cell.getStyleClass().add("map-floor7");
						paredesGrid[x][y] = false;
						tipoDetectado = TipoTerreno.PADRAO;
					}

					else if (r == 255 && g == 106 && b == 0) {
						cell.getStyleClass().add("map-lava");
						paredesGrid[x][y] = false;
						tipoDetectado = TipoTerreno.LAVA;

						EfeitoInstance lavaEffect = new EfeitoInstance(TipoEfeitoSolo.FOGO, 99999, 10, null);
						lavaEffect.setPermanente(true);
						gridEfeitos[x][y] = lavaEffect;
					}

					else {
						cell.getStyleClass().add("map-floor");
						paredesGrid[x][y] = false;
						tipoDetectado = TipoTerreno.PADRAO;
					}

					gridTerreno[x][y] = tipoDetectado;

					cell.getStyleClass().add("map-cell");
					cell.setOnMouseClicked(event -> onGridCellClicked(cell, cellX, cellY));
					cell.setOnMouseEntered(event -> onGridCellMouseEntered(cell, cellX, cellY));
					mapGrid.add(cell, x, y);
					celulasDoGrid[x][y] = cell;
				}
			}

		} catch (Exception e) {
			System.err.println("Erro crítico ao carregar mapa de imagem.");
			e.printStackTrace();
			preencherComChaoPadrao();
		}
	}

	public void criarAreaDeFogo(int centroX, int centroY, int raio, int dano, Personagem criador) {
		System.out.println("MAPA: Criando área de fogo centrada em (" + centroX + "," + centroY + ")");

		for (int y = centroY - raio; y <= centroY + raio; y++) {
			for (int x = centroX - raio; x <= centroX + raio; x++) {
				if (dentroDoGrid(x, y)) {
					// Não põe fogo em parede
					if (paredesGrid[x][y])
						continue;

					// Cria o efeito de fogo
					EfeitoInstance fogo = new EfeitoInstance(TipoEfeitoSolo.FOGO, 300, // Duração (300 TU)
							dano, criador);

					// Se for lava ou carvão, vira permanente
					if (gridTerreno[x][y] == TipoTerreno.LAVA || gridTerreno[x][y] == TipoTerreno.CARVAO) {
						fogo.setPermanente(true);
					}

					aplicarEfeitoNoSolo(x, y, fogo);
				}
			}
		}
	}

	public void avancarTempoTerreno(int tempoDecorrido) {
		boolean visualMudou = false;

		for (int x = 0; x < gridLargura; x++) {
			for (int y = 0; y < gridAltura; y++) {
				TerrainData.EfeitoInstance efeito = gridEfeitos[x][y];

				if (efeito != null && !efeito.isPermanente()) {
					efeito.reduzirDuracao(tempoDecorrido);

					if (efeito.expirou()) {
						System.out.println("MAPA: Efeito em (" + x + "," + y + ") expirou.");
						gridEfeitos[x][y] = null; // Remove o efeito
						atualizarVisualTerreno(x, y); // Atualiza CSS
						visualMudou = true;
					}
				}
			}
		}
	}

	public void aplicarEfeitoNoSolo(int x, int y, EfeitoInstance novoEfeito) {
		if (!dentroDoGrid(x, y))
			return;

		TipoTerreno terrenoAtual = gridTerreno[x][y];

		// Regra 1: Paredes e Saídas geralmente ignoram efeitos de chão
		if (terrenoAtual == TipoTerreno.PAREDE || terrenoAtual == TipoTerreno.SAIDA)
			return;

		// --- LÓGICA DO FOGO ---
		if (novoEfeito.getTipo() == TipoEfeitoSolo.FOGO) {

			// CASO A: Atingiu Carvão
			if (terrenoAtual == TipoTerreno.CARVAO) {
				// Se já estiver pegando fogo permanentemente, ignora para evitar loop infinito
				EfeitoInstance efeitoAtual = gridEfeitos[x][y];
				if (efeitoAtual != null && efeitoAtual.getTipo() == TipoEfeitoSolo.FOGO && efeitoAtual.isPermanente()) {
					return;
				}

				// Transforma em Fogo Permanente
				novoEfeito.setPermanente(true);
				gridEfeitos[x][y] = novoEfeito;

				System.out.println("TERRENO: Carvão aceso em (" + x + "," + y + ")!");
				atualizarVisualTerreno(x, y);

				// Reação em Cadeia (Flood Fill)
				espalharFogoRecursivo(x, y, novoEfeito);
			}
			// CASO B: Chão Comum (ou Objeto)
			else {
				// Apenas aplica o efeito temporário
				gridEfeitos[x][y] = novoEfeito;
				atualizarVisualTerreno(x, y);
			}
		}
		// --- OUTROS EFEITOS Proximos andares rs---
		else {
			// Lógica padrão para ácido, gás, etc.
			gridEfeitos[x][y] = novoEfeito;
			atualizarVisualTerreno(x, y);
		}
	}

	/**
	 * Espalha o fogo para vizinhos que sejam CARVÃO.
	 */
	private void espalharFogoRecursivo(int x, int y, EfeitoInstance efeitoOriginal) {
		int[] dx = { 0, 0, 1, -1 };
		int[] dy = { 1, -1, 0, 0 };

		for (int i = 0; i < 4; i++) {
			int nx = x + dx[i];
			int ny = y + dy[i];

			if (dentroDoGrid(nx, ny)) {
				if (gridTerreno[nx][ny] == TipoTerreno.CARVAO) {
					// Cria uma cópia ou reutiliza a referência (aqui reutilizamos pois é permanente)
					aplicarEfeitoNoSolo(nx, ny, efeitoOriginal);
				}
			}
		}
	}

	private void atualizarVisualTerreno(int x, int y) {
		if (!dentroDoGrid(x, y))
			return;

		Pane cell = celulasDoGrid[x][y];
		EfeitoInstance efeito = gridEfeitos[x][y];
		TipoTerreno terreno = gridTerreno[x][y];

		// LIMPEZA: Remove qualquer imagem de efeito anterior (preservando o fundo/css base)
		// Removem tudo que for ImageView e tiver a ID de efeito
		cell.getChildren().removeIf(node -> node.getId() != null && node.getId().equals("terreno-fx-layer"));

		// Se não tem efeito, remove classes de brilho e sai
		if (efeito == null) {
			cell.getStyleClass().remove("map-coal-burning"); // Remove brilho extra se houver
			return;
		}

		// APLICAÇÃO DE VISUAL NOVO
		String imagemParaCarregar = null;
		boolean aplicarBrilhoCss = false;

		if (efeito.getTipo() == TipoEfeitoSolo.FOGO) {
			if (terreno == TipoTerreno.CARVAO && efeito.isPermanente()) {
				// CASO 1: CARVÃO ACESO (Sprite de "PNJ" em chamas)
				imagemParaCarregar = "/effects/fire_column.png"; // Coloque sua imagem aqui
				aplicarBrilhoCss = true; // Mantém um brilho vermelho no fundo
			} else {
				// CASO 2: FOGO NORMAL (Overlay de chão)
				imagemParaCarregar = "/effects/fire_ground_overlay.png";
			}
		} else if (efeito.getTipo() == TipoEfeitoSolo.ACIDO) {
			imagemParaCarregar = "/effects/acid_pool.png";
		} else if (efeito.getTipo() == TipoEfeitoSolo.GAS) {
			imagemParaCarregar = "/effects/gas_cloud.png";

		} else if (efeito.getTipo() == TipoEfeitoSolo.PORTAL) {
			// Se tiver sprite de portal, carrega aqui. Se não, usa CSS.
			if (!cell.getStyleClass().contains("effect-portal"))
				cell.getStyleClass().add("effect-portal");
			imagemParaCarregar = "/effects/portal_vergil.png";
		}

		// RENDERIZAÇÃO
		if (imagemParaCarregar != null) {
			try {
				// Tenta carregar
				Image img = new Image(FileLoader.carregarArquivo(imagemParaCarregar));
				if (!img.isError()) {
					ImageView fxView = new ImageView(img);
					fxView.setFitWidth(CELL_SIZE);
					fxView.setFitHeight(CELL_SIZE);
					fxView.setPreserveRatio(true);
					fxView.setMouseTransparent(true); // O mouse deve passar direto para clicar no chão
					fxView.setId("terreno-fx-layer"); // Marca para remoção futura

					if (!aplicarBrilhoCss) {
						fxView.setOpacity(0.6);
					}

					cell.getChildren().add(fxView);
				}
			} catch (Exception e) {
				// Fallback silencioso: Se não achar a imagem, usa borda colorida via CSS
				if (efeito.getTipo() == TipoEfeitoSolo.FOGO) {
					if (!cell.getStyleClass().contains("effect-fire"))
						cell.getStyleClass().add("effect-fire");
				}
			}
		}
		if (efeito == null || efeito.getTipo() != TipoEfeitoSolo.PORTAL) {
			cell.getStyleClass().remove("effect-portal");
		}
		// CSS Adicional
		if (aplicarBrilhoCss) {
			if (!cell.getStyleClass().contains("map-coal-burning"))
				cell.getStyleClass().add("map-coal-burning");
		} else {
			cell.getStyleClass().remove("map-coal-burning");
		}
	}

	private void preencherComChaoPadrao() {
		mapGrid.getChildren().clear();
		for (int y = 0; y < gridAltura; y++) {
			for (int x = 0; x < gridLargura; x++) {
				Pane cell = new Pane();
				cell.getStyleClass().add("map-floor");
				final int cellX = x;
				final int cellY = y;
				cell.setOnMouseClicked(event -> onGridCellClicked(cell, cellX, cellY));
				cell.setOnMouseEntered(event -> onGridCellMouseEntered(cell, cellX, cellY));
				mapGrid.add(cell, x, y);
				celulasDoGrid[x][y] = cell;
			}
		}
		paredesGrid = new boolean[gridLargura][gridAltura];
	}

	public void desenharPeoes(List<Personagem> combatentes) {
		// Limpeza
		for (Node peaoNode : peoesAtuais) {
			mapGrid.getChildren().remove(peaoNode);
		}
		peoesAtuais.clear();
		peaoParaPersonagem.clear();

		for (Personagem p : combatentes) {
			if (p != null && p.isAtivoNoCombate()) {

				// --- LÓGICA DE TAMANHO GIGANTE ---
				// EU JURO QUE TO TENTANDO MAS QUE PORRA È ESSA ESSA MERDA NÂO RENDEIRIZA CORRETAMENTE FILHAS DA PUTA QUE PROGRAMARAM ESSA MERDA MORRA (EU)
				int larguraTiles = p.getTamanhoX();
				int alturaTiles = p.getTamanhoY();
				int menorDimensao = Math.min(larguraTiles, alturaTiles); // O tamanho da imagem quadrada
				double margem = 2.0;
				double tamanhoTotalPixels = (menorDimensao * CELL_SIZE) - (margem * 2);
				double raioClip = tamanhoTotalPixels / 2.0;

				Pane peaoContainer = new Pane();
				peaoContainer.setPrefSize(tamanhoTotalPixels, tamanhoTotalPixels);

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
					Image tokenImage = new Image(FileLoader.carregarArquivo(imagePath));
					if (tokenImage.isError())
						throw new Exception("Erro imagem");

					javafx.scene.image.ImageView tokenView = new javafx.scene.image.ImageView(tokenImage);
					tokenView.setFitWidth(tamanhoTotalPixels);
					tokenView.setFitHeight(tamanhoTotalPixels);
					tokenView.setPreserveRatio(true); // Mantém proporção dentro do quadrado

					// Clip Circular
					Circle clip = new Circle(raioClip);
					clip.setCenterX(tamanhoTotalPixels / 2.0);
					clip.setCenterY(tamanhoTotalPixels / 2.0);
					tokenView.setClip(clip);

					Circle borda = new Circle(raioClip, Color.TRANSPARENT);
					borda.setStrokeWidth(3.0); // Borda um pouco mais grossa para gigantes
					if (isPlayer)
						borda.setStroke(Color.CYAN);
					else
						borda.setStroke(Color.RED);

					// Centraliza visualmente no Container
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
					// Fallback (Círculo colorido se falhar imagem)
					Circle peaoCirculo = new Circle(raioClip);
					peaoCirculo.setStroke(Color.WHITE);
					if (isPlayer)
						peaoCirculo.setFill(Color.CYAN);
					else
						peaoCirculo.setFill(Color.RED);
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

				// Centraliza o container dentro das células mescladas
				GridPane.setHalignment(peaoContainer, javafx.geometry.HPos.CENTER);
				GridPane.setValignment(peaoContainer, javafx.geometry.VPos.CENTER);

				peoesAtuais.add(peaoContainer);

				// --- DESENHO DAS "SOBRAS" (Hitbox Vermelha) ---
				// Se o inimigo não for quadrado perfeito (ex: 5x7), pinta o resto
				if (larguraTiles != alturaTiles || larguraTiles > menorDimensao || alturaTiles > menorDimensao) {
					for (int dy = 0; dy < alturaTiles; dy++) {
						for (int dx = 0; dx < larguraTiles; dx++) {

							// A imagem ocupa de (0,0) até (menorDimensao, menorDimensao)
							boolean cobertoPelaImagem = (dx < menorDimensao && dy < menorDimensao);

							if (!cobertoPelaImagem) {
								int tileRealX = p.getPosX() + dx;
								int tileRealY = p.getPosY() + dy;

								if (dentroDoGrid(tileRealX, tileRealY)) {
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

		// --- RESTO DO MÉTODO (Auras, Barras de Vida) ---
		for (Personagem p : combatentes) {
			if (p.isAtivoNoCombate() && p.getEfeitosAtivos().containsKey("Modo Justiça")) {
				desenharAuraDarrell(p);
				break;
			}
		}
		boolean ninguemComAura = combatentes.stream().noneMatch(p -> p.getEfeitosAtivos().containsKey("Modo Justiça"));
		if (ninguemComAura && !celulasAuraDarrell.isEmpty()) {
			for (Pane cell : celulasAuraDarrell)
				cell.getStyleClass().remove("zona-aura-darrell");
			celulasAuraDarrell.clear();
		}
		desenharBarrasDeVidaObjetos(combatentes);
	}

	private Personagem getPersonagemNaCelula(int x, int y) {
		if (mainController == null)
			return null;
		for (Personagem p : mainController.getCombatentes()) {
			if (p.isAtivoNoCombate()) {
				// Verifica se o personagem ocupa a célula (suporta 1x1, 3x3,5x7...)
				if (p.ocupa(x, y))
					return p;
			}

			// Verifica objetos (geralmente 1x1, mas se tiverem tamanho, o ocupa() resolve também)
			if (p instanceof br.com.dantesrpg.model.elementos.ObjetoDestrutivel) {
				if (((br.com.dantesrpg.model.elementos.ObjetoDestrutivel) p).isIntacto()) {
					if (p.ocupa(x, y))
						return p;
				}
			}
		}
		return null;
	}

	private void destacarPeoesAlvo(List<Personagem> alvos) {
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

					// Centraliza o círculo no meio do Pane
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

	// Método auxiliar para obter o Node do peão
	private Node getPeaoNode(Personagem p) {
		for (Map.Entry<Node, Personagem> entry : peaoParaPersonagem.entrySet()) {
			if (entry.getValue().equals(p)) {
				return entry.getKey();
			}
		}
		return null;
	}

	// Lembre-se de chamar limparDestaquesPeoes() em sairModoSelecao() e iniciarSelecaoDeAlvo()
	public void limparDestaquesPeoes() {
		destacarPeoesAlvo(Collections.emptyList()); // Chama com lista vazia para limpar tudo
	}

	public void toggleModoEditor() {
		this.modoEditor = !this.modoEditor;

		// Desativa outros modos para evitar conflito
		if (this.modoEditor) {
			this.modoMovimentoLivre = false;
			this.modoSpawnInimigo = false;
			this.modoSelecaoAlvo = false;
			sairModoSelecao(); // Limpa visual de combate

			System.out.println("MAPA: Modo EDITOR ativado.");
			mapGrid.getScene().setCursor(javafx.scene.Cursor.OPEN_HAND); // Cursor diferente (ex: Ajuste)
		} else {
			System.out.println("MAPA: Modo EDITOR desativado.");
			mapGrid.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
		}
	}

	private void ciclarTerreno(Pane cell, int x, int y) {
		// Verifica o estado atual pelo CSS
		boolean isParede = cell.getStyleClass().contains("map-wall");
		boolean isObjeto = cell.getStyleClass().contains("map-object-light");
		boolean isChao = cell.getStyleClass().contains("map-floor"); // ou padrão

		// Limpa estilos de terreno
		cell.getStyleClass().remove("map-wall");
		cell.getStyleClass().remove("map-object-light");
		cell.getStyleClass().remove("map-floor");
		cell.getStyleClass().remove("map-exit"); // Remove saída se houver

		mainController.removerObjetoNoMapa(x, y);

		if (isChao || (!isParede && !isObjeto)) {
			// Chão -> Parede
			cell.getStyleClass().add("map-wall");
			paredesGrid[x][y] = true;
			System.out.println("EDITOR: (" + x + "," + y + ") definido como PAREDE.");

		} else if (isParede) {
			// Virou OBJETO DESTRUTÍVEL
			cell.getStyleClass().add("map-object-light");
			paredesGrid[x][y] = true; // Bloqueia movimento

			mainController.criarObjetoNoMapa(x, y);

		} else {
			// Objeto -> Chão
			cell.getStyleClass().add("map-floor");
			paredesGrid[x][y] = false; // Desbloqueia
			System.out.println("EDITOR: (" + x + "," + y + ") definido como CHÃO.");
		}
	}

	public void atualizarCelulaParaChao(int x, int y) {
		if (x >= 0 && x < gridLargura && y >= 0 && y < gridAltura) {
			Pane cell = celulasDoGrid[x][y];
			cell.getStyleClass().remove("map-object-light");
			cell.getStyleClass().add("map-floor");
			paredesGrid[x][y] = false; // Libera passagem
		}
	}

	public void gerarMapaPngVazio(String nomeArquivo, int largura, int altura) {
		System.out.println("GERADOR: Gerando PNG de mapa vazio: " + nomeArquivo + " (" + largura + "x" + altura + ")");
		WritableImage wImage = new WritableImage(largura, altura);
		// Preenche com a cor padrão do chão (ex: preto)
		for (int y = 0; y < altura; y++) {
			for (int x = 0; x < largura; x++) {
				wImage.getPixelWriter().setColor(x, y, Color.BLACK); // Cor de chão padrão
			}
		}

		File outputFile = new File("src/main/resources/" + nomeArquivo + ".png"); // Salva na pasta resources
		try {
			ImageIO.write(SwingFXUtils.fromFXImage(wImage, null), "png", outputFile);
			System.out.println("GERADOR: Mapa salvo em: " + outputFile.getAbsolutePath());
		} catch (Exception e) {
			System.err.println("GERADOR: Erro ao salvar o PNG do mapa: " + e.getMessage());
		}
	}

	public void entrarModoSelecao(Habilidade habilidade, Personagem ator) {
		// reseta o modo
		this.modoSelecaoAlvo = true;
		this.habilidadeAtual = habilidade;
		this.atorAtual = ator;
		this.modoSelecaoMultipla = false;
		this.alvosRestantes = 0;
		this.alvosSelecionadosMulti.clear();
		removerLabelContador();

		System.out.println("MAPA: " + ator.getNome() + " entrando em modo de seleção...");

		if (habilidade != null && habilidade.getTipoAlvo() == TipoAlvo.AREA_QUADRADA
				|| habilidade.getTipoAlvo() == TipoAlvo.AREA) {
			toggleMirar.setSelected(true);
			System.out.println("MAPA: Modo Mirar Área (AoE) ativado.");
			calcularEExibirAtaqueRange(ator, habilidade);
		} else if (habilidade != null && habilidade.getTipoAlvo() == TipoAlvo.MULTIPLOS) {
			this.modoSelecaoMultipla = true;
			this.alvosRestantes = habilidade.getNumeroDeAlvos();

			criarLabelContador(); // (Vamos criar este método)
			calcularEExibirAtaqueRange(ator, habilidade); // Mostra o alcance
			System.out.println("MAPA: Modo Seleção Múltipla ativado. Alvos restantes: " + alvosRestantes);

		} else {
			toggleMover.setSelected(true);
			calcularEExibirMovimento(ator);
		}

		mapGrid.getScene().setCursor(javafx.scene.Cursor.CROSSHAIR);
		limparCanvas();
		limparDestaquesGridAtaque();
		limparDestaquesPeoes();
	}

	private void limparDestaquesGridAtaque() {
		for (Pane cell : celulasAlcanceMovimento) { // Assumindo que celulasAlcanceMovimento inclui as células que
													// poderiam ser alvo
			cell.getStyleClass().remove(CSS_ALCANCE_ATAQUE_MELEE);
			cell.getStyleClass().remove(CSS_ALCANCE_ATAQUE_RANGED);
		}
	}

	private void criarLabelContador() {
		if (labelContadorAlvos == null) {
			labelContadorAlvos = new Label();
			labelContadorAlvos.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: yellow;");
		}
		labelContadorAlvos.setText("Alvos restantes: " + alvosRestantes);

		// Adiciona na barra superior se não estiver lá
		if (!topToolbar.getItems().contains(labelContadorAlvos)) {
			topToolbar.getItems().add(labelContadorAlvos);
		}
	}

	private void removerLabelContador() {
		if (labelContadorAlvos != null && topToolbar != null) {
			topToolbar.getItems().remove(labelContadorAlvos);
		}
	}

	private void limparDestaquesAlcance() {
		for (Pane cell : celulasAlcanceMovimento) {
			cell.getStyleClass().remove(CSS_ALCANCE_MOVIMENTO);
			cell.getStyleClass().remove(CSS_ALCANCE_ATAQUE_MELEE);
			cell.getStyleClass().remove(CSS_ALCANCE_ATAQUE_RANGED);
		}
		celulasAlcanceMovimento.clear();
	}

	private void selecionarAreaComEpicentro(int epicentroX, int epicentroY) {
		limparCanvas();

		if (habilidadeAtual != null && habilidadeAtual.getTipoAlvo() != TipoAlvo.AREA) {
			Pane celulaClicada = celulasDoGrid[epicentroX][epicentroY];
			if (!celulasAlcanceMovimento.contains(celulaClicada)) {
				System.out.println(">>> ALVO FORA DO ALCANÇE de conjuração!");
				return;
			}
		}

		System.out.println("MAPA: Célula de centro AoE selecionada: (" + epicentroX + "," + epicentroY + ")");

		// Encontra os alvos (usando o novo método genérico)
		List<Personagem> alvosNaArea = encontrarAlvosNaForma(epicentroX, epicentroY, habilidadeAtual, atorAtual);

		// Envia os alvos E o epicentro (x,y) para a HUD
		mainController.adicionarAlvosArea(alvosNaArea, epicentroX, epicentroY);
		sairModoSelecao();
	}

	private void calcularEExibirMovimento(Personagem ator) {
		limparDestaquesAlcance();
		if (ator == null)
			return;
		if (ator.getRaca() != null && !ator.getRaca().podeSeMover(ator)) {
			System.out.println("DEBUG: Movimento bloqueado por postura de " + ator.getNome() + ".");
			return;
		}
		int startX = ator.getPosX();
		int startY = ator.getPosY();
		int maxDist = ator.getMovimentoRestanteTurno();
		System.out.println("DEBUG: Calculando alcance de MOVIMENTO (BFS) de " + maxDist);

		calcularAlcanceBFS(startX, startY, maxDist, CSS_ALCANCE_MOVIMENTO);
	}

	private void calcularEExibirAtaqueRange(Personagem ator, Habilidade habilidade) {
		limparDestaquesAlcance();
		if (habilidade != null && habilidade.getTipoAlvo() == TipoAlvo.SI_MESMO)
			return;
		int maxDist = obterAlcanceEfetivo(ator, habilidade);

		System.out.println("DEBUG: Alcance Efetivo Calculado: " + maxDist);

		String cssClass = (maxDist > 1) ? CSS_ALCANCE_ATAQUE_RANGED : CSS_ALCANCE_ATAQUE_MELEE;
		calcularAlcanceBFS(ator.getPosX(), ator.getPosY(), maxDist, cssClass);
	}

	private void desenharPreviewCone(int cursorX, int cursorY) {
		// Reutiliza a matemática já existente em encontrarAlvosNaForma
		List<Personagem> alvos = encontrarAlvosNaForma(cursorX, cursorY, habilidadeAtual, atorAtual);

		// Apenas destaca os personagens (borda vermelha) e avisa o controle
		destacarPeoesAlvo(alvos);
		mainController.alvosIdentificadosNoMapa(alvos);
	}

	private void desenharPreviewQuadrado(int x, int y) {
		List<Personagem> alvos = encontrarAlvosNaForma(x, y, habilidadeAtual, atorAtual);
		destacarPeoesAlvo(alvos);
		mainController.alvosIdentificadosNoMapa(alvos);
	}

	private void desenharPreviewCircular(int x, int y) {
		// A lógica Manhattan está dentro de encontrarAlvosNaForma!
		List<Personagem> alvos = encontrarAlvosNaForma(x, y, habilidadeAtual, atorAtual);
		destacarPeoesAlvo(alvos);
		mainController.alvosIdentificadosNoMapa(alvos);
	}

	private void desenharPreviewLinha(int cursorX, int cursorY) {
		List<Personagem> alvos = encontrarAlvosNaForma(cursorX, cursorY, habilidadeAtual, atorAtual);
		destacarPeoesAlvo(alvos);
		mainController.alvosIdentificadosNoMapa(alvos);
	}

	private int calcularDistancia(int startX, int startY, int endX, int endY) {
		if (paredesGrid[endX][endY])
			return -1; // Não pode mirar dentro de uma parede

		Queue<Pair<Integer, Integer>> fila = new LinkedList<>();
		int[][] distancias = new int[gridLargura][gridAltura];
		for (int i = 0; i < gridLargura; i++)
			java.util.Arrays.fill(distancias[i], -1);
		fila.add(new Pair<>(startX, startY));
		distancias[startX][startY] = 0;
		int[] dx = { 0, 0, 1, -1 };
		int[] dy = { 1, -1, 0, 0 }; // Movimento ortogonal (ou adicione diagonais se quiser)

		while (!fila.isEmpty()) {
			Pair<Integer, Integer> atual = fila.poll();
			int x = atual.getKey();
			int y = atual.getValue();
			if (x == endX && y == endY)
				return distancias[x][y];

			for (int i = 0; i < 4; i++) {
				int novoX = x + dx[i];
				int novoY = y + dy[i];
				if (novoX >= 0 && novoX < gridLargura && novoY >= 0 && novoY < gridAltura) {
					if (!paredesGrid[novoX][novoY] && distancias[novoX][novoY] == -1) {
						distancias[novoX][novoY] = distancias[x][y] + 1;
						fila.add(new Pair<>(novoX, novoY));
					}
				}
			}
		}
		return -1;
	}

	public List<Personagem> encontrarAlvosNaForma(int centroX, int centroY, Habilidade habilidade, Personagem ator) {
		List<Personagem> alvosEncontrados = new ArrayList<>();
		boolean atravessaParedes = habilidade.ignoraParedes();
		TipoAlvo tipo = habilidade.getTipoAlvo();

		// Coleta de Células: Encontra todas as CÉLULAS VÁLIDAS na forma
		Set<Pane> celulasDaForma = new HashSet<>();

		if (tipo == TipoAlvo.AREA) {
			int raio = (habilidade.getTamanhoArea()) / 2;
			int px = ator.getPosX();
			int py = ator.getPosY();

			// Varre ao redor do ATOR, ignorando o centroX/Y do clique do mouse
			for (int y = py - raio; y <= py + raio; y++) {
				for (int x = px - raio; x <= px + raio; x++) {
					// Distância Manhattan
					if (Math.abs(x - px) + Math.abs(y - py) <= raio) {
						coletarCelula(x, y, atravessaParedes, celulasDaForma);
					}
				}
			}
		} else if (tipo == TipoAlvo.AREA_CIRCULAR) {
			int raio = (habilidade.getTamanhoArea()) / 2;
			for (int y = centroY - raio; y <= centroY + raio; y++) {
				for (int x = centroX - raio; x <= centroX + raio; x++) {
					if (Math.abs(x - centroX) + Math.abs(y - centroY) <= raio) {
						coletarCelula(x, y, atravessaParedes, celulasDaForma);
					}
				}
			}
		}

		else if (tipo == TipoAlvo.AREA_QUADRADA) {
			int tamanho = habilidade.getTamanhoArea();
			int raio = (tamanho - 1) / 2;
			for (int y = centroY - raio; y <= centroY + raio; y++) {
				for (int x = centroX - raio; x <= centroX + raio; x++) {
					coletarCelula(x, y, atravessaParedes, celulasDaForma);
				}
			}
		}

		else if (tipo == TipoAlvo.LINHA) {
			int comprimento = habilidade.getAlcanceMaximo();
			int largura = habilidade.getTamanhoArea();
			int raioLargura = (largura - 1) / 2;
			int px = ator.getPosX();
			int py = ator.getPosY();

			// centroX/Y é a posição do cursor no momento do clique
			int deltaX = Math.abs(centroX - px);
			int deltaY = Math.abs(centroY - py);

			if (deltaX > deltaY) { // Horizontal
				int dirX = (centroX > px) ? 1 : -1;
				for (int i = 1; i <= comprimento; i++) {
					int currentX = px + (i * dirX);
					for (int j = -raioLargura; j <= raioLargura; j++) {
						int currentY = py + j;
						if (!coletarCelula(currentX, currentY, atravessaParedes, celulasDaForma) && !atravessaParedes) {
							i = comprimento;
							break;
						}
					}
				}
			} else { // Vertical
				int dirY = (centroY > py) ? 1 : -1;
				for (int i = 1; i <= comprimento; i++) {
					int currentY = py + (i * dirY);
					for (int j = -raioLargura; j <= raioLargura; j++) {
						int currentX = px + j;
						if (!coletarCelula(currentX, currentY, atravessaParedes, celulasDaForma) && !atravessaParedes) {
							i = comprimento;
							break;
						}
					}
				}
			}
		} else if (tipo == TipoAlvo.CONE) {
			int alcance = habilidade.getAlcanceMaximo();
			double anguloHabilidadeMetade = Math.toRadians(habilidade.getAnguloCone() / 2.0);
			int px = ator.getPosX();
			int py = ator.getPosY();

			// Ângulo base do clique do mouse
			double anguloCentralMouse = Math.atan2(centroY - py, centroX - px + 0.0001);

			// Pega os desvios da habilidade (Ex: -30, 0, +30)
			List<Integer> desvios = habilidade.getAngulosDesvio();

			// Itera no "quadrado" de alcance (deveria se Otimização mas aumentou em 50MB o consumo por algumo motivo rs)
			for (int y = py - alcance; y <= py + alcance; y++) {
				for (int x = px - alcance; x <= px + alcance; x++) {
					if (x == px && y == py)
						continue;
					if (!dentroDoGrid(x, y))
						continue; 

					int dist = Math.max(Math.abs(x - px), Math.abs(y - py));
					if (dist > alcance)
						continue; // Fora do alcance máximo

					// Ângulo da célula atual em relação ao ator
					double anguloCelula = Math.atan2(y - py, x - px + 0.0001);

					// Verifica se a célula está dentro de PELO MENOS UM dos cones
					boolean acertou = false;

					for (int desvioGraus : desvios) {
						double desvioRad = Math.toRadians(desvioGraus);
						double anguloCentralAjustado = anguloCentralMouse + desvioRad;

						double diff = anguloCelula - anguloCentralAjustado;
						// Normaliza entre -PI e PI
						if (diff > Math.PI)
							diff -= (2 * Math.PI);
						if (diff < -Math.PI)
							diff += (2 * Math.PI);

						// Verifica tolerância angular
						if (Math.abs(diff) <= anguloHabilidadeMetade) {
							acertou = true;
							break; // Já está dentro de um cone, não precisa ver os outros
						}
					}

					if (acertou) {
						if (atravessaParedes || temLinhaDeVisao(px, py, x, y)) {
							coletarCelula(x, y, atravessaParedes, celulasDaForma);
						}
					}
				}
			}
		}

		if (mainController != null) {
			for (Personagem p : mainController.getCombatentes()) {

				boolean alvoValido = p.isAtivoNoCombate();
				if (!alvoValido && p instanceof br.com.dantesrpg.model.elementos.ObjetoDestrutivel) {
					alvoValido = ((br.com.dantesrpg.model.elementos.ObjetoDestrutivel) p).isIntacto();
				}
				if (!alvoValido)
					continue;

				if (personagemIntersecaForma(p, celulasDaForma)) {
						boolean isAliado = p.getFaccao().equals(ator.getFaccao());
						if (p == ator && !habilidade.afetaSiMesmo())
							continue;
						if (isAliado && !habilidade.afetaAliados())
							continue;
						if (!isAliado && !habilidade.afetaInimigos() && !p.getFaccao().equals("OBJETO"))
							continue;
						if (ator.isClone() && p.isClone()) {
							if (ator.getCriador() == p.getCriador()) {
								// São irmãos de invocação. Não pode atacar.
								continue;
							}
						}
						alvosEncontrados.add(p);
				}
			}
		}
		return alvosEncontrados;
	}

	private boolean personagemIntersecaForma(Personagem personagem, Set<Pane> celulasDaForma) {
		if (personagem == null || celulasDaForma == null || celulasDaForma.isEmpty())
			return false;

		for (int y = personagem.getPosY(); y < personagem.getPosY() + personagem.getTamanhoY(); y++) {
			for (int x = personagem.getPosX(); x < personagem.getPosX() + personagem.getTamanhoX(); x++) {
				if (!dentroDoGrid(x, y))
					continue;

				if (celulasDaForma.contains(celulasDoGrid[x][y])) {
					return true;
				}
			}
		}

		return false;
	}

	public void sairModoSelecao() {
		limparCanvas();
		limparDestaquesGridAtaque();
		limparDestaquesPeoes();
		this.modoSelecaoAlvo = false;
		this.habilidadeAtual = null;
		this.atorAtual = null;
		System.out.println("MAPA: Saindo do modo de seleção.");

		removerLabelContador(); // Apenas remove o texto

		limparDestaquesAlcance();

		if (mapGrid.getScene() != null) {
			mapGrid.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
		}

		atualizarBotaoPularSquad();
	}

	public void toggleModoMovimentoLivre() {
		this.modoMovimentoLivre = !this.modoMovimentoLivre; // Inverte o estado

		if (this.modoMovimentoLivre) {
			System.out.println("MAPA: Modo Movimento Livre ATIVADO.");
			mapGrid.getScene().setCursor(javafx.scene.Cursor.OPEN_HAND);

			// Cancela qualquer seleção de combate pendente
			sairModoSelecao();

		} else {
			System.out.println("MAPA: Modo Movimento Livre DESATIVADO.");
			// Volta o cursor ao normal
			mapGrid.getScene().setCursor(javafx.scene.Cursor.DEFAULT);

			// "Solta" qualquer peão que estava sendo movido
			this.peaoSelecionadoParaMover = null;
		}
	}

	public void desenharRingueAlexei(Personagem centro, int tamanho) {
		limparRingueAlexei(); // Limpa qualquer ringue antigo

		// Calcula o raio (para 7x7, tamanho=7, raio=3)
		int raio = (tamanho - 1) / 2;

		System.out.println("MAPA: Desenhando Ringue " + tamanho + "x" + tamanho + " centrado em " + centro.getNome());
		// Itera do (centro - raio) até (centro + raio)
		for (int y = centro.getPosY() - raio; y <= centro.getPosY() + raio; y++) {
			for (int x = centro.getPosX() - raio; x <= centro.getPosX() + raio; x++) {
				// Verifica se a célula está dentro do grid
				if (x >= 0 && x < gridLargura && y >= 0 && y < gridAltura) {
					Pane celulaDoGrid = celulasDoGrid[x][y];
					if (celulaDoGrid != null) {
						celulaDoGrid.getStyleClass().add("zona-dominio-alexei");
						celulasRingueAlexei.add(celulaDoGrid); // Rastreia para limpar
					}
				}
			}
		}
	}

	public void limparRingueAlexei() {
		if (celulasRingueAlexei.isEmpty())
			return;

		System.out.println("MAPA: Limpando Ringue do Alexei.");
		for (Pane cell : celulasRingueAlexei) {
			cell.getStyleClass().remove("zona-dominio-alexei");
		}
		celulasRingueAlexei.clear();
	}

	public void desenharDominioLyria(Personagem centro, int tamanhoIgnorado) {
		limparDominioLyria();

		// Regra: 2 quadrados para cada lado (Centro + 2 Esq + 2 Dir = 5x5)
		int raio = 2;

		System.out.println("MAPA: Desenhando Cassino 5x5 centrado em " + centro.getNome());
		for (int y = centro.getPosY() - raio; y <= centro.getPosY() + raio; y++) {
			for (int x = centro.getPosX() - raio; x <= centro.getPosX() + raio; x++) {
				if (x >= 0 && x < gridLargura && y >= 0 && y < gridAltura) {
					Pane celulaDoGrid = celulasDoGrid[x][y];
					if (celulaDoGrid != null) {
						celulaDoGrid.getStyleClass().add("zona-dominio-lyria");
						celulasDominioLyria.add(celulaDoGrid);
					}
				}
			}
		}
	}

	private boolean isCelulaNoDominio(int x, int y, List<Pane> listaDominio) {
		if (listaDominio.isEmpty())
			return false;
		if (x < 0 || x >= gridLargura || y < 0 || y >= gridAltura)
			return false;
		return listaDominio.contains(celulasDoGrid[x][y]);
	}

	public void limparDominioLyria() {
		if (celulasDominioLyria.isEmpty())
			return;
		System.out.println("MAPA: Limpando Domínio da Lyria.");
		for (Pane cell : celulasDominioLyria) {
			cell.getStyleClass().remove("zona-dominio-lyria");
		}
		celulasDominioLyria.clear();
	}

	public void desenharAuraDarrell(Personagem centro) {
		// Limpa anterior
		for (Pane cell : celulasAuraDarrell) {
			cell.getStyleClass().remove("zona-aura-darrell");
		}
		celulasAuraDarrell.clear();

		// Desenha nova (Raio 3 = 6x6 aproximado, ou 7x7 centrado. Vamos usar 7x7 para ficar simétrico com o centro)
		int raio = 3;

		for (int y = centro.getPosY() - raio; y <= centro.getPosY() + raio; y++) {
			for (int x = centro.getPosX() - raio; x <= centro.getPosX() + raio; x++) {
				if (x >= 0 && x < gridLargura && y >= 0 && y < gridAltura) {
					Pane celulaDoGrid = celulasDoGrid[x][y];
					if (celulaDoGrid != null) {
						celulaDoGrid.getStyleClass().add("zona-aura-darrell");
						celulasAuraDarrell.add(celulaDoGrid);
					}
				}
			}
		}
	}

	private int obterAlcanceEfetivo(Personagem ator, Habilidade habilidade) {
		Arma arma = ator.getArmaEquipada();
		int alcanceArma = (arma != null) ? arma.getAlcance() : 1;
		int alcanceFinal = alcanceArma;
		if (habilidade != null) {
			int alcanceHab = habilidade.getAlcanceMaximo();
			if (alcanceHab != -1) {
				// Regra: Max(Arma, Habilidade)
				alcanceFinal = Math.max(alcanceHab, alcanceArma);
			}
		}
		return alcanceFinal;
	}

	private boolean coletarCelula(int x, int y, boolean atravessaParedes, Set<Pane> celulasColetadas) {
		if (x >= 0 && x < gridLargura && y >= 0 && y < gridAltura) {
			Pane cell = celulasDoGrid[x][y];
			if (cell != null) {
				if (paredesGrid[x][y] && !atravessaParedes) {
					Personagem p = getPersonagemNaCelula(x, y);
					boolean isObjeto = (p instanceof br.com.dantesrpg.model.elementos.ObjetoDestrutivel);
					if (!isObjeto) {
						return false;
					}
				}

				celulasColetadas.add(cell);
				return true;
			}
		}
		return false;
	}

	private void desenharBarrasDeVidaObjetos(List<Personagem> combatentes) {
		// Limpa barras antigas de todas as células (removemos qualquer Node filho que seja barra)
		for (int x = 0; x < gridLargura; x++) {
			for (int y = 0; y < gridAltura; y++) {
				Pane cell = celulasDoGrid[x][y];
				if (cell != null) {
					cell.getChildren().removeIf(node -> node.getStyleClass().contains("obj-hp-bar"));
				}
			}
		}

		for (Personagem p : combatentes) {
			// Verifica se é objeto e se está danificado
			if (p instanceof br.com.dantesrpg.model.elementos.ObjetoDestrutivel) {
				if (p.getVidaAtual() < p.getVidaMaxima() && p.isVivo()) {
					Pane cell = celulasDoGrid[p.getPosX()][p.getPosY()];
					if (cell != null) {
						double pct = p.getVidaAtual() / p.getVidaMaxima();

						// Fundo da barra (Vermelho escuro)
						javafx.scene.shape.Rectangle bgBar = new javafx.scene.shape.Rectangle(5, 5, CELL_SIZE - 10, 6);
						bgBar.setFill(Color.DARKRED);
						bgBar.getStyleClass().add("obj-hp-bar");

						// Frente da barra (Verde/Amarelo/Vermelho dependendo do dano)
						javafx.scene.shape.Rectangle hpBar = new javafx.scene.shape.Rectangle(5, 5,
								(CELL_SIZE - 10) * pct, 6);
						if (pct > 0.5)
							hpBar.setFill(Color.LIME);
						else if (pct > 0.25)
							hpBar.setFill(Color.ORANGE);
						else
							hpBar.setFill(Color.RED);
						hpBar.getStyleClass().add("obj-hp-bar");

						cell.getChildren().addAll(bgBar, hpBar);
					}
				}
			}
		}
	}

	public void entrarModoSpawn(String idMonstro, int quantidade) {
		this.modoSpawnInimigo = true;
		this.idMonstroEmSpawn = idMonstro;
		this.cargasSpawnRestantes = quantidade;

		sairModoSelecao();
		mapGrid.getScene().setCursor(javafx.scene.Cursor.CLOSED_HAND);
		System.out.println("MAPA: Modo Spawn Ativo (" + quantidade + " cargas) para: " + idMonstro);
	}

	public br.com.dantesrpg.model.map.MapMetadata extrairMetadados() {
		br.com.dantesrpg.model.map.MapMetadata meta = new br.com.dantesrpg.model.map.MapMetadata();

		// Escaneia o Grid (Paredes, Saídas, Objetos)
		for (int x = 0; x < gridLargura; x++) {
			for (int y = 0; y < gridAltura; y++) {
				Pane cell = celulasDoGrid[x][y];
				if (cell.getStyleClass().contains("map-wall")) {
					meta.getParedes().add(new br.com.dantesrpg.model.map.MapMetadata.Ponto(x, y));
				} else if (cell.getStyleClass().contains("map-exit")) {
					meta.getSaidas().add(new br.com.dantesrpg.model.map.MapMetadata.Ponto(x, y));
				} else if (cell.getStyleClass().contains("map-object-light")) {
					// Salva a posição do objeto (HP padrão por enquanto)
					meta.getObjetos().add(new br.com.dantesrpg.model.map.MapMetadata.ObjetoData(x, y, 50));
				}
			}
		}

		// Escaneia Inimigos Atuais (Para criar o Preset)
		if (mainController != null) {
			for (Personagem p : mainController.getCombatentes()) {
				// Salva apenas INIMIGOS e que NÃO sejam Objetos Destrutíveis 
				if (!mainController.isPlayer(p) && !(p instanceof br.com.dantesrpg.model.elementos.ObjetoDestrutivel)) {
					// Tenta extrair o ID original do nome (Ex: "Demônio Menor 1" -> "DemonioMenor")
					// Isso é complexo se não guardarmos o ID original.
					
					// TRUQUE: Vou salvar o nome base limpo e torcer para bater com o bestiário, ou idealmente, adicionar um campo 'idOriginal' no Personagem. (muita FÉ)

					// usando um método auxiliar de limpeza de nome
					String idEstimado = p.getNome().replaceAll("\\s\\d+$", ""); // Remove numero final
					// Remove espaços para tentar bater com chaves como "DemonioMenor" se precisar

					meta.getInimigos().add(new br.com.dantesrpg.model.map.MapMetadata.InimigoSpawn(p.getPosX(),
							p.getPosY(), idEstimado));
				}
			}
		}

		return meta;
	}

	public void iniciarSelecaoSquad(List<Personagem> clones, Habilidade habilidade, int rolagem) {
		this.modoSquad = true;
		this.modoSelecaoAlvo = true;
		this.habilidadeAtual = habilidade;
		this.rolagemSquadGlobal = rolagem;
		this.filaClonesSquad.clear();
		this.ataquesDeclaradosSquad.clear();
		this.filaClonesSquad.addAll(clones);
		atualizarBotaoPularSquad();

		// Inicia o primeiro
		prepararProximoCloneSquad();
	}

	private void prepararProximoCloneSquad() {
		while (!filaClonesSquad.isEmpty()) {
			Personagem proximo = filaClonesSquad.peek();
			if (proximo != null && proximo.isAtivoNoCombate()) {
				break;
			}
			filaClonesSquad.poll();
		}

		if (filaClonesSquad.isEmpty()) {
			finalizarSquad();
			return;
		}

		// Pega o próximo da fila
		this.atorAtual = filaClonesSquad.peek();

		System.out.println("MAPA (SQUAD): Vez de " + atorAtual.getNome());
		atualizarBotaoPularSquad();
		prepararMiraParaCloneAtual();
	}

	private void prepararMiraParaCloneAtual() {
		if (atorAtual == null)
			return;

		this.modoSelecaoAlvo = true;
		toggleMover.setSelected(false);
		toggleMirar.setSelected(true);

		limparCanvas();
		limparDestaquesAlcance();
		calcularEExibirAtaqueRange(atorAtual, habilidadeAtual);

		if (mapGrid.getScene() != null) {
			mapGrid.getScene().setCursor(javafx.scene.Cursor.CROSSHAIR);
		}
	}

	private void tratarCliqueSquad(Pane cell, int x, int y) {
		if (toggleMover.isSelected() && atorAtual.getRaca() != null && !atorAtual.getRaca().podeSeMover(atorAtual)) {
			System.out.println("SQUAD: " + atorAtual.getNome() + " nao pode se mover enquanto estiver em postura.");
			return;
		}
		// CLONE ATUAL: atorAtual

		// --- LÓGICA DE MOVIMENTO (Se toggleMover estiver ativo) ---
		if (toggleMover.isSelected()) {
			// Verifica se clicou em uma célula válida de movimento (Azul)
			if (celulasAlcanceMovimento.contains(cell)) {

				// Verifica se a célula está vazia (não pode andar em cima de outro)
				// (getPersonagemNaCelula retorna null se vazio)
				if (getPersonagemNaCelula(x, y) == null) {
					int dist = calcularDistancia(atorAtual.getPosX(), atorAtual.getPosY(), x, y);

					if (dist != -1 && dist <= atorAtual.getMovimentoRestanteTurno()) {
						// Move
						atorAtual.setPosX(x);
						atorAtual.setPosY(y);
						atorAtual.setMovimentoRestanteTurno(atorAtual.getMovimentoRestanteTurno() - dist);

						// Atualiza Visual
						desenharPeoes(mainController.getCombatentes());

						// Recalcula alcance a partir da nova posição
						calcularEExibirMovimento(atorAtual);
						return;
					}
				}
			}
		}

		// --- LÓGICA DE ATAQUE ---
		if (toggleMirar.isSelected()) {
			// Verifica se a célula está no alcance de ataque (Vermelho)
			if (celulasAlcanceMovimento.contains(cell)) {

				Personagem alvo = getPersonagemNaCelula(x, y);

				// Clicou em um Inimigo/Objeto Válido
				if (alvo != null && !alvo.equals(atorAtual)) {
					// Validação Fogo Amigo (Clone vs Clone do mesmo dono)
					if (alvo.isClone() && alvo.getCriador() == atorAtual.getCriador()) {
						System.out.println("SQUAD: Fogo amigo bloqueado.");
						return;
					}

					ataquesDeclaradosSquad.put(atorAtual, alvo);
					System.out.println("SQUAD: " + atorAtual.getNome() + " travou mira em " + alvo.getNome());

					filaClonesSquad.poll();
					prepararProximoCloneSquad();
				}
			}
		}
	}

	private void finalizarSquad() {
		Map<Personagem, Personagem> ataquesFinalizados = new HashMap<>(ataquesDeclaradosSquad);
		this.modoSquad = false;
		this.filaClonesSquad.clear();
		atualizarBotaoPularSquad();
		sairModoSelecao();
		mainController.retornarDoSquadComAlvos(ataquesFinalizados);
	}

	@FXML
	private void onPularSquadClick() {
		if (!modoSquad)
			return;

		System.out.println("SQUAD: Seleção encerrada manualmente. Clones restantes serão pulados.");
		finalizarSquad();
	}

	public javafx.util.Pair<Integer, Integer> encontrarCelulaLivreMaisProxima(int centroX, int centroY) {
		int raioMaximo = 3;

		for (int r = 1; r <= raioMaximo; r++) {
			// Itera no quadrado ao redor do centro com raio 'r'
			for (int x = centroX - r; x <= centroX + r; x++) {
				for (int y = centroY - r; y <= centroY + r; y++) {

					// Verifica limites do grid
					if (!dentroDoGrid(x, y))
						continue;

					// Ignora o próprio centro (onde está o invocador)
					if (x == centroX && y == centroY)
						continue;

					// Verifica Parede
					if (paredesGrid[x][y])
						continue;

					// Verifica Personagem/Objeto existente
					if (getPersonagemNaCelula(x, y) != null)
						continue;

					// Se passou por tudo, está livre! Retorna imediatamente a primeira que achar.
					return new javafx.util.Pair<>(x, y);
				}
			}
		}

		System.err.println("MAPA: Não foi possível encontrar célula livre próxima a (" + centroX + "," + centroY + ")");
		return null;
	}

	@FXML
	private void onSalvarMapaClick() {
		if (mainController != null) {
			// Chama o método de salvamento que já existe no MainController
			mainController.onSalvarMapaJsonClick();
		}
	}

	public void aplicarMetadados(br.com.dantesrpg.model.map.MapMetadata meta) {
		System.out.println("MAPA: Aplicando metadados salvos...");
		// Limpa tudo primeiro
		for (int x = 0; x < gridLargura; x++) {
			for (int y = 0; y < gridAltura; y++) {
				// Reseta para chão
				Pane cell = celulasDoGrid[x][y];
				cell.getStyleClass().removeAll("map-wall", "map-exit", "map-object-light", "map-floor");
				cell.getStyleClass().add("map-floor");
				paredesGrid[x][y] = false;
				mainController.removerObjetoNoMapa(x, y);
			}
		}

		// Aplica Paredes
		for (br.com.dantesrpg.model.map.MapMetadata.Ponto p : meta.getParedes()) {
			if (dentroDoGrid(p.x, p.y)) {
				celulasDoGrid[p.x][p.y].getStyleClass().add("map-wall");
				paredesGrid[p.x][p.y] = true;
			}
		}

		// Aplica Saídas
		for (br.com.dantesrpg.model.map.MapMetadata.Ponto p : meta.getSaidas()) {
			if (dentroDoGrid(p.x, p.y)) {
				celulasDoGrid[p.x][p.y].getStyleClass().add("map-exit");
			}
		}

		// Aplica Objetos (Visual + Lógico)
		for (br.com.dantesrpg.model.map.MapMetadata.ObjetoData obj : meta.getObjetos()) {
			if (dentroDoGrid(obj.x, obj.y)) {
				celulasDoGrid[obj.x][obj.y].getStyleClass().add("map-object-light");
				paredesGrid[obj.x][obj.y] = true;
				// Cria a entidade lógica com HP
				mainController.criarObjetoNoMapa(obj.x, obj.y);
			}
		}

		// Aplica Inimigos (Spawn)
		for (br.com.dantesrpg.model.map.MapMetadata.InimigoSpawn spawn : meta.getInimigos()) {
			if (dentroDoGrid(spawn.x, spawn.y)) {
				// Tenta encontrar o ID no bestiário
				mainController.spawnarMonstro(spawn.idMonstro, spawn.x, spawn.y);
			}
		}
	}

	private boolean dentroDoGrid(int x, int y) {
		return x >= 0 && x < gridLargura && y >= 0 && y < gridAltura;
	}

	public br.com.dantesrpg.model.map.TerrainData.EfeitoInstance getEfeitoNoSolo(int x, int y) {
		if (!dentroDoGrid(x, y))
			return null;
		return gridEfeitos[x][y];
	}

	public boolean isModoMovimentoLivre() {
		return this.modoMovimentoLivre;
	}

	private void limparHitboxesExtras() {
		for (int x = 0; x < gridLargura; x++) {
			for (int y = 0; y < gridAltura; y++) {
				if (celulasDoGrid[x][y] != null) {
					celulasDoGrid[x][y].getStyleClass().remove("enemy-hitbox-extra");
				}
			}
		}
	}

}
