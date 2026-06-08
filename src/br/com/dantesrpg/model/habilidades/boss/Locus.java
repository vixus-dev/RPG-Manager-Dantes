package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

/**
 * Locus — Habilidade de 4 escolhas do Justiceiro Cego.
 * Custo: 3 mana, 80 TU.
 *
 * 1. "Flagelo"     — Causa 400 de dano a si mesmo
 * 2. "Raízes"      — 0.1x dano em 2 alvos e enraiza (0 movimento por 1 turno)
 * 3. "Epicentro"   — AOE circular, empurra oponentes para o centro
 * 4. (reservado)
 */
public class Locus extends Habilidade {

	public Locus() {
		super("Locus", "Escolha entre 3 efeitos de julgamento.",
				TipoHabilidade.ATIVA, 3, 80, 1,
				TipoAlvo.AREA_CIRCULAR, 5, 0, 0, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public List<String> getOpcoesSelection() {
		return Arrays.asList("Flagelo", "Raízes", "Epicentro");
	}

	@Override
	public int getNumeroDeAlvos() {
		return 2; // Para a opção "Raízes"
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	public void executarComEscolha(Personagem conjurador, List<Personagem> alvos,
			int alvoX, int alvoY, String escolha, EstadoCombate estado, CombatManager manager) {

		System.out.println(">>> " + conjurador.getNome() + " usa LOCUS: " + escolha + "!");

		switch (escolha) {

		case "Flagelo":
			// Causa 400 de dano a si mesmo
			double danoSelf = 400;
			conjurador.setVidaAtual(Math.max(0, conjurador.getVidaAtual() - danoSelf));
			conjurador.recalcularAtributosEstatisticas();
			System.out.println(">>> FLAGELO: " + conjurador.getNome()
					+ " se puniu com " + (int) danoSelf + " de dano!");
			break;

		case "Raízes":
			// 0.1x dano em até 2 alvos + enraiza (movimento 0)
			if (alvos == null || alvos.isEmpty())
				return;

			Arma arma = conjurador.getArmaEquipada();
			double danoBase = (arma != null) ? arma.getDanoBase() : 10;
			double danoRaizes = danoBase * 0.1;

			int count = 0;
			for (Personagem alvo : alvos) {
				if (count >= 2)
					break;
				if (alvo == null || !alvo.isAtivoNoCombate() || alvo == conjurador)
					continue;

				manager.getDamageApplicator().aplicarDanoAoAlvoResolvido(
						conjurador, alvo, danoRaizes, false,
						TipoAcao.HABILIDADE, estado, 0);

				// Enraiza: movimento 0 por ~1 turno (150 TU)
				Map<String, Double> modsRaiz = new HashMap<>();
				modsRaiz.put("MOVIMENTO", -99.0);
				Efeito raiz = new Efeito("Enraizado", TipoEfeito.DEBUFF, 150, modsRaiz, 0, 0);
				alvo.adicionarEfeito(raiz);
				alvo.recalcularAtributosEstatisticas();

				System.out.println(">>> RAÍZES: " + alvo.getNome()
						+ " foi enraizado e não pode se mover!");
				count++;
			}
			break;

		case "Epicentro":
			// AOE circular: empurra todos os oponentes para o epicentro
			if (alvos == null || alvos.isEmpty())
				return;

			for (Personagem alvo : alvos) {
				if (alvo == null || !alvo.isAtivoNoCombate())
					continue;
				if (alvo.getFaccao() != null && alvo.getFaccao().equals(conjurador.getFaccao()))
					continue;

				// Move diretamente para o epicentro (ou o mais perto possível)
				alvo.setPosX(alvoX);
				alvo.setPosY(alvoY);

				System.out.println(">>> EPICENTRO: " + alvo.getNome()
						+ " foi puxado para (" + alvoX + "," + alvoY + ")!");
			}

			// Refresh completo da UI para reposicionar tokens corretamente
			if (manager.getMainController() != null) {
				manager.getMainController().atualizarInterfaceTotal();
			}
			break;
		}

		if (manager.getMainController() != null)
			manager.getMainController().atualizarInterfaceTotal();
	}

	@Override
	public void executar(Personagem conjurador, int alvoX, int alvoY, List<Personagem> alvos,
			EstadoCombate estado, CombatManager manager) {
		AcaoMestreInput lastInput = manager.getLastInput();
		String escolha = (lastInput != null) ? lastInput.getOpcaoEscolhida() : null;
		if (escolha == null || escolha.isEmpty())
			escolha = "Flagelo";
		executarComEscolha(conjurador, alvos, alvoX, alvoY, escolha, estado, manager);
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		executar(conjurador, 0, 0, alvos, estado, manager);
	}
}
