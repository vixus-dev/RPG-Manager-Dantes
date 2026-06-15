package br.com.dantesrpg.controller;

import br.com.dantesrpg.model.map.TileDefinition;
import br.com.dantesrpg.model.map.TileRegistry;
import br.com.dantesrpg.model.util.FileLoader;
import br.com.dantesrpg.model.util.ImageCache;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.InputStream;
import java.util.List;

/**
 * Controller da janela de catálogo do Editor de Mapa.
 * Exibe todos os tiles agrupados por categoria e permite selecionar qual tile pintar no mapa.
 */
public class EditorWindowController {

	@FXML
	private Accordion accordionCategorias;

	@FXML
	private Label labelTileSelecionado;

	@FXML
	private Pane previewTile;

	private MapController mapController;

	@FXML
	public void initialize() {
		TileRegistry registry = TileRegistry.getInstance();
		if (!registry.isLoaded()) {
			registry.load();
		}

		construirCatalogo(registry);
	}

	public void setMapController(MapController mapController) {
		this.mapController = mapController;

		// Se já havia um tile selecionado no mapa, restaura a UI
		if (mapController.getEditorTile() != null) {
			atualizarSelecaoVisual(mapController.getEditorTile());
		}
	}

	private void construirCatalogo(TileRegistry registry) {
		List<String> categorias = registry.getCategories();

		for (String categoria : categorias) {
			List<TileDefinition> tiles = registry.getTilesInCategory(categoria);

			FlowPane flowPane = new FlowPane();
			flowPane.setHgap(6);
			flowPane.setVgap(6);
			flowPane.setPadding(new Insets(8));
			flowPane.setStyle("-fx-background-color: #1a1a2e;");

			for (TileDefinition tile : tiles) {
				Pane botaoTile = criarBotaoTile(tile);
				flowPane.getChildren().add(botaoTile);
			}

			TitledPane titledPane = new TitledPane(categoria + " (" + tiles.size() + ")", flowPane);
			titledPane.setStyle("-fx-text-fill: white;");
			titledPane.setAnimated(true);

			accordionCategorias.getPanes().add(titledPane);
		}

		// Expande a primeira categoria por padrão
		if (!accordionCategorias.getPanes().isEmpty()) {
			accordionCategorias.setExpandedPane(accordionCategorias.getPanes().get(0));
		}
	}

	/**
	 * Cria um botão visual (40x40) para um tile, com textura ou cor de fallback.
	 */
	private Pane criarBotaoTile(TileDefinition tile) {
		final int SIZE = 44;

		StackPane container = new StackPane();
		container.setPrefSize(SIZE, SIZE);
		container.setMinSize(SIZE, SIZE);
		container.setMaxSize(SIZE, SIZE);
		container.setStyle(
				"-fx-border-color: #555; -fx-border-width: 1; -fx-cursor: hand; -fx-background-color: #222;");

		// Tenta carregar a textura do tile
		boolean texturaCarregada = false;
		if (tile.getTexturePath() != null && !tile.getTexturePath().isEmpty()) {
			try {
				String path = "/br/com/dantesrpg/textures/" + tile.getTexturePath();
				Image img = ImageCache.get(path, SIZE - 2, SIZE - 2);
				if (img != null && !img.isError()) {
					ImageView iv = new ImageView(img);
					iv.setFitWidth(SIZE - 2);
					iv.setFitHeight(SIZE - 2);
					iv.setPreserveRatio(false);
					container.getChildren().add(iv);
					texturaCarregada = true;
				}
			} catch (Exception e) {
				// Fallback silencioso
			}
		}

		// Fallback: quadrado com a cor RGB do tile
		if (!texturaCarregada && tile.getRgb() != null) {
			Rectangle rect = new Rectangle(SIZE - 2, SIZE - 2);
			TileDefinition.RGBColor rgb = tile.getRgb();
			rect.setFill(Color.rgb(
					Math.min(255, Math.max(0, rgb.r)),
					Math.min(255, Math.max(0, rgb.g)),
					Math.min(255, Math.max(0, rgb.b))));
			rect.setStroke(Color.GRAY);
			container.getChildren().add(rect);
		}

		// Indicador visual: ícone de cadeado se não-andável
		if (!tile.isWalkable()) {
			Label lockIcon = new Label("🚫");
			lockIcon.setStyle("-fx-font-size: 10px;");
			StackPane.setAlignment(lockIcon, Pos.TOP_RIGHT);
			container.getChildren().add(lockIcon);
		}

		// Indicador visual: ícone de perigo se tem efeito
		if (tile.getEffect() != null) {
			Label fxIcon = new Label("⚠");
			fxIcon.setStyle("-fx-font-size: 10px;");
			StackPane.setAlignment(fxIcon, Pos.BOTTOM_LEFT);
			container.getChildren().add(fxIcon);
		}

		// Tooltip com info detalhada
		StringBuilder tooltipText = new StringBuilder();
		tooltipText.append(tile.getName());
		tooltipText.append("\nID: ").append(tile.getId());
		tooltipText.append("\nTerreno: ").append(tile.getTerrainType());
		tooltipText.append("\nAndável: ").append(tile.isWalkable() ? "Sim" : "Não");
		if (tile.getEffect() != null) {
			tooltipText.append("\nEfeito: ").append(tile.getEffect().getTipo());
			tooltipText.append(" (").append(tile.getEffect().getDano()).append(" dano)");
		}
		if (tile.getRgb() != null) {
			tooltipText.append("\nRGB: (").append(tile.getRgb().r).append(",")
					.append(tile.getRgb().g).append(",").append(tile.getRgb().b).append(")");
		}
		Tooltip tooltip = new Tooltip(tooltipText.toString());
		tooltip.setStyle("-fx-font-size: 12px;");
		Tooltip.install(container, tooltip);

		// Hover effect
		container.setOnMouseEntered(e -> container.setStyle(
				"-fx-border-color: #e94560; -fx-border-width: 2; -fx-cursor: hand; -fx-background-color: #333;"));
		container.setOnMouseExited(e -> {
			boolean isSelected = mapController != null && mapController.getEditorTile() == tile;
			if (isSelected) {
				container.setStyle(
						"-fx-border-color: #00ff88; -fx-border-width: 2; -fx-cursor: hand; -fx-background-color: #2a3a2a;");
			} else {
				container.setStyle(
						"-fx-border-color: #555; -fx-border-width: 1; -fx-cursor: hand; -fx-background-color: #222;");
			}
		});

		// Click: seleciona o tile
		container.setOnMouseClicked(e -> {
			selecionarTile(tile);
		});

		return container;
	}

	/**
	 * Seleciona um tile e notifica o MapController.
	 */
	private void selecionarTile(TileDefinition tile) {
		if (mapController != null) {
			mapController.setEditorTile(tile);
		}
		atualizarSelecaoVisual(tile);
	}

	/**
	 * Atualiza a label e preview do tile selecionado.
	 */
	private void atualizarSelecaoVisual(TileDefinition tile) {
		if (labelTileSelecionado != null) {
			labelTileSelecionado.setText(tile.getName());
		}

		if (previewTile != null) {
			previewTile.getChildren().clear();
			previewTile.setStyle("-fx-border-color: #00ff88; -fx-border-width: 2;");

			// Tenta carregar textura para o preview
			if (tile.getTexturePath() != null && !tile.getTexturePath().isEmpty()) {
				try {
					String path = "/br/com/dantesrpg/textures/" + tile.getTexturePath();
					Image img = ImageCache.get(path, 30, 30);
					if (img != null && !img.isError()) {
						ImageView iv = new ImageView(img);
						iv.setFitWidth(30);
						iv.setFitHeight(30);
						iv.setPreserveRatio(false);
						previewTile.getChildren().add(iv);
						return;
					}
				} catch (Exception e) {
					// Fallback
				}
			}

			// Fallback cor RGB
			if (tile.getRgb() != null) {
				TileDefinition.RGBColor rgb = tile.getRgb();
				Rectangle rect = new Rectangle(30, 30, Color.rgb(rgb.r, rgb.g, rgb.b));
				previewTile.getChildren().add(rect);
			}
		}
	}

	/**
	 * Botão "Borracha" — seleciona o tile padrão (chão).
	 */
	@FXML
	private void onBorrachaClick() {
		TileRegistry registry = TileRegistry.getInstance();
		TileDefinition defaultTile = registry.getDefault();
		if (defaultTile != null) {
			selecionarTile(defaultTile);
		}
	}
}
