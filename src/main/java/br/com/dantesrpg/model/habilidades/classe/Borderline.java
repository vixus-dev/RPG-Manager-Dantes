package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.Collections;
import java.util.List;

public class Borderline extends Habilidade {
	public Borderline() {
		super("Borderline", "Cria 3 Sombras. Ganha +30% Dano Crítico por sombra ativa.", TipoHabilidade.ATIVA, 3, 120,
				5, TipoAlvo.SI_MESMO, 0, 0, Collections.emptyList());
	}

	@Override
	public int getCustoMana() {
		return 3;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " usa Borderline! As sombras surgem...");

		if (manager != null) {
			// Spawna 3 clones
			for (int i = 0; i < 3; i++) {
				manager.solicitarSpawnClone(conjurador);
			}
		}

		// O bônus de Dano Crítico será calculado dinamicamente no Personagem.java
		conjurador.recalcularAtributosEstatisticas();
	}
}