package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.util.EffectFactory;
import java.util.*;

/**
 * Quebra-Elmo — Arma "Justiça & Esplendor" (Arcanjo - O Executor).
 *
 * Golpe individual com 0.125x. Custo: 80 TU, 1 de mana.
 * Aplica Hemorragia e Stun no alvo ao impacto.
 */
public class QuebraElmo extends Habilidade {

	public QuebraElmo() {
		super("Quebra-Elmo", "Um golpe preciso contra a cabeça do alvo que causa hemorragia e stun.",
				TipoHabilidade.ATIVA, 1, 80, 1,
				TipoAlvo.INDIVIDUAL, 0, 0.125, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 3;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " usa QUEBRA-ELMO!");

		Arma arma = conjurador.getArmaEquipada();
		int danoBase = (arma != null) ? arma.getDanoBase() : 10;

		for (Personagem alvo : alvos) {
			if (alvo == null || !alvo.isAtivoNoCombate())
				continue;

			Efeito hemorragia = EffectFactory.criarEfeito("Hemorragia", 500, danoBase);
			manager.aplicarEfeito(alvo, hemorragia);

			Efeito stun = new Efeito("STUN", TipoEfeito.DEBUFF, 100, null, 0, 0);
			manager.aplicarEfeito(alvo, stun);

			System.out.println(">>> " + alvo.getNome() + " sofre Hemorragia e Stun!");
		}
	}
}
