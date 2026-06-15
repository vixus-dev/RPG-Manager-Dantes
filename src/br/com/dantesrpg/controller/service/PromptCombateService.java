package br.com.dantesrpg.controller.service;

import java.util.Optional;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import br.com.dantesrpg.controller.AttributeTestPromptController;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.racas.Humano;
import br.com.dantesrpg.model.util.DiceRoller;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class PromptCombateService {

	private final Supplier<EstadoCombate> estadoSupplier;
	private final Supplier<CombatManager> combatManagerSupplier;
	private final Supplier<Stage> detailedTurnHudStageSupplier;
	private final JanelasCombateCoordinator janelasCombateCoordinator;
	private final IntSupplier bonusDificuldadeAndarSupplier;
	private final Runnable atualizarInterfaceTotal;
	private final Runnable popularListasDeCombatentes;

	private Personagem reacaoAtor;
	private Personagem reacaoAlvo;
	private int reacaoNivelCascata;

	public PromptCombateService(Supplier<EstadoCombate> estadoSupplier,
			Supplier<CombatManager> combatManagerSupplier, Supplier<Stage> detailedTurnHudStageSupplier,
			JanelasCombateCoordinator janelasCombateCoordinator, IntSupplier bonusDificuldadeAndarSupplier,
			Runnable atualizarInterfaceTotal, Runnable popularListasDeCombatentes) {
		this.estadoSupplier = estadoSupplier;
		this.combatManagerSupplier = combatManagerSupplier;
		this.detailedTurnHudStageSupplier = detailedTurnHudStageSupplier;
		this.janelasCombateCoordinator = janelasCombateCoordinator;
		this.bonusDificuldadeAndarSupplier = bonusDificuldadeAndarSupplier;
		this.atualizarInterfaceTotal = atualizarInterfaceTotal;
		this.popularListasDeCombatentes = popularListasDeCombatentes;
	}

	public boolean solicitarTesteDeAtributo(Personagem personagem, Atributo atributo, int dificuldadeNA) {
		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("Teste de Resistência: " + personagem.getNome());
		dialog.setHeaderText("Teste de " + atributo.name() + " (NA " + dificuldadeNA + ")");
		dialog.setContentText("O Jogador deve rodar 1d20 + " + atributo.name()
				+ ".\nInsira o resultado final:");
		dialog.getDialogPane().setStyle("-fx-background-color: #222;");
		dialog.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: white;");

		Optional<String> result = dialog.showAndWait();
		if (result.isPresent()) {
			try {
				int valor = Integer.parseInt(result.get());
				boolean sucesso = valor >= dificuldadeNA;
				String msg = sucesso ? "SUCESSO" : "FALHA";
				System.out.println(">>> TESTE (" + valor + " vs NA " + dificuldadeNA + "): " + msg);

return sucesso;
			} catch (NumberFormatException e) {
				System.out.println(">>> Entrada inválida. Considerando Falha.");
				return false;
			}
		}
		return false;
	}

	public void abrirResolucaoEmprestimo(Personagem personagem, Humano humano) {
		try {
			FXMLLoader loader = new FXMLLoader(
					getClass().getResource("/br/com/dantesrpg/view/AttributeTestPrompt.fxml"));
			Parent root = loader.load();
			AttributeTestPromptController controller = loader.getController();

			Stage stage = new Stage();
			stage.setTitle("Fim do Empréstimo");
			stage.setScene(new Scene(root));
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setOnCloseRequest(e -> e.consume());

			int bonusDificuldadeAndar = bonusDificuldadeAndarSupplier.getAsInt();
			int na = humano.calcularDificuldadeTeste(personagem) + bonusDificuldadeAndar;
			String difTexto = humano.getTextoDificuldade(na - bonusDificuldadeAndar);
			controller.setDados(stage, personagem.getNome(), na, difTexto);
			stage.showAndWait();

			if (controller.isConfirmado()) {
				int rolagem = controller.getResultadoRolagem();
				int atributoEndurance = personagem.getAtributosFinais().getOrDefault(Atributo.ENDURANCE, 0);
				int total = rolagem + atributoEndurance;
				boolean sucesso = total >= na;

				humano.resolverResultadoTeste(personagem, sucesso, total);

atualizarInterfaceTotal.run();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean solicitarTesteGatilhoVeloz(Personagem pistoleiro, Personagem atacante) {
		try {
			FXMLLoader loader = new FXMLLoader(
					getClass().getResource("/br/com/dantesrpg/view/AttributeTestPrompt.fxml"));
			Parent root = loader.load();
			AttributeTestPromptController controller = loader.getController();

			Stage stage = new Stage();
			stage.setTitle("Gatilho Veloz");
			stage.setScene(new Scene(root));
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setOnCloseRequest(e -> e.consume());

			int destreza = pistoleiro.getAtributosFinais().getOrDefault(Atributo.DESTREZA, 1);
			int dadoMaximo = DiceRoller.getTipoDado(destreza);
			int dificuldade = dadoMaximo / 2;
			controller.setDadosPersonalizados(stage, "GATILHO VELOZ",
					pistoleiro.getNome() + " reage ao ataque de " + atacante.getNome() + ".",
					"> " + dificuldade + " no d" + dadoMaximo,
					"Role AGILIDADE/DESTREZA (d" + dadoMaximo + "):");

			stage.showAndWait();
			if (!controller.isConfirmado()) {
				return false;
			}

			int rolagem = controller.getResultadoRolagem();
			boolean sucesso = rolagem > dificuldade;

return sucesso;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public void solicitarRolagemFantasmaDoDeserto(Personagem ator, Personagem alvo) {
		System.out.println(">>> REAÇÃO: Fantasma do Deserto ativado!");
		this.reacaoAtor = ator;
		this.reacaoAlvo = alvo;
		this.reacaoNivelCascata = 1;

		Stage detailedTurnHudStage = detailedTurnHudStageSupplier.get();
		if (detailedTurnHudStage != null && detailedTurnHudStage.isShowing()) {
			detailedTurnHudStage.getScene().getRoot().setDisable(true);
		}

		janelasCombateCoordinator.abrirJanelaDeRolagem("Fantasma do Deserto! (Nível " + reacaoNivelCascata + ")",
				"1d4 (Falha em 2 ou menos)");
	}

	public void resolverRolagemReacao(String rolagemStr) {
		int rolagem;
		try {
			rolagem = Integer.parseInt(rolagemStr);
		} catch (Exception e) {
			System.err.println("Rolagem inválida! Insira um número.");
			return;
		}

		boolean continuarCascata = combatManagerSupplier.get().aplicarDanoFantasmaDoDeserto(reacaoAtor, reacaoAlvo,
				rolagem, reacaoNivelCascata, estadoSupplier.get());

		if (continuarCascata) {
			reacaoNivelCascata++;
			janelasCombateCoordinator.prepararRolagem("Moeda Extra! (Nível " + reacaoNivelCascata + ")",
					"1d4 (Falha em 2 ou menos)");
			return;
		}

		System.out.println(">>> REAÇÃO: Fantasma do Deserto terminou.");
		janelasCombateCoordinator.fecharRolagem();

		Stage detailedTurnHudStage = detailedTurnHudStageSupplier.get();
		if (detailedTurnHudStage != null && detailedTurnHudStage.isShowing()) {
			detailedTurnHudStage.getScene().getRoot().setDisable(false);
			detailedTurnHudStage.requestFocus();
		}

		popularListasDeCombatentes.run();
	}
}
