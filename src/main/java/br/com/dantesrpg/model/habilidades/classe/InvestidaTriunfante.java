package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class InvestidaTriunfante extends Habilidade {
	public InvestidaTriunfante() {
		super("Investida Triunfante", "Causa 175% de dano (125% a mais se em Stealth).", TipoHabilidade.ATIVA, 2, // Custo
																													// de
																													// Mana
				200, // Custo de TU
				5, // Nível 5
				TipoAlvo.INDIVIDUAL, 1.75, // Multiplicador base (175%)
				1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 2;
	}

	@Override
	public double getMultiplicadorModificado(Personagem ator, Personagem alvo, EstadoCombate estado) {
		if (ator.getEfeitosAtivos().containsKey("Stealth")) {
			System.out.println(">>> Investida Triunfante em Stealth! Dano multiplicado!");
			return 3.0; // Retorna o multiplicador de 300%
		}

		// Se não estava em Stealth, retorna o dano base (1.75)
		return super.getMultiplicadorModificado(ator, alvo, estado);
	}

	@Override
	public void executar(Personagem c, List<Personagem> a, EstadoCombate estado, CombatManager manager) {
	}
}