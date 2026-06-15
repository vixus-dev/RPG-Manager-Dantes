package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

/**
 * Arremesso Divino — Arma "Justiça & Esplendor" (Arcanjo - O Executor).
 *
 * AOE Circular 3x3, alcance 5, 0.25x. Custo: 50 TU, 1 de mana.
 */
public class ArremessoDivino extends Habilidade {

	public ArremessoDivino() {
		super("Arremesso Divino", "Arremessa a espada divina em área, explodindo em luz.",
				TipoHabilidade.ATIVA, 1, 50, 1,
				TipoAlvo.AREA_CIRCULAR, 3, 0.25, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 5;
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " arremessa a espada divina!");
	}
}
