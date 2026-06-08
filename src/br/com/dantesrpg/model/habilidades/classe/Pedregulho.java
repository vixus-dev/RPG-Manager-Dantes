package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

public class Pedregulho extends Habilidade {
	public Pedregulho() {
		super("Pedregulho", "Arremessa um pedregulho em um inimigo. Dano: 1.5x, Alcance: 5.", TipoHabilidade.ATIVA,
				2, 110, 1, TipoAlvo.INDIVIDUAL, 0, 1.5, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 5;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " arremessa um Pedregulho!");
	}
}
