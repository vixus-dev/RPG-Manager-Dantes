package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

/**
 * Single-target. Causa 0.5x dano e arremessa alvos de peso médio em 3 tiles.
 * Força de empuxo = 9 (medio_padrao fatorResistencia 3.0 → 9/3 = 3 tiles).
 */
public class Arremesso extends Habilidade {

	public Arremesso() {
		super("Arremesso", "Chicoteia um alvo e o arremessa para trás (3 tiles em peso médio).",
				TipoHabilidade.ATIVA, 1, 110, 1, TipoAlvo.INDIVIDUAL, 0, 0.5, 1, Collections.emptyList());
		setForcaEmpuxo(9);
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		// Dano e knockback são aplicados automaticamente pelo DamageApplicator
		// via getMultiplicadorDeDano() e getForcaEmpuxo().
		System.out.println(">>> " + conjurador.getNome() + " usa Arremesso!");
	}
}
