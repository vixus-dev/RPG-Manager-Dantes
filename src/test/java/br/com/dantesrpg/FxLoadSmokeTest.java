package br.com.dantesrpg;

import java.io.File;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import br.com.dantesrpg.controller.service.TemaAndarService;
import br.com.dantesrpg.controller.util.ResponsiveGridPane;
import br.com.dantesrpg.main.ApplicationWindowIconService;
import br.com.dantesrpg.model.EstadoAndarParty;
import br.com.dantesrpg.model.enums.AndarCampanha;
import br.com.dantesrpg.model.theme.CatalogoTemasAndar;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import javax.imageio.ImageIO;

public final class FxLoadSmokeTest {
	private FxLoadSmokeTest() {
	}

	public static void main(String[] args) throws Exception {
		CountDownLatch conclusao = new CountDownLatch(1);
		AtomicReference<Throwable> falha = new AtomicReference<>();

		Platform.startup(() -> {
			try {
				TemaAndarService temaService = null;
				CatalogoTemasAndar catalogo = new CatalogoTemasAndar();
				Parent raizCombate = null;
				Parent raizLoja = null;
				Parent raizEditor = null;
				String[] views = {
						"CombatView.fxml",
						"LojaView.fxml",
						"EditorJogadorView.fxml",
						"CriarView.fxml",
						"GerenciadorCombateView.fxml",
						"DetailedTurnHUD.fxml",
						"DamageResolutionView.fxml",
                        "EmbeddedMapView.fxml",
						"DiceRollPrompt.fxml",
						"BestiarioView.fxml"
				};
				for (String view : views) {
					URL recurso = FxLoadSmokeTest.class.getResource("/br/com/dantesrpg/view/" + view);
					if (recurso == null) {
						throw new IllegalStateException("FXML não encontrado: " + view);
					}
					FXMLLoader loader = new FXMLLoader(recurso);
					Parent raiz = loader.load();
					if ("CombatView.fxml".equals(view)) {
						raizCombate = raiz;
						Region menu = (Region) loader.getNamespace().get("leftNavBar");
						Region barra = (Region) loader.getNamespace().get("gmToolbar");
						temaService = new TemaAndarService(raiz, menu, barra);
					} else if (temaService != null) {
						temaService.registrarRaiz(raiz);
					}
					if ("LojaView.fxml".equals(view)) {
						raizLoja = raiz;
					} else if ("EditorJogadorView.fxml".equals(view)) {
						raizEditor = raiz;
					}
					new Scene(raiz);
					raiz.applyCss();
					System.out.println("FXML_SMOKE_OK=" + view + ":" + raiz.getClass().getSimpleName());
				}

				if (temaService == null) {
					throw new IllegalStateException("Serviço de tema não foi inicializado.");
				}
				for (AndarCampanha andar : AndarCampanha.values()) {
					if (andar == AndarCampanha.NULO) {
						continue;
					}
					int inicio = andar == AndarCampanha.ANDAR_8 ? 0 : 1;
					int limite = switch (andar) {
					case ANDAR_4, ANDAR_5, ANDAR_7 -> 3;
					case ANDAR_6 -> 2;
					default -> 1;
					};
					for (int estado = inicio; estado <= limite; estado++) {
						temaService.aplicarTema(catalogo.buscarPorEstado(new EstadoAndarParty(andar, estado)));
					}
				}
				validarContrasteTemaClaro(temaService, catalogo, raizLoja);
				validarGridResponsivo();
				validarIconeGlobal();
				temaService.aplicarTema(catalogo.getConfiguracaoNula());
				gerarSnapshotsResponsivos(raizCombate, raizLoja, raizEditor);
				System.out.println("TEMAS_SMOKE_OK=17_VARIANTES_E_NULO");
			} catch (Throwable erro) {
				falha.set(erro);
				erro.printStackTrace();
			} finally {
				conclusao.countDown();
			}
		});

		boolean terminou = conclusao.await(30, TimeUnit.SECONDS);
		Platform.exit();
		if (!terminou) {
			throw new IllegalStateException("Timeout ao carregar o FXML principal.");
		}
		if (falha.get() != null) {
			throw new IllegalStateException("Falha no smoke test de FXML.", falha.get());
		}
	}

	private static void validarIconeGlobal() {
		Stage principal = new Stage();
		ApplicationWindowIconService.install(principal);
		if (principal.getIcons().isEmpty()) {
			throw new IllegalStateException("A janela principal não recebeu a logo.");
		}

		Stage auxiliar = new Stage();
		auxiliar.setOpacity(0);
		auxiliar.show();
		if (auxiliar.getIcons().isEmpty()) {
			throw new IllegalStateException("Uma janela criada depois do startup não recebeu a logo.");
		}
		auxiliar.close();
		ApplicationWindowIconService.uninstall();
		System.out.println("ICONE_GLOBAL_SMOKE_OK=JANELA_PRINCIPAL_E_AUXILIAR");
	}

	private static void gerarSnapshotsResponsivos(Parent raizCombate, Parent raizLoja, Parent raizEditor)
			throws Exception {
		File diretorio = new File("target/responsive-smoke");
		if (!diretorio.exists() && !diretorio.mkdirs()) {
			throw new IllegalStateException("Não foi possível criar o diretório de snapshots responsivos.");
		}
		gerarSnapshot(raizCombate, 800, 620, new File(diretorio, "combate-800x620.png"));
		gerarSnapshot(raizLoja, 680, 620, new File(diretorio, "loja-680x620.png"));
		gerarSnapshot(raizEditor, 820, 620, new File(diretorio, "editor-820x620.png"));
		System.out.println("SNAPSHOTS_RESPONSIVOS_OK=" + diretorio.getAbsolutePath());
	}

	private static void gerarSnapshot(Parent raiz, int largura, int altura, File destino) throws Exception {
		if (!(raiz instanceof Region)) {
			throw new IllegalStateException("A raiz responsiva precisa ser uma Region.");
		}
		Scene cenaAnterior = raiz.getScene();
		if (cenaAnterior != null) {
			cenaAnterior.setRoot(new Group());
		}
		new Scene(raiz, largura, altura);
		raiz.applyCss();
		raiz.layout();
		WritableImage imagem = new WritableImage(largura, altura);
		raiz.snapshot(null, imagem);
		if (!ImageIO.write(SwingFXUtils.fromFXImage(imagem, null), "png", destino)) {
			throw new IllegalStateException("Não foi possível gravar o snapshot " + destino);
		}
	}

	private static void validarGridResponsivo() {
		ResponsiveGridPane grid = new ResponsiveGridPane();
		grid.setHgap(10);
		grid.setMaxColumns(3);
		grid.setMinColumnWidth(280);
		grid.getChildren().addAll(new Region(), new Region(), new Region());

		validarNumeroColunas(grid, 1100, 3);
		validarNumeroColunas(grid, 700, 2);
		validarNumeroColunas(grid, 520, 1);
		System.out.println("RESPONSIVIDADE_SMOKE_OK=3_2_1_COLUNAS");
	}

	private static void validarNumeroColunas(ResponsiveGridPane grid, double largura, int esperado) {
		grid.resize(largura, 600);
		grid.layout();
		int atual = grid.getCurrentColumns();
		if (atual != esperado) {
			throw new IllegalStateException("Grid responsivo com " + largura + "px usou "
					+ atual + " colunas; esperado: " + esperado);
		}
	}

	private static void validarContrasteTemaClaro(TemaAndarService temaService,
			CatalogoTemasAndar catalogo, Parent raizLoja) {
		if (!(raizLoja instanceof Pane)) {
			throw new IllegalStateException("Raiz da loja não permite validar contraste.");
		}
		Label amostra = new Label("Item comum");
		amostra.getStyleClass().addAll("loja-item-nome", "raridade-texto-comum");
		StackPane cardInventario = new StackPane(amostra);
		cardInventario.getStyleClass().add("editor-inventory-item-card");
		((Pane) raizLoja).getChildren().add(cardInventario);

		temaService.aplicarTema(catalogo.buscarPorEstado(
				new EstadoAndarParty(AndarCampanha.ANDAR_5, 1)));
		raizLoja.applyCss();
		double contraste = calcularContraste((Color) amostra.getTextFill(), Color.web("#FFF8DF"));
		if (contraste < 4.5) {
			throw new IllegalStateException("Contraste insuficiente no tema Praia: " + contraste);
		}
		double opacidadeCard = ((Color) cardInventario.getBackground().getFills().get(0).getFill()).getOpacity();
		if (opacidadeCard > 0.72) {
			throw new IllegalStateException("Card de inventário não ficou translúcido: " + opacidadeCard);
		}
		((Pane) raizLoja).getChildren().remove(cardInventario);
		System.out.println("CONTRASTE_SMOKE_OK=" + String.format("%.2f", contraste)
				+ "; CARD_OPACIDADE=" + String.format("%.2f", opacidadeCard));
	}

	private static double calcularContraste(Color primeira, Color segunda) {
		double clara = Math.max(luminancia(primeira), luminancia(segunda));
		double escura = Math.min(luminancia(primeira), luminancia(segunda));
		return (clara + 0.05) / (escura + 0.05);
	}

	private static double luminancia(Color cor) {
		return 0.2126 * linearizar(cor.getRed())
				+ 0.7152 * linearizar(cor.getGreen())
				+ 0.0722 * linearizar(cor.getBlue());
	}

	private static double linearizar(double canal) {
		return canal <= 0.03928 ? canal / 12.92 : Math.pow((canal + 0.055) / 1.055, 2.4);
	}
}
