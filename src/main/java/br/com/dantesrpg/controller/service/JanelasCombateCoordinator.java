package br.com.dantesrpg.controller.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import br.com.dantesrpg.controller.BestiarioController;
import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.controller.DamageResolutionController;
import br.com.dantesrpg.controller.DiceRollPromptController;
import br.com.dantesrpg.controller.GerenciadorCombateController;
import br.com.dantesrpg.controller.MapController;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.util.DamageEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class JanelasCombateCoordinator {

	private final CombatController controller;
	private final Supplier<EstadoCombate> estadoSupplier;
	private final Supplier<CombatManager> combatManagerSupplier;
	private final Supplier<Map<String, Map<String, Object>>> armorySupplier;
	private final Supplier<Map<String, Map<String, Object>>> itempediaSupplier;
	private final Supplier<Map<String, Map<String, Object>>> bestiarioSupplier;
	private final Supplier<File> arquivoMapaAtualSupplier;
	private final Consumer<MapController> mapControllerSetter;
	private final Consumer<File> carregarMetadadosDoMapa;
	private final Supplier<Stage> detailedTurnHudStageSupplier;

	private Stage mapStage;
	private MapController mapController;
	private Stage diceRollStage;
	private DiceRollPromptController diceRollController;
	private Stage bestiarioStage;
	private BestiarioController bestiarioController;
	private GerenciadorCombateController gerenciadorCombateController;

	public JanelasCombateCoordinator(CombatController controller, Supplier<EstadoCombate> estadoSupplier,
			Supplier<CombatManager> combatManagerSupplier,
			Supplier<Map<String, Map<String, Object>>> armorySupplier,
			Supplier<Map<String, Map<String, Object>>> itempediaSupplier,
			Supplier<Map<String, Map<String, Object>>> bestiarioSupplier, Supplier<File> arquivoMapaAtualSupplier,
			Consumer<MapController> mapControllerSetter, Consumer<File> carregarMetadadosDoMapa,
			Supplier<Stage> detailedTurnHudStageSupplier) {
		this.controller = controller;
		this.estadoSupplier = estadoSupplier;
		this.combatManagerSupplier = combatManagerSupplier;
		this.armorySupplier = armorySupplier;
		this.itempediaSupplier = itempediaSupplier;
		this.bestiarioSupplier = bestiarioSupplier;
		this.arquivoMapaAtualSupplier = arquivoMapaAtualSupplier;
		this.mapControllerSetter = mapControllerSetter;
		this.carregarMetadadosDoMapa = carregarMetadadosDoMapa;
		this.detailedTurnHudStageSupplier = detailedTurnHudStageSupplier;
	}

	public void abrirMapaExterno() {
		try {
			if (mapStage == null) {
				FXMLLoader loader = new FXMLLoader(
						controller.getClass().getResource("/br/com/dantesrpg/view/MapView.fxml"));
				Parent mapRoot = loader.load();

				mapController = loader.getController();
				mapController.setMainController(controller);
				mapControllerSetter.accept(mapController);

				mapStage = new Stage();
				mapStage.setTitle("Modo Combate");
				mapStage.setScene(new Scene(mapRoot));
				mapStage.setResizable(true);

				sincronizarMapaExternoComMapaAtual();
			}
			mapStage.show();
			mapStage.toFront();
		} catch (Exception e) {
			System.err.println("Erro crítico ao carregar MapView.fxml ou imagem do mapa:");
			e.printStackTrace();
		}
	}

	private void sincronizarMapaExternoComMapaAtual() {
		File arquivoMapaAtual = arquivoMapaAtualSupplier.get();
		if (arquivoMapaAtual == null || mapController == null) {
			return;
		}

		mapController.carregarMapaDeImagem(arquivoMapaAtual);
		carregarMetadadosDoMapa.accept(arquivoMapaAtual);
		EstadoCombate estado = estadoSupplier.get();
		if (estado != null) {
			mapController.desenharPeoes(estado.getCombatentes());
		}
	}

	public void abrirJanelaResolucao(Personagem atacante, List<Personagem> alvos, Habilidade habilidade,
			Map<Personagem, List<DamageEvent>> mapaDanos) {
		try {
			FXMLLoader loader = new FXMLLoader(
					controller.getClass().getResource("/br/com/dantesrpg/view/DamageResolutionView.fxml"));
			Parent root = loader.load();

			DamageResolutionController resolutionController = loader.getController();
			resolutionController.setMainController(controller);
			resolutionController.setupResolution(atacante, habilidade, mapaDanos, estadoSupplier.get());

			Stage stage = new Stage();
			stage.setTitle("Resolução de Dano");
			stage.setScene(new Scene(root));
			stage.setHeight(Screen.getPrimary().getVisualBounds().getHeight() * 0.80);
			stage.setOnHidden(e -> {
				CombatManager combatManager = combatManagerSupplier.get();
				if (combatManager != null) {
					combatManager.cancelarMunicaoPendente();
				}
			});
			stage.show();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Erro ao abrir janela de resolução de dano.");
		}
	}

	public void abrirJanelaDeRolagem(String textoPrompt, String promptDado) {
		try {
			if (diceRollStage == null) {
				FXMLLoader loader = new FXMLLoader(
						controller.getClass().getResource("/br/com/dantesrpg/view/DiceRollPrompt.fxml"));
				Parent root = loader.load();
				diceRollController = loader.getController();
				diceRollController.setMainController(controller);

				diceRollStage = new Stage();
				diceRollStage.initModality(Modality.APPLICATION_MODAL);
				Stage detailedTurnHudStage = detailedTurnHudStageSupplier.get();
				if (detailedTurnHudStage != null) {
					diceRollStage.initOwner(detailedTurnHudStage);
				}
				diceRollStage.setScene(new Scene(root));
				diceRollStage.setTitle("Rolagem de Reação Requerida!");
				diceRollStage.setOnCloseRequest(e -> e.consume());
			}
			prepararRolagem(textoPrompt, promptDado);
			diceRollStage.show();
			diceRollController.requestFocus();
		} catch (Exception e) {
			System.err.println("Erro crítico ao carregar DiceRollPrompt.fxml:");
			e.printStackTrace();
		}
	}

	public void prepararRolagem(String textoPrompt, String promptDado) {
		if (diceRollController != null) {
			diceRollController.preparar(textoPrompt, promptDado);
		}
	}

	public void fecharRolagem() {
		if (diceRollStage != null) {
			diceRollStage.hide();
		}
	}

	public void abrirGerenciadorCombate() {
		try {
			FXMLLoader loader = new FXMLLoader(
					controller.getClass().getResource("/br/com/dantesrpg/view/GerenciadorCombateView.fxml"));
			Parent root = loader.load();

			GerenciadorCombateController painelController = loader.getController();
			painelController.setMainController(controller, estadoSupplier.get());

			List<String> todosItens = new ArrayList<>();
			Map<String, Map<String, Object>> itempediaDatabase = itempediaSupplier.get();
			Map<String, Map<String, Object>> armoryDatabase = armorySupplier.get();
			if (itempediaDatabase != null) {
				todosItens.addAll(itempediaDatabase.keySet());
			}
			if (armoryDatabase != null) {
				todosItens.addAll(armoryDatabase.keySet());
			}
			painelController.setListaItensMestre(todosItens);
			painelController.setListaItensComInfo(todosItens, controller);

			gerenciadorCombateController = painelController;

			Stage stage = new Stage();
			stage.setTitle("Painel do Mestre");
			stage.setScene(new Scene(root));
			stage.setOnCloseRequest(e -> gerenciadorCombateController = null);
			stage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void notificarGerenciadorCombate() {
		if (gerenciadorCombateController != null) {
			gerenciadorCombateController.refreshCompleto();
		}
	}

	public void abrirJanelaBestiario() {
		try {
			if (bestiarioStage == null) {
				FXMLLoader loader = new FXMLLoader(
						controller.getClass().getResource("/br/com/dantesrpg/view/BestiarioView.fxml"));
				Parent root = loader.load();
				bestiarioController = loader.getController();

				bestiarioStage = new Stage();
				bestiarioStage.setTitle("Bestiário");
				bestiarioStage.setScene(new Scene(root));
				bestiarioStage.initModality(Modality.WINDOW_MODAL);
				bestiarioController.setStage(bestiarioStage);
			}
			bestiarioController.inicializar(controller, bestiarioSupplier.get());
			bestiarioStage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
