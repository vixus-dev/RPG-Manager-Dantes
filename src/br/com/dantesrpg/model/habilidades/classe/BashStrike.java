package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class BashStrike extends Habilidade {
	public BashStrike() {
		super("Bash Strike", "Golpe brutal que causa 135% de dano e devolve 15% ao usuário.", TipoHabilidade.ATIVA, 1,
				90, 1, TipoAlvo.INDIVIDUAL, 1.35, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 3;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " desfere um Bash Strike!");
	}

	/**
	 * Chamado pelo CombatManager após resolver o dano total causado.
	 * Aplica 15% do dano causado de volta ao conjurador.
	 */
	public static void aplicarRetornoDeDano(Personagem ator, double danoCausado, EstadoCombate estado,
			CombatManager manager) {
		double danoRetorno = danoCausado * 0.15;
		if (danoRetorno >= 1.0) {
			System.out.println(">>> Bash Strike: " + ator.getNome() + " sofre " + String.format("%.0f", danoRetorno)
					+ " de dano de retorno!");
			manager.aplicarDanoAoAlvoResolvido(null, ator, danoRetorno, true, TipoAcao.OUTRO, estado);
		}
	}
}
