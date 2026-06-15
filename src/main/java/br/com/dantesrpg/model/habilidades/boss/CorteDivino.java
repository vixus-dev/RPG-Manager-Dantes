package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

/**
 * Corte Divino — Arma "Justiça & Esplendor" (Arcanjo - O Executor).
 *
 * AOE Cone 135°, alcance 3, 0.25x. Custo: 80 TU, -1 de mana (o Arcanjo GANHA 1 de mana ao usar).
 */
public class CorteDivino extends Habilidade {

	public CorteDivino() {
		super("Corte Divino", "Um corte amplo em cone, banhado em esplendor celeste.",
				TipoHabilidade.ATIVA, -1, 80, 1,
				TipoAlvo.CONE, 0, 0.25, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 3;
	}

	@Override
	public int getAnguloCone() {
		return 135;
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " desfere um CORTE DIVINO!");
	}
}
