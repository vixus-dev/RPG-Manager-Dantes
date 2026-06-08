package br.com.dantesrpg.controller.map;

import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;

import java.util.*;

/**
 * Máquina de estados para o modo Squad (clones declarando alvos em sequência).
 * Extraído de MapController para isolar o fluxo de seleção múltipla de alvos.
 * Chama de volta ao MapController via métodos públicos para renderização e cálculo.
 */
public class SquadModeHandler {

	private final br.com.dantesrpg.controller.MapController mapController;
	private final AoEShapeCalculator aoeCalc;
	private final ToggleButton toggleMover;
	private final ToggleButton toggleMirar;
	private final Set<Pane> celulasAlcanceMovimento;

	// Estado próprio (movido de MapController)
	private final Queue<Personagem> filaClonesSquad = new LinkedList<>();
	private final Map<Personagem, Personagem> ataquesDeclaradosSquad = new HashMap<>();
	private boolean modoSquad = false;
	private int rolagemSquadGlobal;

	public SquadModeHandler(br.com.dantesrpg.controller.MapController mapController,
			AoEShapeCalculator aoeCalc,
			ToggleButton toggleMover, ToggleButton toggleMirar,
			Set<Pane> celulasAlcanceMovimento) {
		this.mapController = mapController;
		this.aoeCalc = aoeCalc;
		this.toggleMover = toggleMover;
		this.toggleMirar = toggleMirar;
		this.celulasAlcanceMovimento = celulasAlcanceMovimento;
	}

	// ========== API PÚBLICA ==========

	public boolean isModoSquad() {
		return modoSquad;
	}

	public void iniciarSelecaoSquad(List<Personagem> clones, Habilidade habilidade, int rolagem) {
		this.modoSquad = true;
		this.rolagemSquadGlobal = rolagem;
		this.filaClonesSquad.clear();
		this.ataquesDeclaradosSquad.clear();
		this.filaClonesSquad.addAll(clones);

		mapController.setModoSelecaoAlvo(true);
		mapController.setHabilidadeAtual(habilidade);
		mapController.atualizarBotaoPularSquad();
		prepararProximoCloneSquad();
	}

	public void tratarCliqueSquad(Pane cell, int x, int y) {
		Personagem atorAtual = mapController.getAtorAtual();
		if (atorAtual == null) return;

		boolean isMover = (toggleMover != null) ? toggleMover.isSelected() : false;
		boolean isMirar = (toggleMirar != null) ? toggleMirar.isSelected() : true;

		if (isMover && atorAtual.getRaca() != null && !atorAtual.getRaca().podeSeMover(atorAtual)) {
			System.out.println("SQUAD: " + atorAtual.getNome() + " nao pode se mover enquanto estiver em postura.");
			return;
		}

		if (isMover) {
			if (celulasAlcanceMovimento.contains(cell)) {
				if (mapController.getPersonagemNaCelula(x, y) == null) {
					int dist = aoeCalc.calcularDistancia(atorAtual.getPosX(), atorAtual.getPosY(), x, y);
					if (dist != -1 && dist <= atorAtual.getMovimentoRestanteTurno()) {
						atorAtual.setPosX(x);
						atorAtual.setPosY(y);
						atorAtual.setMovimentoRestanteTurno(atorAtual.getMovimentoRestanteTurno() - dist);
						mapController.desenharPeoes(mapController.getCombatentes());
						mapController.calcularEExibirMovimento(atorAtual);
						return;
					}
				}
			}
		}

		if (isMirar) {
			if (celulasAlcanceMovimento.contains(cell)) {
				Personagem alvo = mapController.getPersonagemNaCelula(x, y);
				if (alvo != null && !alvo.equals(atorAtual)) {
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

	public void pularSquad() {
		if (!modoSquad) return;
		System.out.println("SQUAD: Seleção encerrada manualmente. Clones restantes serão pulados.");
		finalizarSquad();
	}

	// ========== PRIVADOS ==========

	private void prepararProximoCloneSquad() {
		while (!filaClonesSquad.isEmpty()) {
			Personagem proximo = filaClonesSquad.peek();
			if (proximo != null && proximo.isAtivoNoCombate()) break;
			filaClonesSquad.poll();
		}

		if (filaClonesSquad.isEmpty()) {
			finalizarSquad();
			return;
		}

		Personagem proximo = filaClonesSquad.peek();
		mapController.setAtorAtual(proximo);
		System.out.println("MAPA (SQUAD): Vez de " + proximo.getNome());
		mapController.atualizarBotaoPularSquad();
		prepararMiraParaCloneAtual();
	}

	private void prepararMiraParaCloneAtual() {
		Personagem atorAtual = mapController.getAtorAtual();
		if (atorAtual == null) return;

		mapController.setModoSelecaoAlvo(true);
		if (toggleMover != null) toggleMover.setSelected(false);
		if (toggleMirar != null) toggleMirar.setSelected(true);

		mapController.limparCanvas();
		mapController.limparDestaquesAlcance();
		mapController.calcularEExibirAtaqueRange(atorAtual, mapController.getHabilidadeAtual());
		mapController.setCombatCursor(javafx.scene.Cursor.CROSSHAIR);
	}

	private void finalizarSquad() {
		Map<Personagem, Personagem> ataquesFinalizados = new HashMap<>(ataquesDeclaradosSquad);
		this.modoSquad = false;
		this.filaClonesSquad.clear();
		mapController.atualizarBotaoPularSquad();
		mapController.sairModoSelecao();
		mapController.getMainController().retornarDoSquadComAlvos(ataquesFinalizados);
	}
}
