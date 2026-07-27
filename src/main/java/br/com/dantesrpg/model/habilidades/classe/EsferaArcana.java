package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;

import java.util.Collections;
import java.util.List;

public class EsferaArcana extends Habilidade {
	public EsferaArcana() {
		super("Esfera Arcana", "Causa 150% de dano em uma área circular 1x1.", TipoHabilidade.ATIVA,
				1, 90, 1, TipoAlvo.AREA_CIRCULAR, 1, 1.5, 1, Collections.emptyList());
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " lança uma Esfera Arcana.");
	}
}
