package br.com.dantesrpg.model.habilidades.classe;

import java.util.Collections;
import java.util.List;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;

public class Ilusao extends Habilidade {

	public Ilusao() {
		super("Ilusao", "Cria um clone sombrio que imita os ataques do jogador.", // Descrição
				TipoHabilidade.ATIVA, 2, 150, 1, TipoAlvo.SI_MESMO, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> Darrell usou Ilusão! Spawnando clone...");
		if (manager != null) {
			manager.solicitarSpawnClone(conjurador);
		}
	}
}
