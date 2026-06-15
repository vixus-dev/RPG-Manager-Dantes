package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

public class Meteoro extends Habilidade {
	public Meteoro() {
		super("Meteoro", "Invoca um meteoro que devasta uma área 3x3. Dano: 2.25x, Alcance: 5.",
				TipoHabilidade.ATIVA, 2, 150, 1, TipoAlvo.AREA_QUADRADA, 3, 2.25, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 5;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " invoca um METEORO!");
	}
}
