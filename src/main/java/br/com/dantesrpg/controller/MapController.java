package br.com.dantesrpg.controller;

import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.map.CoordenadaMapa;
import br.com.dantesrpg.model.util.ImageCache;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.shape.ArcType;
import javafx.scene.paint.Color;
import java.util.List;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import br.com.dantesrpg.model.map.TerrainData;
import br.com.dantesrpg.model.map.TerrainData.*;
import br.com.dantesrpg.model.map.TileDefinition;
import br.com.dantesrpg.model.map.TileRegistry;
import br.com.dantesrpg.model.map.Dominio;

import java.util.HashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;
import javafx.scene.input.MouseEvent;

import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import br.com.dantesrpg.controller.map.AoEShapeCalculator;
import br.com.dantesrpg.controller.map.MapTokenRenderer;
import br.com.dantesrpg.controller.map.SquadModeHandler;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javax.imageio.ImageIO;
import java.io.FileInputStream;

public class MapController {
    private br.com.dantesrpg.model.map.EstadoMapa estadoMapa = new br.com.dantesrpg.model.map.EstadoMapa();
    private final Consumer<CoordenadaMapa> ouvinteMapa = this::sincronizarEstadoMapa;
    private br.com.dantesrpg.controller.map.RenderizadorMapa2D renderizador2D;
    private br.com.dantesrpg.controller.map.RenderizadorMapa3D renderizador3D;
    private boolean visualizacao3D;
    private ToggleButton alternarVisualizacao;
    private final List<Node> controlesElevados=new ArrayList<>();
    private boolean sincronizandoCelulas;
    private boolean sincronizandoInteracao;
    private javafx.stage.Window janelaObservada;
    private final javafx.beans.value.ChangeListener<Boolean> ouvinteVisibilidade = (o,a,b) -> atualizarVisibilidadeJanela();
    private final Map<CoordenadaMapa, EfeitoInstance> efeitosDesenhados = new HashMap<>();
    private CoordenadaMapa cursorGrade;
    private final Set<CoordenadaMapa> previewAtual = new LinkedHashSet<>();


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
	private StackPane mapViewport;
	@FXML
	private StackPane mapContent;
	@FXML
	private ToggleButton btnMovimentoLivre;
	@FXML
	private ToggleButton btnEditorMapa;
	@FXML
	private Button btnPularSquad;
	@FXML
	private HBox floatingToggleBox;

	private double zoomLevel = 1.0;
	private static final double ZOOM_MIN = 0.3;
	private static final double ZOOM_MAX = 3.0;
	private static final double ZOOM_STEP = 0.1;
	private double dragAnchorX;
	private double dragAnchorY;
	private double dragAnchorHvalue;
	private double dragAnchorVvalue;
	private boolean dragOcorreu = false;

	private CombatController mainController;

	private int gridLargura;
	private int gridAltura;
	private final int CELL_SIZE = 40;

	private boolean modoSelecaoAlvo = false;
	private Habilidade habilidadeAtual;
	private boolean modoSelecaoMultipla = false;
	private int alvosRestantes;
	private List<Personagem> alvosSelecionadosMulti = new ArrayList<>();
	private boolean modoSelecaoMultiAoE = false;
	private int areasRestantes;
	private final List<AcaoMestreInput.AreaSelecionada> areasSelecionadasMultiAoE = new ArrayList<>();
	private Label labelContadorAlvos;

	private boolean modoMovimentoLivre = false;
	private Personagem peaoSelecionadoParaMover = null;

	// Fallback de modo mover/mirar quando os toggles do toolbar não existem
	// (embedded).
	// Default: mirar (false), idêntico ao FXML externo onde toggleMirar começa
	// selecionado.
	private boolean moverModeEmbedded = false;

	private boolean isMoverMode() {
		return toggleMover != null ? toggleMover.isSelected() : moverModeEmbedded;
	}

	private void setMoverMode(boolean mover) {
		if (toggleMover != null)
			toggleMover.setSelected(mover);
		if (toggleMirar != null)
			toggleMirar.setSelected(!mover);
		this.moverModeEmbedded = mover;
	}

	private boolean modoSpawnInimigo = false;
	private String idMonstroEmSpawn = null;
	private boolean modoSelecaoExplosaoAmbiental = false;
	private Consumer<PontoMapa> aoSelecionarExplosaoAmbiental;
	private Runnable aoCancelarSelecaoExplosaoAmbiental;

	private boolean modoEditor = false;
	private TileDefinition editorTile = null;
	private javafx.stage.Stage editorWindowStage = null;

	// Helpers (criados em initialize() e em inicializarHelpers())
	private AoEShapeCalculator aoeCalc;
	private MapTokenRenderer tokenRenderer;
	private SquadModeHandler squadHandler;

	private Personagem atorAtual;
	private Set<CoordenadaMapa> celulasAlcanceMovimento = new HashSet<>();

	private Pane[][] celulasDoGrid = new Pane[gridLargura][gridAltura];
	private boolean[][] paredesGrid = new boolean[gridLargura][gridAltura];

	private TipoTerreno[][] gridTerreno;
	private EfeitoInstance[][] gridEfeitos;
	private boolean sobreposicaoAguaTempestadeAtiva;
	private static final String ID_SOBREPOSICAO_AGUA_TEMPESTADE = "tempestade-agua-layer";

	private final String CSS_ALCANCE_MOVIMENTO = "movimento-alcance";
	private final String CSS_ALCANCE_ATAQUE_MELEE = "ataque-alcance-melee";
	private final String CSS_ALCANCE_ATAQUE_RANGED = "ataque-alcance-ranged";

	// Sistema genérico de domínios
	private final Map<String, Dominio> dominiosAtivos = new HashMap<>();
	private final Map<String, List<Pane>> celulasVisuaisDominios = new HashMap<>();

	private int cargasSpawnRestantes = 0;
	private List<Pane> celulasGuardiaoDestacadas = new ArrayList<>();
	private List<Pane> celulasVigiliaDestacadas = new ArrayList<>();
	private List<Pane> celulasHarmoniaDestacadas = new ArrayList<>();

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
		configurarZoomEPan();

		squadHandler = new SquadModeHandler(this, aoeCalc,
				toggleMover, toggleMirar, celulasAlcanceMovimento);

		mapGrid.setOnMouseClicked(event -> {
			if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
				if (modoSpawnInimigo) {
					System.out.println("GM: Modo SPAWN cancelado.");
					mainController.notifySpawnConcluido();
					event.consume();
				}
			}
		});

		if (mapViewport != null) {
			javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
			clip.widthProperty().bind(mapViewport.widthProperty());
			clip.heightProperty().bind(mapViewport.heightProperty());
			mapViewport.setClip(clip);
		}
        inicializarRenderizadores();
        estadoMapa.observar(ouvinteMapa);

	}

	private void configurarZoomEPan() {
		if (mapViewport == null || mapContent == null) return;

		// Zoom com Ctrl + Scroll
		mapViewport.addEventFilter(ScrollEvent.SCROLL, event -> {
			if (!visualizacao3D && event.isControlDown()) {
				event.consume();
				double delta = event.getDeltaY();
				
				double oldZoom = zoomLevel;
				if (delta > 0) {
					zoomLevel = Math.min(ZOOM_MAX, zoomLevel + ZOOM_STEP);
				} else {
					zoomLevel = Math.max(ZOOM_MIN, zoomLevel - ZOOM_STEP);
				}
				
				double f = (zoomLevel / oldZoom) - 1;
				
				double dx = (event.getX() - (mapContent.getBoundsInParent().getWidth() / 2 + mapContent.getBoundsInParent().getMinX()));
				double dy = (event.getY() - (mapContent.getBoundsInParent().getHeight() / 2 + mapContent.getBoundsInParent().getMinY()));
				
				mapContent.setScaleX(zoomLevel);
				mapContent.setScaleY(zoomLevel);
				
				mapContent.setTranslateX(mapContent.getTranslateX() - f * dx);
				mapContent.setTranslateY(mapContent.getTranslateY() - f * dy);
			}
		});

		// Drag com botao do meio ou botao esquerdo para pan
		mapViewport.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (visualizacao3D) return;
			if (event.isMiddleButtonDown() || event.isPrimaryButtonDown()) {
				dragAnchorX = event.getSceneX();
				dragAnchorY = event.getSceneY();
				dragAnchorHvalue = mapContent.getTranslateX();
				dragAnchorVvalue = mapContent.getTranslateY();
				dragOcorreu = false;
				if (event.isMiddleButtonDown()) {
					event.consume();
				}
			}
		});

		mapViewport.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (visualizacao3D) return;
			if (event.isMiddleButtonDown() || event.isPrimaryButtonDown()) {
				double deltaX = event.getSceneX() - dragAnchorX;
				double deltaY = event.getSceneY() - dragAnchorY;
				
				if (Math.abs(deltaX) > 3 || Math.abs(deltaY) > 3) {
					dragOcorreu = true;
				}
				
				mapContent.setTranslateX(dragAnchorHvalue + deltaX);
				mapContent.setTranslateY(dragAnchorVvalue + deltaY);
				event.consume();
			}
		});
	}

	public void zoomIn() {
        if (visualizacao3D) { renderizador3D.zoom(.85); return; }
		if (mapContent == null) return;
		zoomLevel = Math.min(ZOOM_MAX, zoomLevel + ZOOM_STEP);
		mapContent.setScaleX(zoomLevel);
		mapContent.setScaleY(zoomLevel);
	}

	public void zoomOut() {
        if (visualizacao3D) { renderizador3D.zoom(1.15); return; }
		if (mapContent == null) return;
		zoomLevel = Math.max(ZOOM_MIN, zoomLevel - ZOOM_STEP);
		mapContent.setScaleX(zoomLevel);
		mapContent.setScaleY(zoomLevel);
	}

	public void resetZoom() {
        if (visualizacao3D) { renderizador3D.restaurar(); return; }
		if (mapContent == null) return;
		zoomLevel = 1.0;
		mapContent.setScaleX(1.0);
		mapContent.setScaleY(1.0);
		mapContent.setTranslateX(0);
		mapContent.setTranslateY(0);
	}

	@FXML
	private void onZoomInClick() {
		zoomIn();
	}

	@FXML
	private void onZoomOutClick() {
		zoomOut();
	}

	@FXML
	private void onZoomResetClick() {
		resetZoom();
	}

	@FXML
	private void onMovimentoLivreClick() {
		if (mainController != null) {
			boolean ativar = !isModoMovimentoLivre();
			mainController.forEachMap(m -> m.toggleModoMovimentoLivre(ativar));
		} else {
			toggleModoMovimentoLivre(!this.modoMovimentoLivre);
		}
	}

	@FXML
	private void onEditorMapaClick() {
		if (mainController != null) {
			boolean ativar = !isModoEditor();
			mainController.forEachMap(m -> m.setModoEditor(ativar));
		} else {
			setModoEditor(!this.modoEditor);
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
        cursorGrade = new CoordenadaMapa((int)(event.getX()/CELL_SIZE),(int)(event.getY()/CELL_SIZE));
		if (modoSelecaoExplosaoAmbiental) {
			desenharPreviewExplosaoAmbiental(event.getX(), event.getY());
			return;
		}
		if (!modoSelecaoAlvo || isMoverMode() || habilidadeAtual == null) {
			limparCanvas();
			return;
		}
		desenharVisualAoE(event.getX(), event.getY());
	}

	private void desenharPreviewExplosaoAmbiental(double mouseX, double mouseY) {
		limparCanvas();
		if (aoeCanvas == null) {
			return;
		}

		int x = (int) (mouseX / CELL_SIZE);
		int y = (int) (mouseY / CELL_SIZE);
		if (!dentroDoGrid(x, y)) {
			return;
		}

		GraphicsContext gc = aoeCanvas.getGraphicsContext2D();
		double raio = CELL_SIZE * 1.5;
		double centroX = x * CELL_SIZE + CELL_SIZE / 2.0;
		double centroY = y * CELL_SIZE + CELL_SIZE / 2.0;
		gc.setFill(Color.rgb(235, 92, 32, 0.32));
		gc.setStroke(Color.rgb(255, 181, 54, 0.92));
		gc.setLineWidth(2);
		gc.fillOval(centroX - raio, centroY - raio, raio * 2, raio * 2);
		gc.strokeOval(centroX - raio, centroY - raio, raio * 2, raio * 2);
	}

	public void limparCanvas() {
        previewAtual.clear();
        if(renderizador3D!=null)renderizador3D.apresentarPreview(List.of(),"aoe",areasSelecionadasMultiAoE.stream().map(AcaoMestreInput.AreaSelecionada::epicentro).toList());
		if (aoeCanvas != null) {
			GraphicsContext gc = aoeCanvas.getGraphicsContext2D();
			gc.clearRect(0, 0, aoeCanvas.getWidth(), aoeCanvas.getHeight());
		}
	}

	private void desenharVisualAoE(double mouseX, double mouseY) {
		limparCanvas();
		if (atorAtual == null)
			return;

        if(aoeCalc!=null)previewAtual.addAll(aoeCalc.calcularForma((int)(mouseX/CELL_SIZE),(int)(mouseY/CELL_SIZE),habilidadeAtual,atorAtual));

		GraphicsContext gc = aoeCanvas.getGraphicsContext2D();
		gc.setFill(Color.rgb(148, 0, 211, 0.3)); // Roxo translúcido
		gc.setStroke(Color.rgb(148, 0, 211, 0.8));
		gc.setLineWidth(2);

		double atorPixelX = (atorAtual.getPosX() * CELL_SIZE) + ((atorAtual.getTamanhoX() * CELL_SIZE) / 2.0);
		double atorPixelY = (atorAtual.getPosY() * CELL_SIZE) + ((atorAtual.getTamanhoY() * CELL_SIZE) / 2.0);

		double alcancePixels = habilidadeAtual.getAlcanceMaximo() * CELL_SIZE + (CELL_SIZE / 2.0);

		TipoAlvo tipo = habilidadeAtual.getTipoAlvoEfetivo();
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
			double larguraLinha = CELL_SIZE * Math.max(1, habilidadeAtual.getTamanhoArea());
			gc.save();
			gc.translate(atorPixelX, atorPixelY);
			double deltaX = mouseX - atorPixelX;
			double deltaY = mouseY - atorPixelY;
			double angle = Math.toDegrees(Math.atan2(deltaY, deltaX));
			gc.rotate(angle);
			gc.fillRect(0, -larguraLinha / 2, alcancePixels, larguraLinha);
			gc.strokeRect(0, -larguraLinha / 2, alcancePixels, larguraLinha);
			gc.restore();

		}

		if (modoSelecaoMultiAoE && !areasSelecionadasMultiAoE.isEmpty()) {
			gc.setFill(Color.rgb(255, 176, 32, 0.9));
			gc.setStroke(Color.rgb(255, 225, 150, 1.0));
			for (AcaoMestreInput.AreaSelecionada area : areasSelecionadasMultiAoE) {
				double centroX = area.epicentro().x() * CELL_SIZE + CELL_SIZE / 2.0;
				double centroY = area.epicentro().y() * CELL_SIZE + CELL_SIZE / 2.0;
				double raioMarcador = Math.max(4.0, CELL_SIZE * 0.16);
				gc.fillOval(centroX - raioMarcador, centroY - raioMarcador,
						raioMarcador * 2, raioMarcador * 2);
				gc.strokeOval(centroX - raioMarcador, centroY - raioMarcador,
						raioMarcador * 2, raioMarcador * 2);
			}
		}
	}

	private void onGridCellMouseEntered(Pane cell, int x, int y) {
		Personagem p = getPersonagemNaCelula(x, y);
		if (p != null && p.getEfeitosAtivos().containsKey("Guardião")) {
			desenharAlcanceGuardiao(x, y);
		} else {
			limparDestaqueGuardiao();
		}

		// Pálida Vigília: mostra alcance de cura (raio 5) ao passar o cursor
		if (p != null && p.getArmaEquipada() instanceof br.com.dantesrpg.model.armas.unicas.PalidaVigilia
				&& !p.getEfeitosAtivos().containsKey("CD: Pálida Vigília")) {
			desenharAlcanceVigilia(p.getPosX(), p.getPosY());
		} else {
			limparDestaqueVigilia();
		}

		// Anjo Caído V2 (Harmonia Perfeita): mostra alcance de ataque 360° (raio 2)
		if (p != null && p.getEfeitosAtivos().containsKey("Τέλεια αρμονία")) {
			desenharAlcanceHarmonia(p.getPosX(), p.getPosY());
		} else {
			limparDestaqueHarmonia();
		}

		if (!modoSelecaoAlvo || isMoverMode() || habilidadeAtual == null) {
			return;
		}

		boolean celularDiretamenteNoAlcance = celulasAlcanceMovimento.contains(new CoordenadaMapa(x, y));
		Personagem hoverTarget = getPersonagemNaCelula(x, y);
		boolean alvoNoAlcance = celularDiretamenteNoAlcance;

		if (!celularDiretamenteNoAlcance && hoverTarget != null) {
			for (int alvoX = hoverTarget.getPosX(); alvoX < hoverTarget.getPosX() + hoverTarget.getTamanhoX(); alvoX++) {
				for (int alvoY = hoverTarget.getPosY(); alvoY < hoverTarget.getPosY() + hoverTarget.getTamanhoY(); alvoY++) {
					if (alvoX >= 0 && alvoX < gridLargura && alvoY >= 0 && alvoY < gridAltura) {
						if (celulasAlcanceMovimento.contains(new CoordenadaMapa(alvoX, alvoY))) {
							alvoNoAlcance = true;
							break;
						}
					}
				}
				if (alvoNoAlcance) break;
			}
		}

		if (!alvoNoAlcance) {
			if (modoSelecaoMultiAoE && !areasSelecionadasMultiAoE.isEmpty()) {
				atualizarPreviewMultiAoE(List.of());
			} else {
				mainController.limparSelecaoDeAlvo();
				limparDestaquesPeoes();
			}
			return;
		}

		switch (habilidadeAtual.getTipoAlvoEfetivo()) {
			case AREA_QUADRADA:
				if (celularDiretamenteNoAlcance) desenharPreviewQuadrado(x, y);
				break;
			case AREA:
			case AREA_CIRCULAR:
				if (celularDiretamenteNoAlcance) desenharPreviewCircular(x, y);
				break;
			case LINHA:
				if (celularDiretamenteNoAlcance) desenharPreviewLinha(x, y);
				break;
			case CONE:
				if (celularDiretamenteNoAlcance) desenharPreviewCone(x, y);
				break;
			default: // INDIVIDUAL ou MULTIPLOS
				if (hoverTarget != null && (!hoverTarget.equals(atorAtual) || (habilidadeAtual != null && habilidadeAtual.afetaSiMesmo()))) {
					List<Personagem> lista = new ArrayList<>();
					lista.add(hoverTarget);
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
						Pane cell = obterCelula(x,y);
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

	// --- PÁLIDA VIGÍLIA: Alcance de cura em área (raio 5) ---
	private void desenharAlcanceVigilia(int centroX, int centroY) {
		limparDestaqueVigilia();
		int raio = 5;
		for (int y = centroY - raio; y <= centroY + raio; y++) {
			for (int x = centroX - raio; x <= centroX + raio; x++) {
				if (dentroDoGrid(x, y)) {
					if (!paredesGrid[x][y] || (x == centroX && y == centroY)) {
						Pane cell = obterCelula(x,y);
						cell.getStyleClass().add("alcance-vigilia");
						celulasVigiliaDestacadas.add(cell);
					}
				}
			}
		}
	}

	private void limparDestaqueVigilia() {
		if (celulasVigiliaDestacadas.isEmpty())
			return;
		for (Pane cell : celulasVigiliaDestacadas) {
			cell.getStyleClass().remove("alcance-vigilia");
		}
		celulasVigiliaDestacadas.clear();
	}

	// --- ANJO CAÍDO V2 (Harmonia Perfeita): Alcance de ataque 360° (raio 2) ---
	private void desenharAlcanceHarmonia(int centroX, int centroY) {
		limparDestaqueHarmonia();
		int raio = 2;
		for (int y = centroY - raio; y <= centroY + raio; y++) {
			for (int x = centroX - raio; x <= centroX + raio; x++) {
				if (dentroDoGrid(x, y)) {
					if (!paredesGrid[x][y] || (x == centroX && y == centroY)) {
						Pane cell = obterCelula(x,y);
						cell.getStyleClass().add("alcance-harmonia");
						celulasHarmoniaDestacadas.add(cell);
					}
				}
			}
		}
	}

	private void limparDestaqueHarmonia() {
		if (celulasHarmoniaDestacadas.isEmpty())
			return;
		for (Pane cell : celulasHarmoniaDestacadas) {
			cell.getStyleClass().remove("alcance-harmonia");
		}
		celulasHarmoniaDestacadas.clear();
	}

	private void onGridCellClicked(MouseEvent event, Pane cell, int x, int y) {
        try { processarCliqueCelula(event,cell,x,y); }
        finally { publicarInteracao(); }
    }

    private void processarCliqueCelula(MouseEvent event, Pane cell, int x, int y) {
		if (dragOcorreu) {
			dragOcorreu = false;
			event.consume();
			return;
		}
		// 1. Botão Direito: Seleciona peão para movimento livre ou cancela seleção
		if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
			if (modoEditor) {
				event.consume();
				return;
			}
			if (modoSelecaoExplosaoAmbiental) {
				cancelarModoSelecaoExplosaoAmbiental();
				event.consume();
				return;
			}
			if (modoSpawnInimigo) {
				System.out.println("GM: Modo SPAWN cancelado via clique na célula.");
				mainController.notifySpawnConcluido();
				event.consume();
				return;
			}

			if (peaoSelecionadoParaMover != null) {
				System.out.println("MAPA (GM): Cancelou movimento livre do peão: " + peaoSelecionadoParaMover.getNome());
				peaoSelecionadoParaMover = null;
				setCombatCursor(javafx.scene.Cursor.DEFAULT);
			} else {
				Personagem alvoNaCelula = getPersonagemNaCelula(x, y);
				if (alvoNaCelula != null) {
					peaoSelecionadoParaMover = alvoNaCelula;
					System.out.println("MAPA (GM): 'Pegou' o peão (botão direito): " + alvoNaCelula.getNome());
					setCombatCursor(javafx.scene.Cursor.CLOSED_HAND);
				} else {
					System.out.println("MAPA (GM): Clique com botão direito em célula vazia.");
				}
			}
			event.consume();
			return;
		}

		// 2. Botão Esquerdo com peão já selecionado para movimento livre: Posiciona
		if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY && peaoSelecionadoParaMover != null) {
			if (tentarCruzarBordaDominio(peaoSelecionadoParaMover, peaoSelecionadoParaMover.getPosX(),
					peaoSelecionadoParaMover.getPosY(), x, y)) {
				event.consume();
				return;
			}
			if (paredesGrid[x][y]) {
				System.out.println("MAPA (GM): Não pode soltar em parede.");
				event.consume();
				return;
			}

			// Move
			peaoSelecionadoParaMover.setPosX(x);
			peaoSelecionadoParaMover.setPosY(y);

			// Atualiza visual
			if (mainController != null) {
				mainController.forEachMap(m -> m.desenharPeoes(mainController.getCombatentes()));
			} else {
				desenharPeoes(getCombatentes());
			}

			// Solta
			System.out.println("MAPA (GM): Soltou " + peaoSelecionadoParaMover.getNome() + " em (" + x + "," + y + ")");
			peaoSelecionadoParaMover = null;
			setCombatCursor(javafx.scene.Cursor.DEFAULT);
			event.consume();
			return;
		}

		// Modos Especiais
		if (modoSelecaoExplosaoAmbiental) {
			if (paredesGrid[x][y]) {
				System.out.println("MAPA: O epicentro da explosão deve ser uma célula transitável.");
				return;
			}
			Consumer<PontoMapa> seletor = aoSelecionarExplosaoAmbiental;
			if (seletor != null) {
				seletor.accept(new PontoMapa(x, y));
			}
			return;
		}

		if (modoEditor) {
			if (editorTile != null) {
				aplicarTileNoEditor(cell, editorTile, x, y);
			} else {
				System.out.println("EDITOR: Nenhum tile selecionado. Abra o catalogo e selecione um tile.");
			}
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
				System.out.println("MAPA: Cargas de spawn esgotadas.");
				// Notifica CombatController para limpar estado global e sincronizar o outro
				// mapa
				mainController.notifySpawnConcluido();
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

			if (tentarCruzarBordaDominio(peaoSelecionadoParaMover, peaoSelecionadoParaMover.getPosX(),
					peaoSelecionadoParaMover.getPosY(), x, y)) {
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
			if (mainController != null) {
				mainController.forEachMap(m -> m.desenharPeoes(mainController.getCombatentes()));
			} else {
				desenharPeoes(getCombatentes());
			}

			// Solta
			System.out.println("MAPA (GM): Soltou " + peaoSelecionadoParaMover.getNome() + " em (" + x + "," + y + ")");
			peaoSelecionadoParaMover = null;
			mapGrid.getScene().setCursor(javafx.scene.Cursor.OPEN_HAND);
			return;
		}

		if (squadHandler != null && squadHandler.isModoSquad() && atorAtual != null) {
			tratarCliqueSquad(cell, x, y);
			return;
		}

		if (!modoSelecaoAlvo || atorAtual == null)
			return;

		// MODO MOVER
		if (isMoverMode()) {
			if (atorAtual.getRaca() != null && !atorAtual.getRaca().podeSeMover(atorAtual)) {
				System.out.println("MAPA: " + atorAtual.getNome() + " nao pode se mover enquanto estiver em postura.");
				return;
			}
			if (tentarCruzarBordaDominio(atorAtual, atorAtual.getPosX(), atorAtual.getPosY(), x, y)) {
				return;
			}
			if (celulasAlcanceMovimento.contains(new CoordenadaMapa(x, y))) {
				// Prioridade: Saída
				if (estadoMapa.getTerreno(x,y)==TipoTerreno.SAIDA) {
					mainController.acionarTransicaoDeMapa(atorAtual);
					return;
				}
				int custoMovimento = calcularDistancia(atorAtual.getPosX(), atorAtual.getPosY(), x, y);
				if (custoMovimento < 0 || custoMovimento > atorAtual.getMovimentoRestanteTurno())
					return;


atorAtual.setMovimentoRestanteTurno(atorAtual.getMovimentoRestanteTurno() - custoMovimento);
				atorAtual.setPosX(x);
				atorAtual.setPosY(y);

				mainController.forEachMap(m -> m.desenharPeoes(mainController.getCombatentes()));

				if (mainController != null) {
					mainController.verificarInteracaoTerreno(atorAtual);
					mainController.notificarMovimentoRealizado();
				}

				if (modoSelecaoAlvo && habilidadeAtual != null) {
					setMoverMode(false);
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
				TipoAlvo tipo = habilidadeAtual.getTipoAlvoEfetivo();
				if (tipo == TipoAlvo.AREA || tipo.isFormatoAreaComEpicentro()
						|| tipo == TipoAlvo.LINHA || tipo == TipoAlvo.CONE) {
					selecionarAreaComEpicentro(x, y);
					return;
				}
			}

			// Verifica Single Target (Peão ou Objeto)
			Personagem alvo = getPersonagemNaCelula(x, y);
			boolean alvoNoAlcance = celulasAlcanceMovimento.contains(new CoordenadaMapa(x, y));

			if (!alvoNoAlcance && alvo != null) {
				for (int alvoX = alvo.getPosX(); alvoX < alvo.getPosX() + alvo.getTamanhoX(); alvoX++) {
					for (int alvoY = alvo.getPosY(); alvoY < alvo.getPosY() + alvo.getTamanhoY(); alvoY++) {
						if (dentroDoGrid(alvoX, alvoY)) {
							Pane alvoCell = obterCelula(alvoX,alvoY);
							if (celulasAlcanceMovimento.contains(new CoordenadaMapa(alvoX, alvoY))) {
								alvoNoAlcance = true;
								break;
							}
						}
					}
					if (alvoNoAlcance) break;
				}
			}

			if (alvoNoAlcance) {
				if (alvo != null && (!alvo.equals(atorAtual) || (habilidadeAtual != null && habilidadeAtual.afetaSiMesmo()))) {
					if (atorAtual.isClone() && alvo.isClone() && atorAtual.getCriador() == alvo.getCriador()) {
						System.out.println("MAPA: Clone não pode atacar seu aliado clone!");
						return;
					}
					if (modoSelecaoMultipla) {
						alvosSelecionadosMulti.add(alvo);
						alvosRestantes--;
						atualizarTextoContadorSelecao();
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
		if (maxDist <= 0 || aoeCalc == null)
			return;
		limparDestaquesAlcance();

		Set<CoordenadaMapa> celulas;
		if (cssClass.equals(CSS_ALCANCE_MOVIMENTO)) {
			celulas = aoeCalc.calcularCelulasMovimento(startX, startY, maxDist, dominiosAtivos);
		} else if (habilidadeAtual != null && habilidadeAtual.ignoraParedes()) {
			celulas = new LinkedHashSet<>();
			for (int x = 0; x < gridLargura; x++) {
				for (int y = 0; y < gridAltura; y++) {
					if (Math.max(Math.abs(x - startX), Math.abs(y - startY)) <= maxDist) {
						celulas.add(new CoordenadaMapa(x, y));
					}
				}
			}
		} else {
			celulas = aoeCalc.calcularCelulasAtaque(startX, startY, maxDist);
			if (habilidadeAtual != null && habilidadeAtual.afetaSiMesmo()) {
				celulas.add(new CoordenadaMapa(startX, startY));
			}
		}
		for (CoordenadaMapa c : celulas) {
			obterCelula(c.x(),c.y()).getStyleClass().add(cssClass);
			celulasAlcanceMovimento.add(c);
		}
	}

	private boolean temLinhaDeVisao(int x0, int y0, int x1, int y1) {
		return aoeCalc != null && aoeCalc.temLinhaDeVisao(x0, y0, x1, y1);
	}

	private void onToggleModo() {
        if(sincronizandoInteracao)return;
		if (atorAtual == null)
			return;

		limparCanvas();

		if (isMoverMode()) {
			System.out.println("MAPA: Modo Mover (Squad/Normal).");
			calcularEExibirMovimento(atorAtual);
		} else {
			System.out.println("MAPA: Modo Mirar (Squad/Normal).");
			calcularEExibirAtaqueRange(atorAtual, habilidadeAtual);
		}
        publicarInteracao();
	}

	public void atualizarBotaoPularSquad() {
		if (btnPularSquad == null || squadHandler == null)
			return;
		boolean mostrar = squadHandler.isModoSquad();
		btnPularSquad.setVisible(mostrar);
		btnPularSquad.setManaged(mostrar);
		btnPularSquad.setDisable(!mostrar);
	}

	public void setMainController(CombatController mainController) {
        estadoMapa.desobservar(ouvinteMapa);
        this.mainController = mainController;
        estadoMapa = mainController.getEstadoMapa();
        estadoMapa.observar(ouvinteMapa);
        if (estadoMapa.getLargura() > 0) sincronizarEstadoMapa(null);
		setSobreposicaoAguaTempestadeAtiva(mainController != null
				&& mainController.isSobreposicaoAguaTempestadeAtiva());
	}

	// ========== ACESSORES PARA HELPERS (SquadModeHandler, etc.) ==========

	public Personagem getAtorAtual() {
		return atorAtual;
	}

	public void setAtorAtual(Personagem ator) {
		this.atorAtual = ator;
	}

	public Habilidade getHabilidadeAtual() {
		return habilidadeAtual;
	}

	public void setHabilidadeAtual(Habilidade hab) {
		this.habilidadeAtual = hab;
	}

	public void setModoSelecaoAlvo(boolean v) {
		this.modoSelecaoAlvo = v;
	}

	public CombatController getMainController() {
		return mainController;
	}

	public List<Personagem> getCombatentes() {
		return mainController != null ? mainController.getCombatentes() : new ArrayList<>();
	}

	public void setCombatCursor(javafx.scene.Cursor cursor) {
		if (mapGrid.getScene() != null)
			mapGrid.getScene().setCursor(cursor);
	}

	public void carregarMapaDeImagem(File mapaFile) {
		try (FileInputStream fis = new FileInputStream(mapaFile)) {
			carregarMapaDeImagem(fis, mapaFile.getName());
		} catch (Exception e) {
			System.err.println("Erro ao abrir arquivo do mapa: " + mapaFile.getName());
			e.printStackTrace();
			preencherComChaoPadrao();
		}
	}

    public void carregarMapaDeImagem(InputStream is, String nomeMapa) {
        Image imagem = new Image(is);
        if (imagem.isError()) throw new IllegalArgumentException("Não foi possível ler o mapa " + nomeMapa, imagem.getException());
        TileRegistry registro = TileRegistry.getInstance();
        TileDefinition[][] tiles = new TileDefinition[(int) imagem.getWidth()][(int) imagem.getHeight()];
        PixelReader leitor = imagem.getPixelReader();
        for (int x=0;x<tiles.length;x++) for (int y=0;y<tiles[x].length;y++) {
            int rgb = leitor.getArgb(x,y);
            tiles[x][y] = registro.getByRgb((rgb >> 16)&255,(rgb >> 8)&255,rgb&255);
        }
        carregarMapaProcedural(tiles);
    }

    public void carregarMapaProcedural(TileDefinition[][] matrizTiles) {
        estadoMapa.carregar(matrizTiles, TileRegistry.getInstance().getDefault());
    }

    public br.com.dantesrpg.model.map.EstadoMapa getEstadoMapa() { return estadoMapa; }

    private void sincronizarEstadoMapa(CoordenadaMapa c) {
        if (c == null) {
            gridLargura=estadoMapa.getLargura(); gridAltura=estadoMapa.getAltura();
            paredesGrid=estadoMapa.paredesParaConsulta(); gridTerreno=estadoMapa.terrenosParaConsulta();
            gridEfeitos=estadoMapa.efeitosParaConsulta();
            celulasAlcanceMovimento.clear(); efeitosDesenhados.clear(); celulasVisuaisDominios.clear(); dominiosAtivos.clear();
            celulasGuardiaoDestacadas.clear(); celulasVigiliaDestacadas.clear(); celulasHarmoniaDestacadas.clear();
            mapGrid.getChildren().clear(); mapGrid.getColumnConstraints().clear(); mapGrid.getRowConstraints().clear();
            celulasDoGrid=new Pane[gridLargura][gridAltura];
            aoeCanvas.setWidth(gridLargura*CELL_SIZE); aoeCanvas.setHeight(gridAltura*CELL_SIZE);
            inicializarHelpers();
            renderizador2D.carregar(estadoMapa);
            if (renderizador3D!=null) renderizador3D.carregar(estadoMapa);
            desenharPeoes(getCombatentes()); resetZoom();
        } else {
            sincronizarCelula(c.x(),c.y());
            if(renderizador3D!=null)renderizador3D.atualizarCelula(c);
        }
    }

    private Pane obterCelula(int x,int y) {
        if(!dentroDoGrid(x,y))return null;
        Pane existente=celulasDoGrid[x][y];
        if(existente!=null)return existente;
        Pane cell=new Pane();celulasDoGrid[x][y]=cell;
        cell.setOnMouseClicked(e -> onGridCellClicked(e,cell,x,y));
        cell.setOnMouseEntered(e -> onGridCellMouseEntered(cell,x,y));
        cell.getStyleClass().addListener((javafx.collections.ListChangeListener<String>) change -> {
            if(sincronizandoCelulas)return;
            List<String> marcas=new ArrayList<>(cell.getStyleClass());
            marcas.removeAll(TileRegistry.getInstance().getAllCssClasses());marcas.remove("map-cell");
            estadoMapa.definirMarcas(x,y,marcas);
        });
        sincronizarCelula(x,y);
        return cell;
    }

    private void prepararGrade2D() {
        if(mapGrid.getColumnConstraints().isEmpty()) {
            for(int x=0;x<gridLargura;x++)mapGrid.getColumnConstraints().add(new ColumnConstraints(CELL_SIZE));
            for(int y=0;y<gridAltura;y++)mapGrid.getRowConstraints().add(new RowConstraints(CELL_SIZE));
        }
        for(int x=0;x<gridLargura;x++)for(int y=0;y<gridAltura;y++) {
            Pane cell=obterCelula(x,y);
            if(cell.getParent()==null)mapGrid.add(cell,x,y);
        }
    }

    private void sincronizarCelula(int x,int y) {
        if (!dentroDoGrid(x,y) || celulasDoGrid[x][y]==null) return;
        boolean anterior=sincronizandoCelulas; sincronizandoCelulas=true;
        try {
            Pane cell=celulasDoGrid[x][y];
            List<String> classes=new ArrayList<>();
            classes.add("map-cell"); classes.add(estadoMapa.getTile(x,y).getCssClass()); classes.addAll(estadoMapa.getMarcas(x,y));
            if (!cell.getStyleClass().equals(classes)) cell.getStyleClass().setAll(classes);
            CoordenadaMapa c=new CoordenadaMapa(x,y);
            if (efeitosDesenhados.get(c)!=gridEfeitos[x][y]) {
                efeitosDesenhados.put(c,gridEfeitos[x][y]); atualizarVisualTerreno(x,y);
            }
        } finally { sincronizandoCelulas=anterior; }
    }

    private void inicializarRenderizadores() {
        renderizador2D=new br.com.dantesrpg.controller.map.RenderizadorMapa2D(mapContent,
                lista -> { prepararGrade2D(); if(tokenRenderer!=null)tokenRenderer.desenharPeoes(lista); },
                lista -> { if(tokenRenderer!=null)tokenRenderer.destacarPeoesAlvo(lista); });
        boolean suporta=javafx.application.Platform.isSupported(javafx.application.ConditionalFeature.SCENE3D);
        ToggleButton alternar=new ToggleButton("3D"); alternarVisualizacao=alternar; alternar.getStyleClass().add("mapa-vista-toggle");
        alternar.setTooltip(new javafx.scene.control.Tooltip(suporta?"Alternar entre tabuleiro 2D e 3D":"3D indisponível neste dispositivo"));
        alternar.setDisable(!suporta);
        if(suporta) {
            renderizador3D=new br.com.dantesrpg.controller.map.RenderizadorMapa3D(this::clicarEm3D,this::apontarEm3D,
                    p -> mainController!=null && mainController.isPlayer(p),this::getAtorAtual);
            mapViewport.getChildren().add(renderizador3D.getNode());
        }
        mapViewport.getChildren().add(alternar);
        StackPane.setAlignment(alternar,javafx.geometry.Pos.BOTTOM_RIGHT);
        StackPane.setMargin(alternar,new javafx.geometry.Insets(8));
        alternar.setOnAction(e -> definirVisualizacao3D(alternar.isSelected()));
        alternar.setSelected(suporta); definirVisualizacao3D(suporta);
        mapViewport.sceneProperty().addListener((o,anterior,nova) -> {
            if(nova==null){observarJanela(null);suspenderVisualizacao(true);return;}
            nova.windowProperty().addListener((w,antiga,janela) -> observarJanela(janela));
            observarJanela(nova.getWindow());
        });
    }

    /** Mantém comandos acima do sombreado e do roster, sem interceptar o restante do mapa. */
    public void elevarControlesSobre(StackPane destino) {
        List<Node> controles=new ArrayList<>();
        controles.add(alternarVisualizacao);
        if(floatingToggleBox!=null)controles.add(floatingToggleBox);
        if(renderizador3D!=null)controles.add(renderizador3D.getControles());
        for(Node controle:controles) {
            if(controle.getParent() instanceof Pane origem)origem.getChildren().remove(controle);
            destino.getChildren().add(controle);
            if(!controlesElevados.contains(controle))controlesElevados.add(controle);
        }
    }

    private void observarJanela(javafx.stage.Window janela) {
        if(janelaObservada==janela)return;
        if(janelaObservada!=null) {
            janelaObservada.showingProperty().removeListener(ouvinteVisibilidade);
            if(janelaObservada instanceof javafx.stage.Stage stage)stage.iconifiedProperty().removeListener(ouvinteVisibilidade);
        }
        janelaObservada=janela;
        if(janela!=null) {
            janela.showingProperty().addListener(ouvinteVisibilidade);
            if(janela instanceof javafx.stage.Stage stage)stage.iconifiedProperty().addListener(ouvinteVisibilidade);
        }
        atualizarVisibilidadeJanela();
    }

    private void atualizarVisibilidadeJanela() {
        suspenderVisualizacao(janelaObservada==null || !janelaObservada.isShowing()
                || janelaObservada instanceof javafx.stage.Stage stage && stage.isIconified());
    }

    public void definirVisualizacao3D(boolean valor) {
        visualizacao3D=valor && renderizador3D!=null;
        if(alternarVisualizacao!=null)alternarVisualizacao.setSelected(visualizacao3D);
        renderizador2D.ativar(!visualizacao3D);
        if(renderizador3D!=null)renderizador3D.ativar(visualizacao3D);
        if(!visualizacao3D && cursorGrade!=null && habilidadeAtual!=null && modoSelecaoAlvo && !isMoverMode())
            desenharVisualAoE((cursorGrade.x()+.5)*CELL_SIZE,(cursorGrade.y()+.5)*CELL_SIZE);
        if(renderizador3D!=null)renderizador3D.apresentarPreview(previewAtual,"aoe",
                areasSelecionadasMultiAoE.stream().map(AcaoMestreInput.AreaSelecionada::epicentro).toList());
    }

    public void sincronizarInteracaoDe(MapController origem) {
        if(origem==null || origem==this)return;
        sincronizandoInteracao=true;
        try {
            modoSelecaoAlvo=origem.modoSelecaoAlvo; habilidadeAtual=origem.habilidadeAtual; atorAtual=origem.atorAtual;
            modoSelecaoMultipla=origem.modoSelecaoMultipla; alvosRestantes=origem.alvosRestantes;
            modoSelecaoMultiAoE=origem.modoSelecaoMultiAoE; areasRestantes=origem.areasRestantes;
            alvosSelecionadosMulti.clear();alvosSelecionadosMulti.addAll(origem.alvosSelecionadosMulti);
            areasSelecionadasMultiAoE.clear();areasSelecionadasMultiAoE.addAll(origem.areasSelecionadasMultiAoE);
            celulasAlcanceMovimento.clear();celulasAlcanceMovimento.addAll(origem.celulasAlcanceMovimento);
            modoMovimentoLivre=origem.modoMovimentoLivre;peaoSelecionadoParaMover=origem.peaoSelecionadoParaMover;
            modoSpawnInimigo=origem.modoSpawnInimigo;idMonstroEmSpawn=origem.idMonstroEmSpawn;cargasSpawnRestantes=origem.cargasSpawnRestantes;
            modoSelecaoExplosaoAmbiental=origem.modoSelecaoExplosaoAmbiental;
            aoSelecionarExplosaoAmbiental=origem.aoSelecionarExplosaoAmbiental;aoCancelarSelecaoExplosaoAmbiental=origem.aoCancelarSelecaoExplosaoAmbiental;
            setMoverMode(origem.isMoverMode());
            if(squadHandler!=null)squadHandler.sincronizarDe(origem.squadHandler);
            atualizarBotaoPularSquad();
            if(floatingToggleBox!=null){floatingToggleBox.setVisible(modoSelecaoAlvo);floatingToggleBox.setManaged(modoSelecaoAlvo);}
            if(modoSelecaoMultipla || modoSelecaoMultiAoE)criarLabelContador();else removerLabelContador();
            previewAtual.clear();previewAtual.addAll(origem.previewAtual);
            if(renderizador3D!=null)renderizador3D.apresentarPreview(previewAtual,"aoe",areasSelecionadasMultiAoE.stream().map(AcaoMestreInput.AreaSelecionada::epicentro).toList());
        } finally {sincronizandoInteracao=false;}
    }

    private void publicarInteracao() {
        if(mainController!=null && !sincronizandoInteracao)mainController.forEachMap(m -> m.sincronizarInteracaoDe(this));
    }

    public void suspenderVisualizacao(boolean suspender) {
        if(renderizador3D!=null)renderizador3D.ativar(!suspender && visualizacao3D);
    }

    public void liberarRecursos() {
        observarJanela(null);
        for(Node controle:controlesElevados)
            if(controle.getParent() instanceof Pane origem)origem.getChildren().remove(controle);
        controlesElevados.clear();
        estadoMapa.desobservar(ouvinteMapa);
        if(renderizador3D!=null)renderizador3D.close();
        if(renderizador2D!=null)renderizador2D.close();
    }

    private void clicarEm3D(CoordenadaMapa c, MouseEvent evento) {
        if(c!=null && dentroDoGrid(c.x(),c.y())) onGridCellClicked(evento,obterCelula(c.x(),c.y()),c.x(),c.y());
        else if(evento.getButton()==javafx.scene.input.MouseButton.SECONDARY) {
            if(modoSelecaoExplosaoAmbiental)cancelarModoSelecaoExplosaoAmbiental();
            else if(modoSpawnInimigo && mainController!=null)mainController.notifySpawnConcluido();
            else peaoSelecionadoParaMover=null;
        }
        desenharPeoes(getCombatentes());
        if(cursorGrade!=null)apontarEm3D(cursorGrade);
    }

    private void apontarEm3D(CoordenadaMapa c) {
        cursorGrade=c;
        if(c==null || !dentroDoGrid(c.x(),c.y())) { limparCanvas(); return; }
        onGridCellMouseEntered(obterCelula(c.x(),c.y()),c.x(),c.y());
        previewAtual.clear();
        if(modoSelecaoExplosaoAmbiental) {
            for(int x=c.x()-1;x<=c.x()+1;x++)for(int y=c.y()-1;y<=c.y()+1;y++)
                if(dentroDoGrid(x,y) && Math.abs(x-c.x())+Math.abs(y-c.y())<=1)previewAtual.add(new CoordenadaMapa(x,y));
        } else if(modoSelecaoAlvo && !isMoverMode() && habilidadeAtual!=null && aoeCalc!=null) {
            previewAtual.addAll(aoeCalc.calcularForma(c.x(),c.y(),habilidadeAtual,atorAtual));
        }
        if(renderizador3D!=null)renderizador3D.apresentarPreview(previewAtual,"aoe",
                areasSelecionadasMultiAoE.stream().map(AcaoMestreInput.AreaSelecionada::epicentro).toList());
    }

	private void inicializarHelpers() {
		aoeCalc = new AoEShapeCalculator(paredesGrid, gridLargura, gridAltura,
				this::getPersonagemNaCelula,
				() -> mainController != null ? mainController.getCombatentes() : new ArrayList<>());
		tokenRenderer = new MapTokenRenderer(mapGrid, mainController, this::obterCelula,
				gridLargura, gridAltura, CELL_SIZE, this::estaEmAguaProfunda);
		// Recria squadHandler com a nova referência a aoeCalc
		squadHandler = new SquadModeHandler(this, aoeCalc, toggleMover, toggleMirar, celulasAlcanceMovimento);
		atualizarSobreposicaoAguaTempestade();
	}

	/** Exibe uma camada visual de água sobre todas as tiles sem alterar o terreno base. */
	public void setSobreposicaoAguaTempestadeAtiva(boolean ativa) {
		this.sobreposicaoAguaTempestadeAtiva = ativa;
		atualizarSobreposicaoAguaTempestade();
	}

    private void atualizarSobreposicaoAguaTempestade() {
        for(int x=0;x<gridLargura;x++)for(int y=0;y<gridAltura;y++) {
            List<String> marcas=new ArrayList<>(estadoMapa.getMarcas(x,y));
            marcas.remove("tempestade-agua-overlay");
            if(sobreposicaoAguaTempestadeAtiva)marcas.add("tempestade-agua-overlay");
            estadoMapa.definirMarcas(x,y,marcas);
        }
    }

	/**
	 * Aplica uma TileDefinition em uma celula do grid.
	 * Metodo central usado pelo carregamento de PNG e pelo editor.
	 */
    private void aplicarTileNaCelula(Pane cell, TileDefinition tile, int x, int y) {
        estadoMapa.aplicarTile(x,y,tile);
    }

	/**
	 * Aplica um tile no editor, limpando o estado anterior da celula.
	 */
	public void aplicarTileNoEditor(Pane cell, TileDefinition tile, int x, int y) {
		if (tile == null || !dentroDoGrid(x, y))
			return;

		if (mainController != null) {
			// Ações lógicas de criação/remoção de objetos, executadas 1 vez apenas
			if (gridTerreno[x][y] == TipoTerreno.OBJETO) {
				mainController.removerObjetoNoMapa(x, y);
			}
			if ("OBJETO".equals(tile.getTerrainType())) {
				mainController.criarObjetoNoMapa(x, y);
			}

			System.out.println("EDITOR GLOBAL: (" + x + "," + y + ") -> " + tile.getName() + " [" + tile.getId() + "]");

			// Atualiza a visualização e arrays internos de TODOS os mapas abertos
			aplicarTileSomenteVisual(tile, x, y);
		} else {
			aplicarTileSomenteVisual(tile, x, y);
			System.out.println("EDITOR LOCAL: (" + x + "," + y + ") -> " + tile.getName() + " [" + tile.getId() + "]");
		}
	}

    public void aplicarTileSomenteVisual(TileDefinition tile, int x, int y) {
        estadoMapa.aplicarTile(x,y,tile);
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

	/**
	 * Converte o círculo 3x3 de uma explosão ambiental em carvão queimando.
	 * O formato usa a mesma geometria circular (Manhattan) das habilidades de
	 * área: centro e seus quatro vizinhos ortogonais.
	 */
	public void criarExplosaoDimensionRift(int centroX, int centroY) {
		TileDefinition carvao = TileRegistry.getInstance().getById("coal");
		if (carvao == null) {
			System.err.println("MAPA: Tile 'coal' não encontrado para a explosão Dimension Rift.");
			return;
		}

		for (int y = centroY - 1; y <= centroY + 1; y++) {
			for (int x = centroX - 1; x <= centroX + 1; x++) {
				if (Math.abs(x - centroX) + Math.abs(y - centroY) > 1
						|| !dentroDoGrid(x, y) || paredesGrid[x][y]) {
					continue;
				}
				aplicarTileSomenteVisual(carvao, x, y);
				aplicarEfeitoNoSolo(x, y,
						new EfeitoInstance(TipoEfeitoSolo.FOGO, 0, 0, null));
			}
		}
	}

	public void criarAreaDeFogoAmaldicoado(int centroX, int centroY, int raio, int duracaoTU, Personagem criador) {
		System.out.println("MAPA: Criando fogo amaldiçoado centrado em (" + centroX + "," + centroY + ")");
		for (int y = centroY - raio; y <= centroY + raio; y++) {
			for (int x = centroX - raio; x <= centroX + raio; x++) {
				if (dentroDoGrid(x, y) && !paredesGrid[x][y]) {
					aplicarEfeitoNoSolo(x, y,
							new EfeitoInstance(TipoEfeitoSolo.FOGO_AMALDICOADO, duracaoTU, 0, criador));
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
						estadoMapa.definirEfeito(x,y,null); // Remove o efeito
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
				estadoMapa.definirEfeito(x,y,novoEfeito);

				System.out.println("TERRENO: Carvão aceso em (" + x + "," + y + ")!");
				atualizarVisualTerreno(x, y);

				// Reação em Cadeia (Flood Fill)
				espalharFogoRecursivo(x, y, novoEfeito);
			}
			// CASO B: Chão Comum (ou Objeto)
			else {
				// Apenas aplica o efeito temporário
				estadoMapa.definirEfeito(x,y,novoEfeito);
				atualizarVisualTerreno(x, y);
			}
		}
		// --- OUTROS EFEITOS Proximos andares rs---
		else {
			// Lógica padrão para ácido, gás, etc.
			estadoMapa.definirEfeito(x,y,novoEfeito);
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
					// Cria uma cópia ou reutiliza a referência (aqui reutilizamos pois é
					// permanente)
					aplicarEfeitoNoSolo(nx, ny, efeitoOriginal);
				}
			}
		}
	}

	private void atualizarVisualTerreno(int x, int y) {
		if (!dentroDoGrid(x, y))
			return;

		Pane cell = obterCelula(x,y);
		EfeitoInstance efeito = gridEfeitos[x][y];
		TipoTerreno terreno = gridTerreno[x][y];

		// LIMPEZA: Remove qualquer imagem de efeito anterior (preservando o fundo/css
		// base)
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
		} else if (efeito.getTipo() == TipoEfeitoSolo.FOGO_AMALDICOADO) {
			imagemParaCarregar = "/effects/cusedFireOverlay.png";
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
				// Tenta carregar com cache e downsampling
				Image img = ImageCache.get(imagemParaCarregar, CELL_SIZE, CELL_SIZE);
				if (img != null && !img.isError()) {
					ImageView fxView = new ImageView(img);
					fxView.setSmooth(false);
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
        carregarMapaProcedural(new TileDefinition[Math.max(1,gridLargura)][Math.max(1,gridAltura)]);
    }

    public void desenharPeoes(List<Personagem> combatentes) {
        List<Personagem> lista=combatentes.stream().filter(java.util.Objects::nonNull).toList();
        if(tokenRenderer!=null && visualizacao3D)tokenRenderer.atualizarAuras(lista);
        if(renderizador2D!=null)renderizador2D.atualizarCombatentes(lista);
        if(renderizador3D!=null)renderizador3D.atualizarCombatentes(lista);
    }

	public Personagem getPersonagemNaCelula(int x, int y) {
		if (mainController == null)
			return null;
		for (Personagem p : mainController.getCombatentes()) {
			if (p.isAtivoNoCombate()) {
				if (p.ocupa(x, y))
					return p;
			}
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
		if(renderizador2D!=null)renderizador2D.destacar(alvos);
        if(renderizador3D!=null)renderizador3D.destacar(alvos);
	}

	public void limparDestaquesPeoes() {
		destacarPeoesAlvo(List.of());
	}

	private Node getPeaoNode(Personagem p) {
		return tokenRenderer != null ? tokenRenderer.getPeaoNode(p) : null;
	}

	// ========== EMPUXO (KNOCKBACK) — API pública ==========

	/** Retorna true se o tile (x,y) é uma parede (bloqueante). */
	public boolean isParedeem(int x, int y) {
		if (paredesGrid == null || x < 0 || y < 0
				|| x >= gridLargura || y >= gridAltura)
			return true;
		return paredesGrid[x][y];
	}

	/** Intercepta a travessia de uma borda de domínio e aplica sua penalidade. */
	public boolean tentarCruzarBordaDominio(Personagem personagem, int origemX, int origemY, int destinoX, int destinoY) {
		if (personagem == null) return false;
		for (Dominio dominio : dominiosAtivos.values()) {
			if (!dominio.bloqueiaMovimento(origemX, origemY, destinoX, destinoY)) continue;
			if (dominio.exigeTesteDestrezaParaSair()) {
				if (resolverTesteDestrezaParaSair(personagem, dominio)) {
					return false;
				}
				aplicarDanoFalhaSaida(personagem, dominio);
				return true;
			}

			if (dominio.getDanoAoCruzarBorda() > 0.0 && mainController != null
					&& mainController.getCombatManager() != null) {
				mainController.getCombatManager().getDamageApplicator().aplicarDanoAoAlvo(
						null, personagem, dominio.getDanoAoCruzarBorda(), false,
						br.com.dantesrpg.model.enums.TipoAcao.AMBIENTE, mainController.getEstadoCombate());
			}
			if (dominio.getChoqueAoCruzarBordaTU() > 0 && mainController != null
					&& mainController.getCombatManager() != null) {
				br.com.dantesrpg.model.Efeito choque = br.com.dantesrpg.model.util.EffectFactory.criarEfeito(
						"Choque", 1, dominio.getChoqueAoCruzarBordaTU());
				mainController.getCombatManager().getEffectProcessor().aplicarEfeito(personagem, choque);
			}
			System.out.println(">>> " + personagem.getNome() + " atingiu a borda de " + dominio.getNomeEfeito() + ".");
			return true;
		}
		return false;
	}

	private boolean resolverTesteDestrezaParaSair(Personagem personagem, Dominio dominio) {
		Personagem dono = dominio.getDono();
		if (dono == null) return false;

		int dadoPersonagem = br.com.dantesrpg.model.util.DiceRoller.getTipoDado(
				personagem.getAtributosFinais().getOrDefault(br.com.dantesrpg.model.enums.Atributo.DESTREZA, 1));
		int dadoDono = br.com.dantesrpg.model.util.DiceRoller.getTipoDado(
				dono.getAtributosFinais().getOrDefault(br.com.dantesrpg.model.enums.Atributo.DESTREZA, 1));

		javafx.scene.control.TextInputDialog testePersonagem = new javafx.scene.control.TextInputDialog();
		testePersonagem.setTitle("Cerca Elétrica");
		testePersonagem.setHeaderText(personagem.getNome() + " tenta atravessar a cerca.");
		testePersonagem.setContentText("Teste de Destreza de " + personagem.getNome() + " (d" + dadoPersonagem + "): ");
		java.util.Optional<String> resultadoPersonagem = testePersonagem.showAndWait();
		if (resultadoPersonagem.isEmpty()) return false;

		javafx.scene.control.TextInputDialog testeDono = new javafx.scene.control.TextInputDialog();
		testeDono.setTitle("Cerca Elétrica");
		testeDono.setHeaderText(dono.getNome() + " sustenta a contenção.");
		testeDono.setContentText("Teste de Destreza de " + dono.getNome() + " (d" + dadoDono + "): ");
		java.util.Optional<String> resultadoDono = testeDono.showAndWait();
		if (resultadoDono.isEmpty()) return false;

		try {
			int rolagemPersonagem = Integer.parseInt(resultadoPersonagem.get().trim());
			int rolagemDono = Integer.parseInt(resultadoDono.get().trim());
			boolean sucesso = rolagemPersonagem > rolagemDono;
			System.out.println(">>> Cerca Elétrica: " + personagem.getNome() + " rolou " + rolagemPersonagem
					+ " vs " + dono.getNome() + " " + rolagemDono + ". "
					+ (sucesso ? "A saída foi bem-sucedida." : "A contenção resistiu."));
			return sucesso;
		} catch (NumberFormatException e) {
			System.out.println(">>> Cerca Elétrica: resultado de Destreza inválido; a saída falhou.");
			return false;
		}
	}

	private void aplicarDanoFalhaSaida(Personagem personagem, Dominio dominio) {
		if (dominio.getDanoAoFalharSaida() > 0.0 && mainController != null
				&& mainController.getCombatManager() != null) {
			mainController.getCombatManager().getDamageApplicator().aplicarDanoAoAlvo(
					dominio.getDono(), personagem, dominio.getDanoAoFalharSaida(), false,
					br.com.dantesrpg.model.enums.TipoAcao.AMBIENTE, mainController.getEstadoCombate());
		}
		System.out.println(">>> " + personagem.getNome() + " falhou ao sair de " + dominio.getNomeEfeito() + ".");
	}

	/** Cria uma zona de escombros persistente que bloqueia movimento e linha de visão. */
	public void criarEscombros(int centroX, int centroY, int tamanho) {
		int raio = (Math.max(1, tamanho) - 1) / 2;
		for (int y = centroY - raio; y <= centroY + raio; y++) {
			for (int x = centroX - raio; x <= centroX + raio; x++) {
				if (!dentroDoGrid(x, y) || paredesGrid[x][y]) continue;
				Pane celula = obterCelula(x,y);
				if (celula == null) continue;
				estadoMapa.definirBloqueio(x,y,true);
				celula.getStyleClass().add("map-escombros");
			}
		}
	}

	/** Largura do grid em tiles. */
	public int getGridLargura() {
		return gridLargura;
	}

	/** Altura do grid em tiles. */
	public int getGridAltura() {
		return gridAltura;
	}

	/**
	 * Desenha no {@code aoeCanvas} um preview da trajetória de empuxo.
	 *
	 * <p>
	 * Tiles de passagem são exibidos em laranja translúcido.
	 * O tile de destino final é marcado com uma borda mais intensa.
	 * Se {@code colidiu} for true, o destino é marcado em vermelho.
	 * </p>
	 *
	 * @param trajetoria Lista de coordenadas {x,y} do caminho (gerada por
	 *                   KnockbackProcessor).
	 * @param colidiu    true se o empuxo termina em colisão (parede/borda).
	 */
	public void desenharPreviewEmpuxo(java.util.List<int[]> trajetoria, boolean colidiu) {
        if(renderizador3D!=null && trajetoria!=null)renderizador3D.apresentarPreview(
                trajetoria.stream().map(p -> new CoordenadaMapa(p[0],p[1])).toList(), colidiu?"colisao":"empuxo",List.of());
		if (aoeCanvas == null || trajetoria == null || trajetoria.isEmpty())
			return;

		GraphicsContext gc = aoeCanvas.getGraphicsContext2D();

		javafx.scene.paint.Color corPassagem = javafx.scene.paint.Color.rgb(255, 165, 0, 0.35); // laranja
		javafx.scene.paint.Color corBordaPassagem = javafx.scene.paint.Color.rgb(255, 140, 0, 0.85);
		javafx.scene.paint.Color corDestino = colidiu
				? javafx.scene.paint.Color.rgb(220, 50, 50, 0.55) // vermelho = colisão
				: javafx.scene.paint.Color.rgb(255, 200, 0, 0.55); // amarelo-ouro = parada livre
		javafx.scene.paint.Color corBordaDestino = colidiu
				? javafx.scene.paint.Color.rgb(180, 20, 20, 0.95)
				: javafx.scene.paint.Color.rgb(220, 160, 0, 0.95);

		gc.setLineWidth(2.0);

		for (int i = 0; i < trajetoria.size(); i++) {
			int[] coord = trajetoria.get(i);
			double px = coord[0] * CELL_SIZE;
			double py = coord[1] * CELL_SIZE;
			boolean isDestino = (i == trajetoria.size() - 1);

			gc.setFill(isDestino ? corDestino : corPassagem);
			gc.fillRect(px + 1, py + 1, CELL_SIZE - 2, CELL_SIZE - 2);

			gc.setStroke(isDestino ? corBordaDestino : corBordaPassagem);
			gc.strokeRect(px + 1, py + 1, CELL_SIZE - 2, CELL_SIZE - 2);
		}
	}

	/**
	 * Remove o preview de empuxo do canvas (limpa sem apagar outros desenhos AoE).
	 */
	public void limparPreviewEmpuxo() {
		limparCanvas();
	}

	public boolean isModoEditor() {
		return this.modoEditor;
	}

	public void toggleModoEditor() {
		setModoEditor(!this.modoEditor);
	}

	public void setModoEditor(boolean ativo) {
		if (this.modoEditor == ativo)
			return;
		this.modoEditor = ativo;

		if (btnEditorMapa != null)
			btnEditorMapa.setSelected(ativo);

		if (this.modoEditor) {
			if (btnMovimentoLivre != null)
				btnMovimentoLivre.setSelected(false);
			this.modoMovimentoLivre = false;
			this.modoSpawnInimigo = false;
			this.modoSelecaoAlvo = false;
			sairModoSelecao();

			System.out.println("MAPA: Modo EDITOR ativado.");
			mapGrid.getScene().setCursor(javafx.scene.Cursor.OPEN_HAND);

			// Apenas o PrimaryMap gerencia a janela do catálogo para não abrir duplicada
			if (mainController == null || mainController.getPrimaryMap() == this) {
				abrirJanelaEditor();
			}
		} else {
			System.out.println("MAPA: Modo EDITOR desativado.");
			mapGrid.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
			fecharJanelaEditor();
		}
	}

	public void setEditorTile(TileDefinition tile) {
		this.editorTile = tile;
		if (tile != null) {
			System.out.println("EDITOR: Tile selecionado -> " + tile.getName() + " [" + tile.getId() + "]");
		}
	}

	public TileDefinition getEditorTile() {
		return this.editorTile;
	}

	private void abrirJanelaEditor() {
		if (editorWindowStage != null && editorWindowStage.isShowing()) {
			editorWindowStage.toFront();
			return;
		}

		try {
			javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
					getClass().getResource("/br/com/dantesrpg/view/EditorWindow.fxml"));
			javafx.scene.Parent root = loader.load();

			EditorWindowController editorController = loader.getController();
			editorController.setMapController(this);

			editorWindowStage = new javafx.stage.Stage();
			editorWindowStage.setTitle("Editor de Mapa - Catalogo de Tiles");
			editorWindowStage.setScene(new javafx.scene.Scene(root));
			editorWindowStage.setAlwaysOnTop(true);
			editorWindowStage.setResizable(true);
			editorWindowStage.setWidth(340);
			editorWindowStage.setHeight(550);

			editorWindowStage.setOnCloseRequest(e -> {
				if (mainController != null) {
					mainController.forEachMap(m -> m.setModoEditor(false));
				} else {
					setModoEditor(false);
				}
				this.editorTile = null;
				System.out.println("MAPA: Janela do editor fechada. Modo EDITOR desativado.");
			});

			editorWindowStage.show();
			System.out.println("MAPA: Janela do editor aberta.");

		} catch (Exception e) {
			System.err.println("ERRO ao abrir janela do editor de mapa:");
			e.printStackTrace();
		}
	}

	private void fecharJanelaEditor() {
		if (editorWindowStage != null && editorWindowStage.isShowing()) {
			editorWindowStage.close();
		}
		editorWindowStage = null;
		editorTile = null;
	}

    public void atualizarCelulaParaChao(int x, int y) {
        estadoMapa.aplicarTile(x,y,TileRegistry.getInstance().getById("floor"));
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
		this.modoSelecaoMultiAoE = false;
		this.alvosRestantes = 0;
		this.areasRestantes = 0;
		this.alvosSelecionadosMulti.clear();
		this.areasSelecionadasMultiAoE.clear();
		removerLabelContador();

		System.out.println("MAPA: " + ator.getNome() + " entrando em modo de seleção...");

		if (habilidade != null && habilidade.getTipoAlvo() == TipoAlvo.MULTI_AOE) {
			this.modoSelecaoMultiAoE = true;
			this.areasRestantes = habilidade.getNumeroDeAreas();
			if (areasRestantes <= 0) {
				throw new IllegalStateException(
						"Habilidade MULTI_AOE sem áreas selecionáveis: " + habilidade.getNome());
			}
			habilidade.getTipoAlvoEfetivo();
			setMoverMode(false);
			criarLabelContador();
			calcularEExibirAtaqueRange(ator, habilidade);
			System.out.println("MAPA: Modo MULTI-AOE ativado. Áreas restantes: " + areasRestantes);
		} else if (habilidade != null && (habilidade.getTipoAlvo() == TipoAlvo.AREA_QUADRADA
				|| habilidade.getTipoAlvo() == TipoAlvo.AREA_CIRCULAR || habilidade.getTipoAlvo() == TipoAlvo.AREA
				|| habilidade.getTipoAlvo() == TipoAlvo.LINHA || habilidade.getTipoAlvo() == TipoAlvo.CONE)) {
			setMoverMode(false);
			System.out.println("MAPA: Modo Mirar Área (AoE) ativado.");
			calcularEExibirAtaqueRange(ator, habilidade);
		} else if (habilidade != null && habilidade.getTipoAlvo() == TipoAlvo.MULTIPLOS) {
			this.modoSelecaoMultipla = true;
			this.alvosRestantes = habilidade.getNumeroDeAlvos();

			criarLabelContador(); // (Vamos criar este método)
			calcularEExibirAtaqueRange(ator, habilidade); // Mostra o alcance
			System.out.println("MAPA: Modo Seleção Múltipla ativado. Alvos restantes: " + alvosRestantes);

		} else {
			setMoverMode(true);
			calcularEExibirMovimento(ator);
		}

		if (floatingToggleBox != null) {
			floatingToggleBox.setVisible(true);
			floatingToggleBox.setManaged(true);
		}

		mapGrid.getScene().setCursor(javafx.scene.Cursor.CROSSHAIR);
		limparCanvas();
		limparDestaquesGridAtaque();
		limparDestaquesPeoes();
	}

	private void limparDestaquesGridAtaque() {
		for (CoordenadaMapa c : celulasAlcanceMovimento) {
			Pane cell = obterCelula(c.x(),c.y()); // Assumindo que celulasAlcanceMovimento inclui as células que
													// poderiam ser alvo
			cell.getStyleClass().remove(CSS_ALCANCE_ATAQUE_MELEE);
			cell.getStyleClass().remove(CSS_ALCANCE_ATAQUE_RANGED);
		}
	}

	private void criarLabelContador() {
		if (labelContadorAlvos == null) {
			labelContadorAlvos = new Label();
			labelContadorAlvos.getStyleClass().add("mapa3d-mensagem");
		}
		atualizarTextoContadorSelecao();

		// Adiciona na barra superior se não estiver lá (ignora em modo embedded sem
		// toolbar)
		if (topToolbar != null && !topToolbar.getItems().contains(labelContadorAlvos)) {
			topToolbar.getItems().add(labelContadorAlvos);
        } else if(topToolbar==null && !mapViewport.getChildren().contains(labelContadorAlvos)) {
            mapViewport.getChildren().add(labelContadorAlvos);
            StackPane.setAlignment(labelContadorAlvos,javafx.geometry.Pos.TOP_LEFT);
            StackPane.setMargin(labelContadorAlvos,new javafx.geometry.Insets(8));
        }
	}

	/** Aplica uma parede ou chão neste mapa; o chamador sincroniza mapas abertos. */
	public void aplicarParedeDaArteDoCaos(int x, int y, boolean criar) {
		if (!dentroDoGrid(x, y)) return;
		if (obterCelula(x,y) == null) return;
		Pane cell = obterCelula(x,y);
		cell.getStyleClass().removeAll(TileRegistry.getInstance().getAllCssClasses());
		TileDefinition tile = TileRegistry.getInstance().getById(criar ? "wall" : "floor");
		if (tile == null) tile = TileRegistry.getInstance().getDefault();
		aplicarTileNaCelula(cell, tile, x, y);
		cell.getStyleClass().add("map-cell");
		estadoMapa.definirEfeito(x,y,null);
	}

	private void atualizarTextoContadorSelecao() {
		if (labelContadorAlvos == null) {
			return;
		}
		if (modoSelecaoMultiAoE) {
			labelContadorAlvos.setText("Áreas restantes: " + areasRestantes);
		} else {
			labelContadorAlvos.setText("Alvos restantes: " + alvosRestantes);
		}
	}

	private void removerLabelContador() {
		if (labelContadorAlvos != null && topToolbar != null) {
			topToolbar.getItems().remove(labelContadorAlvos);
		}
        if(labelContadorAlvos!=null)mapViewport.getChildren().remove(labelContadorAlvos);
	}

	public void limparDestaquesAlcance() {
		for (CoordenadaMapa c : celulasAlcanceMovimento) {
			Pane cell = obterCelula(c.x(),c.y());
			cell.getStyleClass().remove(CSS_ALCANCE_MOVIMENTO);
			cell.getStyleClass().remove(CSS_ALCANCE_ATAQUE_MELEE);
			cell.getStyleClass().remove(CSS_ALCANCE_ATAQUE_RANGED);
		}
		celulasAlcanceMovimento.clear();
	}

	private void selecionarAreaComEpicentro(int epicentroX, int epicentroY) {
		limparCanvas();

		if (habilidadeAtual != null && habilidadeAtual.getTipoAlvo() != TipoAlvo.AREA) {
			Pane celulaClicada = obterCelula(epicentroX,epicentroY);
			if (!celulasAlcanceMovimento.contains(new CoordenadaMapa(epicentroX, epicentroY))) {
				System.out.println(">>> ALVO FORA DO ALCANÇE de conjuração!");
				return;
			}
		}

		System.out.println("MAPA: Célula de centro AoE selecionada: (" + epicentroX + "," + epicentroY + ")");

		// Encontra os alvos (usando o novo método genérico)
		List<Personagem> alvosNaArea = encontrarAlvosNaForma(epicentroX, epicentroY, habilidadeAtual, atorAtual);

		if (modoSelecaoMultiAoE) {
			AcaoMestreInput.AreaSelecionada area = new AcaoMestreInput.AreaSelecionada(
					new CoordenadaMapa(epicentroX, epicentroY), alvosNaArea);
			areasSelecionadasMultiAoE.add(area);
			areasRestantes--;
			atualizarTextoContadorSelecao();
			atualizarPreviewMultiAoE(List.of());

			if (areasRestantes > 0) {
				System.out.println("MAPA: Área MULTI-AOE registrada. Restam " + areasRestantes + ".");
				return;
			}

			mainController.adicionarAlvosMultiArea(List.copyOf(areasSelecionadasMultiAoE));
			sairModoSelecao();
			return;
		}

		// Envia os alvos E o epicentro (x,y) para a HUD
		mainController.adicionarAlvosArea(alvosNaArea, epicentroX, epicentroY);
		sairModoSelecao();
	}

	public void calcularEExibirMovimento(Personagem ator) {
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

	public void calcularEExibirAtaqueRange(Personagem ator, Habilidade habilidade) {
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
		atualizarPreviewMultiAoE(alvos);
	}

	private void desenharPreviewQuadrado(int x, int y) {
		List<Personagem> alvos = encontrarAlvosNaForma(x, y, habilidadeAtual, atorAtual);
		atualizarPreviewMultiAoE(alvos);
	}

	private void desenharPreviewCircular(int x, int y) {
		// A lógica Manhattan está dentro de encontrarAlvosNaForma!
		List<Personagem> alvos = encontrarAlvosNaForma(x, y, habilidadeAtual, atorAtual);
		atualizarPreviewMultiAoE(alvos);
	}

	private void desenharPreviewLinha(int cursorX, int cursorY) {
		List<Personagem> alvos = encontrarAlvosNaForma(cursorX, cursorY, habilidadeAtual, atorAtual);
		atualizarPreviewMultiAoE(alvos);
	}

	private void atualizarPreviewMultiAoE(List<Personagem> alvosDaAreaAtual) {
		if (!modoSelecaoMultiAoE) {
			destacarPeoesAlvo(alvosDaAreaAtual);
			mainController.alvosIdentificadosNoMapa(alvosDaAreaAtual);
			return;
		}

		List<Personagem> impactos = new ArrayList<>();
		for (AcaoMestreInput.AreaSelecionada area : areasSelecionadasMultiAoE) {
			impactos.addAll(area.alvos());
		}
		if (alvosDaAreaAtual != null) {
			impactos.addAll(alvosDaAreaAtual);
		}

		destacarPeoesAlvo(new ArrayList<>(new LinkedHashSet<>(impactos)));
		mainController.alvosIdentificadosNoMapa(impactos);
	}

	public int calcularDistancia(int startX, int startY, int endX, int endY) {
		return aoeCalc != null ? aoeCalc.calcularDistancia(startX, startY, endX, endY) : -1;
	}

	public List<Personagem> encontrarAlvosNaForma(int centroX, int centroY, Habilidade habilidade, Personagem ator) {
		if (aoeCalc == null)
			return new ArrayList<>();
		return aoeCalc.encontrarAlvosNaForma(centroX, centroY, habilidade, ator);
	}

	public void sairModoSelecao() {
		limparCanvas();
		limparDestaquesGridAtaque();
		limparDestaquesPeoes();
		this.modoSelecaoAlvo = false;
		this.habilidadeAtual = null;
		this.atorAtual = null;
		this.modoSelecaoMultipla = false;
		this.modoSelecaoMultiAoE = false;
		this.alvosRestantes = 0;
		this.areasRestantes = 0;
		this.alvosSelecionadosMulti.clear();
		this.areasSelecionadasMultiAoE.clear();
        limparCanvas();
		System.out.println("MAPA: Saindo do modo de seleção.");

		removerLabelContador(); // Apenas remove o texto

		limparDestaquesAlcance();

		if (floatingToggleBox != null) {
			floatingToggleBox.setVisible(false);
			floatingToggleBox.setManaged(false);
		}

		if (mapGrid.getScene() != null) {
			mapGrid.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
		}

		atualizarBotaoPularSquad();
	}

	public boolean isModoMovimentoLivre() {
		return this.modoMovimentoLivre;
	}

	public void toggleModoMovimentoLivre() {
		toggleModoMovimentoLivre(!this.modoMovimentoLivre);
	}

	public void toggleModoMovimentoLivre(boolean estado) {
		if (this.modoMovimentoLivre == estado)
			return;
		this.modoMovimentoLivre = estado;

		if (btnMovimentoLivre != null)
			btnMovimentoLivre.setSelected(estado);

		if (this.modoMovimentoLivre) {
			if (btnEditorMapa != null)
				btnEditorMapa.setSelected(false);
			if (this.modoEditor) {
				this.modoEditor = false;
				fecharJanelaEditor();
			}

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

	// === SISTEMA GENÉRICO DE DOMÍNIOS ===

	/**
	 * Registra e desenha um domínio no mapa. Se já existir um domínio com o mesmo
	 * ID, ele é removido antes.
	 */
	public void registrarDominio(Dominio dominio) {
		removerDominio(dominio.getId()); // Limpa qualquer domínio antigo com mesmo ID

		dominiosAtivos.put(dominio.getId(), dominio);

		List<Pane> celulasVisuais = new ArrayList<>();

		String donoNome = (dominio.getDono() != null) ? dominio.getDono().getNome() : "Fusão";
		System.out.println("MAPA: Desenhando domínio [" + dominio.getId() + "] ("
				+ dominio.getCoordenadas().size() + " tiles) — " + donoNome);

		// Carrega sprite overlay se o domínio tiver texturePath
		Image spriteOverlay = null;
		if (dominio.getTexturePath() != null) {
			try {
				spriteOverlay = ImageCache.get(dominio.getTexturePath(), CELL_SIZE, CELL_SIZE);
				if (spriteOverlay != null && spriteOverlay.isError())
					spriteOverlay = null;
			} catch (Exception e) {
				System.err.println("MAPA: Sprite de domínio não encontrado: " + dominio.getTexturePath());
			}
		}

		String fxId = "dominio-fx-" + dominio.getId();

		// Itera pelas coordenadas reais do domínio (suporta formas irregulares/fusões)
		for (long coordKey : dominio.getCoordenadas()) {
			int x = (int) (coordKey >> 32);
			int y = (int) coordKey;
			if (x >= 0 && x < gridLargura && y >= 0 && y < gridAltura) {
				Pane celulaDoGrid = obterCelula(x,y);
				if (celulaDoGrid != null) {
					if(!celulaDoGrid.getStyleClass().contains(dominio.getCssClass()))
                        celulaDoGrid.getStyleClass().add(dominio.getCssClass());
					celulasVisuais.add(celulaDoGrid);

					// Adiciona sprite overlay (ImageView) se disponível
					if (spriteOverlay != null) {
						ImageView fxView = new ImageView(spriteOverlay);
						fxView.setSmooth(false);
						fxView.setFitWidth(CELL_SIZE);
						fxView.setFitHeight(CELL_SIZE);
						fxView.setPreserveRatio(false);
						fxView.setMouseTransparent(true);
						fxView.setOpacity(dominio.getOverlayOpacity());
						fxView.setId(fxId);
						celulaDoGrid.getChildren().add(fxView);
					}
				}
			}
		}
		celulasVisuaisDominios.put(dominio.getId(), celulasVisuais);
	}

	/** Remove um domínio pelo ID, limpando visuais e dados lógicos. */
    public void removerDominio(String dominioId) {
        Dominio removido=dominiosAtivos.remove(dominioId);
        celulasVisuaisDominios.remove(dominioId);
        if(removido==null)return;
        String fxId="dominio-fx-"+dominioId;
        for(long chave:removido.getCoordenadas()) {
            int x=(int)(chave>>32),y=(int)chave;
            if(!dentroDoGrid(x,y))continue;
            Pane cell=obterCelula(x,y);
            boolean outraFonte=dominiosAtivos.values().stream().anyMatch(d ->
                    java.util.Objects.equals(d.getCssClass(),removido.getCssClass()) && d.contemCoordenada(x,y));
            if(!outraFonte)cell.getStyleClass().removeIf(c -> java.util.Objects.equals(c,removido.getCssClass()));
            cell.getChildren().removeIf(n -> fxId.equals(n.getId()));
        }
    }

	/** Remove todos os domínios ativos do mapa. */
	public void limparTodosDominios() {
		for (String id : new ArrayList<>(dominiosAtivos.keySet())) {
			removerDominio(id);
		}
	}

	/** Retorna o domínio ativo pelo ID, ou null. */
	public Dominio getDominio(String dominioId) {
		return dominiosAtivos.get(dominioId);
	}

	/** Retorna todos os domínios ativos. */
	public Map<String, Dominio> getDominiosAtivos() {
		return dominiosAtivos;
	}

	/**
	 * Verifica se um personagem está dentro de um domínio específico.
	 * Também checa domínios fundidos — se o personagem está dentro de uma fusão
	 * que contém o domínio original, retorna true.
	 */
	public boolean isPersonagemNoDominio(Personagem p, String dominioId) {
		// Checagem direta
		Dominio dom = dominiosAtivos.get(dominioId);
		if (dom != null && dom.contemPersonagem(p))
			return true;

		// Checagem em fusões — se algum domínio fundido contém o original
		for (Dominio ativo : dominiosAtivos.values()) {
			if (ativo.isFusao() && ativo.contemPersonagem(p)) {
				for (Dominio original : ativo.getDominiosOriginais()) {
					if (original.getId().equals(dominioId))
						return true;
				}
			}
		}
		return false;
	}

	// === MÉTODOS RETROCOMPATÍVEIS (delegam ao sistema genérico) ===

	public void desenharRingueAlexei(Personagem centro, int tamanho) {
		Dominio ringue = new Dominio("ringue_alexei", "Ringue da Vontade", centro, centro.getPosX(), centro.getPosY(),
				tamanho, "zona-dominio-alexei");
		registrarDominio(ringue);
	}

	public void limparRingueAlexei() {
		removerDominio("ringue_alexei");
	}

	public void desenharDominioLyria(Personagem centro, int tamanho) {
		Dominio dominio = new Dominio("dominio_lyria", "Domínio: Idle Death Gamble", centro, centro.getPosX(),
				centro.getPosY(), tamanho, "zona-dominio-lyria");
		registrarDominio(dominio);
	}

	public void limparDominioLyria() {
		removerDominio("dominio_lyria");
	}

	public void desenharAuraDarrell(Personagem centro) {
		if (tokenRenderer != null)
			tokenRenderer.desenharAuraDarrell(centro);
	}

	// --- AURA PRESENÇA DO ZERO (Zeraphon) ---
	public void desenharAuraZero(Personagem centro) {
		if (tokenRenderer != null)
			tokenRenderer.desenharAuraZero(centro);
	}

	// --- AURA DE SANGUE (Lillith — Mergulho) ---
	public void desenharAuraSangue(Personagem centro) {
		if (tokenRenderer != null)
			tokenRenderer.desenharAuraSangue(centro);
	}

	// --- AURA BAD OMEN (Vampiro V2) ---
	public void desenharAuraBadOmen(Personagem centro) {
		if (tokenRenderer != null)
			tokenRenderer.desenharAuraBadOmen(centro);
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

	public void entrarModoSpawn(String idMonstro, int quantidade) {
		this.modoSpawnInimigo = true;
		this.idMonstroEmSpawn = idMonstro;
		this.cargasSpawnRestantes = quantidade;

		sairModoSelecao();
		mapGrid.getScene().setCursor(javafx.scene.Cursor.CLOSED_HAND);
		System.out.println("MAPA: Modo Spawn Ativo (" + quantidade + " cargas) para: " + idMonstro);
	}

	/** Ativa a seleção, no mapa, de um epicentro para uma explosão ambiental. */
	public void entrarModoSelecaoExplosaoAmbiental(Consumer<PontoMapa> aoSelecionar, Runnable aoCancelar) {
		sairModoSelecao();
		this.modoSelecaoExplosaoAmbiental = true;
		this.aoSelecionarExplosaoAmbiental = aoSelecionar;
		this.aoCancelarSelecaoExplosaoAmbiental = aoCancelar;
		setMoverMode(false);
		if (mapGrid != null && mapGrid.getScene() != null) {
			mapGrid.getScene().setCursor(javafx.scene.Cursor.CROSSHAIR);
		}
	}

	public void cancelarModoSelecaoExplosaoAmbiental() {
		boolean estavaSelecionando = modoSelecaoExplosaoAmbiental;
		Runnable aoCancelar = aoCancelarSelecaoExplosaoAmbiental;
		modoSelecaoExplosaoAmbiental = false;
		aoSelecionarExplosaoAmbiental = null;
		aoCancelarSelecaoExplosaoAmbiental = null;
		limparCanvas();
		if (mapGrid != null && mapGrid.getScene() != null) {
			mapGrid.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
		}
		if (estavaSelecionando && aoCancelar != null) {
			aoCancelar.run();
		}
	}

	/**
	 * Cancela o modo spawn local deste MapController. Chamado pelo CombatController
	 * quando as cargas se esgotam em qualquer instância, para manter embedded e
	 * externo sincronizados.
	 */
	public void cancelarModoSpawn() {
		this.modoSpawnInimigo = false;
		this.idMonstroEmSpawn = null;
		this.cargasSpawnRestantes = 0;
		if (mapGrid != null && mapGrid.getScene() != null) {
			mapGrid.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
		}
	}

	public br.com.dantesrpg.model.map.MapMetadata extrairMetadados() {
		br.com.dantesrpg.model.map.MapMetadata meta = new br.com.dantesrpg.model.map.MapMetadata();

		// Escaneia o Grid (Paredes, Saídas, Objetos)
		for (int x = 0; x < gridLargura; x++) {
			for (int y = 0; y < gridAltura; y++) {
				meta.adicionarTile(x,y,estadoMapa.getTile(x,y).getId());
				if (estadoMapa.getTerreno(x,y)==TipoTerreno.PAREDE) {
					meta.getParedes().add(new br.com.dantesrpg.model.map.MapMetadata.Ponto(x, y));
				} else if (estadoMapa.getTerreno(x,y)==TipoTerreno.SAIDA) {
					meta.getSaidas().add(new br.com.dantesrpg.model.map.MapMetadata.Ponto(x, y));
				} else if (estadoMapa.getTerreno(x,y)==TipoTerreno.OBJETO) {
					// Salva a posição do objeto (HP padrão por enquanto)
					meta.getObjetos().add(new br.com.dantesrpg.model.map.MapMetadata.ObjetoData(x, y,
                            getPersonagemNaCelula(x,y) instanceof br.com.dantesrpg.model.elementos.ObjetoDestrutivel objeto ? (int)Math.ceil(objeto.getVidaAtual()) : 50));
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

					// TRUQUE: Vou salvar o nome base limpo e torcer para bater com o bestiário, ou
					// idealmente, adicionar um campo 'idOriginal' no Personagem. (muita FÉ)

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
		squadHandler.iniciarSelecaoSquad(clones, habilidade, rolagem);
	}

	private void tratarCliqueSquad(Pane cell, int x, int y) {
		squadHandler.tratarCliqueSquad(x, y);
	}

	@FXML
	private void onPularSquadClick() {
		squadHandler.pularSquad();
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
        if(meta==null)return;
        TileRegistry registro=TileRegistry.getInstance();
        for(int x=0;x<gridLargura;x++)for(int y=0;y<gridAltura;y++) {
            TipoTerreno tipo=estadoMapa.getTerreno(x,y);
            if(tipo==TipoTerreno.PAREDE || tipo==TipoTerreno.SAIDA || tipo==TipoTerreno.OBJETO)
                estadoMapa.aplicarTile(x,y,registro.getById("floor"));
        }
        if(mainController!=null)mainController.getCombatentes().removeIf(p -> p instanceof br.com.dantesrpg.model.elementos.ObjetoDestrutivel);
        for(var p:meta.getTiles()) {
            TileDefinition tile=registro.getById(p.id());
            if(tile==null)throw new IllegalArgumentException("Tile desconhecido nos metadados: "+p.id());
            estadoMapa.aplicarTile(p.x(),p.y(),tile);
        }
        for(var p:meta.getParedes()) estadoMapa.aplicarTile(p.x,p.y,registro.getById("wall"));
        for(var p:meta.getSaidas()) estadoMapa.aplicarTile(p.x,p.y,tileDoTipo(TipoTerreno.SAIDA));
        for(var p:meta.getObjetos()) if(dentroDoGrid(p.x,p.y)) {
            estadoMapa.aplicarTile(p.x,p.y,tileDoTipo(TipoTerreno.OBJETO));
            if(mainController!=null) {
                mainController.criarObjetoNoMapa(p.x,p.y,Math.max(1,p.hp));
            }
        }
        if(mainController!=null)for(var p:meta.getInimigos())if(dentroDoGrid(p.x,p.y))mainController.spawnarMonstro(p.idMonstro,p.x,p.y);
        desenharPeoes(getCombatentes());
    }

    private TileDefinition tileDoTipo(TipoTerreno tipo) {
        return TileRegistry.getInstance().getAllTiles().stream()
                .filter(t -> tipo.name().equals(t.getTerrainType())).findFirst().orElse(TileRegistry.getInstance().getDefault());
    }

	private boolean dentroDoGrid(int x, int y) {
		return x >= 0 && x < gridLargura && y >= 0 && y < gridAltura;
	}

	public br.com.dantesrpg.model.map.TerrainData.EfeitoInstance getEfeitoNoSolo(int x, int y) {
		if (gridEfeitos == null || !dentroDoGrid(x, y)
				|| x >= gridEfeitos.length || y >= gridEfeitos[x].length) {
			return null;
		}
		return gridEfeitos[x][y];
	}

	public boolean estaEmAguaProfunda(Personagem personagem) {
		if (personagem == null) {
			return false;
		}
		EfeitoInstance efeito = getEfeitoNoSolo(personagem.getPosX(), personagem.getPosY());
		return efeito != null && efeito.getTipo() == TipoEfeitoSolo.AGUA_PROFUNDA;
	}

	public TipoTerreno getTerreno(int x, int y) {
		if (!dentroDoGrid(x, y) || gridTerreno == null)
			return TipoTerreno.PADRAO;
		return gridTerreno[x][y];
	}

	public boolean isCelulaDisponivelParaSpawn(int x, int y) {
		return dentroDoGrid(x, y) && !paredesGrid[x][y] && getPersonagemNaCelula(x, y) == null;
	}

	public record PontoMapa(int x, int y) {
	}

	private void limparHitboxesExtras() {
		if (tokenRenderer != null)
			tokenRenderer.limparHitboxesExtras();
	}

}
