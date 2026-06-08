package br.com.dantesrpg.controller.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import br.com.dantesrpg.controller.DetailedTurnHUDController;
import br.com.dantesrpg.controller.MapController;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.ModoAtaque;
import javafx.stage.Stage;

public class SquadCombateCoordinator {

	private final Supplier<EstadoCombate> estadoSupplier;
	private final Supplier<CombatManager> combatManagerSupplier;
	private final Supplier<Stage> detailedTurnHudStageSupplier;
	private final Supplier<DetailedTurnHUDController> detailedTurnHudControllerSupplier;
	private final Consumer<Consumer<MapController>> forEachMap;
	private final Runnable avancarTurnoAposAcao;
	private final Consumer<Personagem> passarVez;
	private final Runnable fecharHudEAvancar;

	private Habilidade habilidadeSquadTemp;
	private int rolagemSquadTemp;
	private ModoAtaque modoAtaqueSquadTemp = ModoAtaque.NORMAL;
	private int tirosExtrasSquadTemp;
	private Map<Personagem, Personagem> ataquesSquadTemp;
	private List<Personagem> clonesSquadAtuais;

	public SquadCombateCoordinator(Supplier<EstadoCombate> estadoSupplier,
			Supplier<CombatManager> combatManagerSupplier, Supplier<Stage> detailedTurnHudStageSupplier,
			Supplier<DetailedTurnHUDController> detailedTurnHudControllerSupplier,
			Consumer<Consumer<MapController>> forEachMap, Runnable avancarTurnoAposAcao,
			Consumer<Personagem> passarVez, Runnable fecharHudEAvancar) {
		this.estadoSupplier = estadoSupplier;
		this.combatManagerSupplier = combatManagerSupplier;
		this.detailedTurnHudStageSupplier = detailedTurnHudStageSupplier;
		this.detailedTurnHudControllerSupplier = detailedTurnHudControllerSupplier;
		this.forEachMap = forEachMap;
		this.avancarTurnoAposAcao = avancarTurnoAposAcao;
		this.passarVez = passarVez;
		this.fecharHudEAvancar = fecharHudEAvancar;
	}

	public void definirClonesDoTurno(List<Personagem> clonesDoTurno) {
		this.clonesSquadAtuais = clonesDoTurno;
	}

	public void limparClonesDoTurno() {
		this.clonesSquadAtuais = null;
	}

	public void limparEstadoTemporario() {
		habilidadeSquadTemp = null;
		ataquesSquadTemp = null;
		rolagemSquadTemp = 0;
		modoAtaqueSquadTemp = ModoAtaque.NORMAL;
		tirosExtrasSquadTemp = 0;
		clonesSquadAtuais = null;
	}

	public void iniciarAtaqueSquad(Habilidade habilidadeAcao, Habilidade habilidadeSelecao, int rolagemGlobal,
			ModoAtaque modoAtaque, int tirosExtras) {
		this.habilidadeSquadTemp = habilidadeAcao;
		this.rolagemSquadTemp = rolagemGlobal;
		this.modoAtaqueSquadTemp = modoAtaque != null ? modoAtaque : ModoAtaque.NORMAL;
		this.tirosExtrasSquadTemp = Math.max(0, tirosExtras);

		Stage detailedTurnHudStage = detailedTurnHudStageSupplier.get();
		if (detailedTurnHudStage != null) {
			detailedTurnHudStage.hide();
		}

		if (clonesSquadAtuais != null) {
			forEachMap.accept(m -> m.iniciarSelecaoSquad(clonesSquadAtuais, habilidadeSelecao, rolagemGlobal));
		}
	}

	public void executarAcaoClonesSemAlvo(Habilidade habilidade, int rolagemGlobal) {
		if (clonesSquadAtuais == null) {
			clonesSquadAtuais = new ArrayList<>();
		}

		Stage detailedTurnHudStage = detailedTurnHudStageSupplier.get();
		if (detailedTurnHudStage != null) {
			detailedTurnHudStage.hide();
		}

		CombatManager combatManager = combatManagerSupplier.get();
		if (combatManager != null) {
			combatManager.executarAtaqueCoordenado(new HashMap<>(), habilidade, rolagemGlobal, ModoAtaque.NORMAL, 0,
					estadoSupplier.get(), clonesSquadAtuais);
		}

		limparEstadoTemporario();
		avancarTurnoAposAcao.run();
	}

	public void retornarDoSquadComAlvos(Map<Personagem, Personagem> ataquesDefinidos) {
		ataquesSquadTemp = ataquesDefinidos != null ? new HashMap<>(ataquesDefinidos) : new HashMap<>();

		if (ataquesSquadTemp.isEmpty()) {
			System.out.println("SQUAD: Nenhum alvo foi travado. Encerrando o turno dos clones.");
			passarTurnoSquadAtual();
			return;
		}

		System.out.println("SQUAD: Todos os alvos selecionados. Retornando à HUD.");

		DetailedTurnHUDController detailedTurnHudController = detailedTurnHudControllerSupplier.get();
		Stage detailedTurnHudStage = detailedTurnHudStageSupplier.get();
		if (detailedTurnHudController != null && detailedTurnHudStage != null) {
			detailedTurnHudController.adicionarAlvos(new ArrayList<>(ataquesSquadTemp.values()));
			detailedTurnHudController.configurarConfirmacaoSquad(ataquesSquadTemp.size());
			detailedTurnHudStage.show();
			detailedTurnHudStage.toFront();
		}
	}

	public void executarAtaqueSquadFinal() {
		if (ataquesSquadTemp == null) {
			ataquesSquadTemp = new HashMap<>();
		}
		if (clonesSquadAtuais == null) {
			clonesSquadAtuais = new ArrayList<>();
		}

		CombatManager combatManager = combatManagerSupplier.get();
		if (combatManager != null) {
			combatManager.executarAtaqueCoordenado(ataquesSquadTemp, habilidadeSquadTemp, rolagemSquadTemp,
					modoAtaqueSquadTemp, tirosExtrasSquadTemp, estadoSupplier.get(), clonesSquadAtuais);
		}

		limparEstadoTemporario();
		avancarTurnoAposAcao.run();
	}

	public void resolverAtaqueCoordenado(Map<Personagem, Personagem> ataques, Habilidade habilidade,
			int rolagemGlobal) {
		List<Personagem> envolvidos = new ArrayList<>(ataques.keySet());
		CombatManager combatManager = combatManagerSupplier.get();
		if (combatManager != null) {
			combatManager.executarAtaqueCoordenado(ataques, habilidade, rolagemGlobal, ModoAtaque.NORMAL, 0,
					estadoSupplier.get(), envolvidos);
		}

		avancarTurnoAposAcao.run();
	}

	public boolean devePassarSquadDeClones(Personagem ator) {
		if (ator == null || !ator.isClone() || clonesSquadAtuais == null || clonesSquadAtuais.isEmpty()) {
			return false;
		}
		Personagem criador = ator.getCriador();
		return criador != null && clonesSquadAtuais.stream()
				.anyMatch(clone -> clone != null && clone.isAtivoNoCombate() && clone.isClone()
						&& clone.getCriador() == criador);
	}

	public void passarTurnoSquadAtual() {
		EstadoCombate estado = estadoSupplier.get();
		if (clonesSquadAtuais == null || clonesSquadAtuais.isEmpty()) {
			passarVez.accept(estado != null ? estado.getAtorAtual() : null);
			limparEstadoTemporario();
			fecharHudEAvancar.run();
			return;
		}

		Personagem criador = null;
		Personagem atorAtual = estado != null ? estado.getAtorAtual() : null;
		if (atorAtual != null && atorAtual.isClone()) {
			criador = atorAtual.getCriador();
		}

		if (criador == null) {
			for (Personagem clone : clonesSquadAtuais) {
				if (clone != null && clone.isAtivoNoCombate() && clone.isClone()) {
					criador = clone.getCriador();
					break;
				}
			}
		}

		for (Personagem clone : new ArrayList<>(clonesSquadAtuais)) {
			if (clone == null || !clone.isAtivoNoCombate() || !clone.isClone()) {
				continue;
			}
			if (criador != null && clone.getCriador() != criador) {
				continue;
			}

			passarVez.accept(clone);
		}

		limparEstadoTemporario();
		fecharHudEAvancar.run();
	}
}
