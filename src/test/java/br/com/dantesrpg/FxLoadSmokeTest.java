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
import javafx.scene.layout.Region;

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
				String[] views = {
						"CombatView.fxml",
						"LojaView.fxml",
						"EditorJogadorView.fxml",
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
					int limite = andar == AndarCampanha.ANDAR_4 ? 3 : andar == AndarCampanha.ANDAR_5 ? 2 : 1;
					for (int estado = 1; estado <= limite; estado++) {
						temaService.aplicarTema(catalogo.buscarPorEstado(new EstadoAndarParty(andar, estado)));
					}
				}
				temaService.aplicarTema(catalogo.getConfiguracaoNula());
				System.out.println("TEMAS_SMOKE_OK=12_VARIANTES_E_NULO");
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
}
