package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

/**
 * Estocada Divina — Arma "Justiça & Esplendor" (Arcanjo - O Executor).
 *
 * AOE Linha, alcance 5, 0.25x. Custo: 80 TU, -1 de mana (o Arcanjo GANHA 1 de mana ao usar).
 */
public class EstocadaDivina extends Habilidade {

	public EstocadaDivina() {
		super("Estocada Divina", "Uma estocada em linha reta banhada em luz divina.",
				TipoHabilidade.ATIVA, -1, 80, 1,
				TipoAlvo.LINHA, 1, 0.25, 1, Collections.emptyList());
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
		System.out.println(">>> " + conjurador.getNome() + " perfura com ESTOCADA DIVINA!");
	}
}
