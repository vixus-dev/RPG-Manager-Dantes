package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class DestruidorDeGuardioes extends Habilidade {
	public DestruidorDeGuardioes() {
		super("Destruidor de Guardiões", "Dano massivo contra alvos com efeito Guardião.", TipoHabilidade.ATIVA, 2, 175,
				3, // Nível 3
				TipoAlvo.INDIVIDUAL, 1.0, 1, Collections.emptyList());
	}

	@Override
	public double getMultiplicadorModificado(Personagem ator, Personagem alvo, EstadoCombate estado) {
		if (alvo != null && alvo.getEfeitosAtivos().containsKey("Guardião")) {
			System.out.println(">>> Destruidor de Guardiões acerta um Guardião! Dano 300%!");
			return 3.0; // Retorna o multiplicador de 300%
		}

		// Se o alvo não for um Guardião, retorna o dano base (1.0)
		return super.getMultiplicadorModificado(ator, alvo, estado);
	}

	@Override
	public void executar(Personagem c, List<Personagem> a, EstadoCombate estado, CombatManager manager) {
		System.out.println(c.getNome() + " prepara um golpe contra guardiões!");
	}
}