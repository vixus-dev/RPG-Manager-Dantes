package br.com.dantesrpg.controller.service;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import br.com.dantesrpg.controller.MapController;
import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Item;
import br.com.dantesrpg.model.Personagem;
import javafx.stage.Stage;

public class TurnoCombateCoordinator {

	private final Supplier<EstadoCombate> estadoSupplier;
	private final Supplier<CombatManager> combatManagerSupplier;
	private final Supplier<MapController> mapControllerSupplier;
	private final Supplier<Stage> detailedTurnHudStageSupplier;
	private final Consumer<Consumer<MapController>> forEachMap;
	private final Predicate<Personagem> devePassarSquadDeClones;
	private final Runnable passarTurnoSquadAtual;
	private final Consumer<Personagem> aplicarPassarVez;
	private final Runnable limparTUPreview;
	private final Runnable popularListasDeCombatentes;
	private final Runnable removerDestaques;
	private final Supplier<Personagem> getProximoAtorCalculado;
	private final BooleanSupplier verificarFimDeCombate;
	private final Runnable atualizarTimelineTU;

	public TurnoCombateCoordinator(Supplier<EstadoCombate> estadoSupplier,
			Supplier<CombatManager> combatManagerSupplier, Supplier<MapController> mapControllerSupplier,
			Supplier<Stage> detailedTurnHudStageSupplier, Consumer<Consumer<MapController>> forEachMap,
			Predicate<Personagem> devePassarSquadDeClones, Runnable passarTurnoSquadAtual,
			Consumer<Personagem> aplicarPassarVez, Runnable limparTUPreview, Runnable popularListasDeCombatentes,
			Runnable removerDestaques, Supplier<Personagem> getProximoAtorCalculado,
			BooleanSupplier verificarFimDeCombate, Runnable atualizarTimelineTU) {
		this.estadoSupplier = estadoSupplier;
		this.combatManagerSupplier = combatManagerSupplier;
		this.mapControllerSupplier = mapControllerSupplier;
		this.detailedTurnHudStageSupplier = detailedTurnHudStageSupplier;
		this.forEachMap = forEachMap;
		this.devePassarSquadDeClones = devePassarSquadDeClones;
		this.passarTurnoSquadAtual = passarTurnoSquadAtual;
		this.aplicarPassarVez = aplicarPassarVez;
		this.limparTUPreview = limparTUPreview;
		this.popularListasDeCombatentes = popularListasDeCombatentes;
		this.removerDestaques = removerDestaques;
		this.getProximoAtorCalculado = getProximoAtorCalculado;
		this.verificarFimDeCombate = verificarFimDeCombate;
		this.atualizarTimelineTU = atualizarTimelineTU;
	}

	public void resolverAcaoDoMestre(AcaoMestreInput input) {
		EstadoCombate estado = estadoSupplier.get();
		if (input.getAtor() != estado.getAtorAtual() || !estado.isCombateAtivo()) {
			return;
		}

		combatManagerSupplier.get().resolverAcao(input, estado);
		fecharHudEAvancar();
	}

	public void resolverAcaoPassarVez(AcaoMestreInput input) {
		EstadoCombate estado = estadoSupplier.get();
		if (input.getAtor() != estado.getAtorAtual() || !estado.isCombateAtivo()) {
			return;
		}

		Personagem ator = input.getAtor();
		if (devePassarSquadDeClones.test(ator)) {
			System.out.println("SQUAD: Encerrando o turno de todos os clones restantes.");
			passarTurnoSquadAtual.run();
			return;
		}

		aplicarPassarVez.accept(ator);
		fecharHudEAvancar();
	}

	public void resolverAcaoItem(AcaoMestreInput input) {
		EstadoCombate estado = estadoSupplier.get();
		if (input.getAtor() != estado.getAtorAtual() || !estado.isCombateAtivo()) {
			return;
		}

		Item item = input.getItemSendoUsado();
		if (item == null) {
			System.err.println("Erro: Ação de item sem item definido.");
			return;
		}

		combatManagerSupplier.get().resolverAcaoItem(input, estado);
		fecharHudEAvancar();
	}

	public void resolverAcaoFugir(Personagem ator) {
		EstadoCombate estado = estadoSupplier.get();
		if (ator != estado.getAtorAtual() || !estado.isCombateAtivo()) {
			return;
		}

		System.out.println(">>> " + ator.getNome() + " conseguiu fugir do combate!");
		ator.setFugiu(true);
		forEachMap.accept(m -> m.desenharPeoes(estado.getCombatentes()));
		fecharHudEAvancar();
	}

	public void verificarInteracaoTerreno(Personagem ator) {
		CombatManager combatManager = combatManagerSupplier.get();
		MapController mapController = mapControllerSupplier.get();
		if (combatManager != null && mapController != null) {
			combatManager.resolverTerrenoPerigoso(ator, mapController, estadoSupplier.get());
		}
	}

	public void iniciarSelecaoDeAlvo(Habilidade habilidade, Personagem ator) {
		Stage detailedTurnHudStage = detailedTurnHudStageSupplier.get();
		if (detailedTurnHudStage != null) {
			detailedTurnHudStage.hide();
		}

		forEachMap.accept(m -> m.entrarModoSelecao(habilidade, ator));
		if (mapControllerSupplier.get() == null) {
			System.err.println("Info: MapController externo não está aberto (embedded ativo).");
		}
	}

	public void fecharHudEAvancar() {
		limparTUPreview.run();
		Stage detailedTurnHudStage = detailedTurnHudStageSupplier.get();
		if (detailedTurnHudStage != null && detailedTurnHudStage.isShowing()) {
			detailedTurnHudStage.hide();
		}
		avancarParaProximoTurno();
	}

	public void avancarParaProximoTurno() {
		EstadoCombate estado = estadoSupplier.get();
		popularListasDeCombatentes.run();
		removerDestaques.run();

		Personagem atorQueAgiu = estado.getAtorAtual();
		if (atorQueAgiu != null && atorQueAgiu.getEfeitosAtivos().containsKey("Sussurro Sombrio")) {
			if (Math.random() < 0.25) {
				System.out.println(">>> SUSSURRO SOMBRIO ATIVADO! " + atorQueAgiu.getNome() + " age novamente!");

				int menorTuNaBatalha = estado.getCombatentes().stream()
						.filter(Personagem::isAtivoNoCombate)
						.mapToInt(Personagem::getContadorTU)
						.min()
						.orElse(0);

				atorQueAgiu.setContadorTU(menorTuNaBatalha - 1);
			} else {
				System.out.println(">>> Sussurro Sombrio (25%) falhou.");
			}
		}

		Personagem proximoCalculado = getProximoAtorCalculado.get();
		if (!verificarFimDeCombate.getAsBoolean() && estado.isCombateAtivo()) {
			System.out.println("\n...Ação Concluída...\nPróximo: "
					+ (proximoCalculado != null ? proximoCalculado.getNome() : "Ninguém")
					+ ". Aguardando 'Iniciar Turno'.");
			atualizarTimelineTU.run();
		}
	}
}
