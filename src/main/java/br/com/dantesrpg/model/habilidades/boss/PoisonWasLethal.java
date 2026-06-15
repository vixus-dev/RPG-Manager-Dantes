package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.util.EffectFactory;
import java.util.*;

/**
 * Poison Was Lethal — Causa 1.2x dano e aplica Toxina por 200 TU.
 * Custo: 1 mana, 100 TU.
 */
public class PoisonWasLethal extends Habilidade {

	public PoisonWasLethal() {
		super("Poison Was Lethal",
				"Causa 1.2x dano e aplica Toxina (200 TU).",
				TipoHabilidade.ATIVA, 1, 100, 1,
				TipoAlvo.INDIVIDUAL, 0, 1.2, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {

		if (alvos == null || alvos.isEmpty())
			return;

		Personagem alvo = alvos.get(0);
		if (alvo == null || !alvo.isAtivoNoCombate())
			return;

		// Dano já é tratado por resolverDanoPadrao() via janela de resolução.
		// Aqui só aplica o efeito secundário (Toxina).

		Arma arma = conjurador.getArmaEquipada();
		double danoBase = (arma != null) ? arma.getDanoBase() : 10;

		Efeito toxina = EffectFactory.criarEfeito("Toxina", 200, (int) danoBase);
		if (toxina != null) {
			manager.getEffectProcessor().aplicarEfeito(alvo, toxina);
			System.out.println(">>> " + alvo.getNome() + " foi envenenado!");
		}

		if (manager.getMainController() != null)
			manager.getMainController().atualizarInterfaceTotal();
	}
}
