package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

/** Rajada em linha larga, composta por dez impactos leves. */
public class ManiacSteam extends Habilidade {
	public ManiacSteam() {
		super("Maniac STEAM", "Rajada em linha de largura 3 e comprimento 10, com 10 impactos de 0,1x.",
				TipoHabilidade.ATIVA, 0, 114, 1, TipoAlvo.LINHA, 3, 0.1, 10, Collections.emptyList());
	}

	@Override public int getAlcanceMaximo() { return 10; }
	@Override public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " libera Maniac STEAM!");
	}
}
