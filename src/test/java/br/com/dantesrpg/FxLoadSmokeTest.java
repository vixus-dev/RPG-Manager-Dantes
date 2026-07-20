package br.com.dantesrpg;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import br.com.dantesrpg.controller.service.TemaAndarService;
import br.com.dantesrpg.model.EstadoAndarParty;
import br.com.dantesrpg.model.enums.AndarCampanha;
import br.com.dantesrpg.model.theme.CatalogoTemasAndar;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

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
				Parent raizLoja = null;
				String[] views = {
						"CombatView.fxml",
						"LojaView.fxml",
						"EditorJogadorView.fxml",
						"CriarView.fxml",
						"GerenciadorCombateView.fxml",
						"DetailedTurnHUD.fxml",
						"DamageResolutionView.fxml",
						"MapView.fxml",
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
						Region menu = (Region) loader.getNamespace().get("leftNavBar");
						Region barra = (Region) loader.getNamespace().get("gmToolbar");
						temaService = new TemaAndarService(raiz, menu, barra);
					} else if (temaService != null) {
						temaService.registrarRaiz(raiz);
					}
					if ("LojaView.fxml".equals(view)) {
						raizLoja = raiz;
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
					int limite = switch (andar) {
					case ANDAR_4, ANDAR_5, ANDAR_7 -> 3;
					case ANDAR_6 -> 2;
					default -> 1;
					};
					for (int estado = 1; estado <= limite; estado++) {
						temaService.aplicarTema(catalogo.buscarPorEstado(new EstadoAndarParty(andar, estado)));
					}
				}
				validarContrasteTemaClaro(temaService, catalogo, raizLoja);
				temaService.aplicarTema(catalogo.getConfiguracaoNula());
				System.out.println("TEMAS_SMOKE_OK=16_VARIANTES_E_NULO");
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
