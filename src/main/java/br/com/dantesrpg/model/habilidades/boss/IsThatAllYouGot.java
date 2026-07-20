package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

public class IsThatAllYouGot extends Habilidade {
	public IsThatAllYouGot() {
		super("IS THAT ALL YOU GOT?", "Atinge um alvo 15 vezes, com 0,1x de dano por impacto.",
				TipoHabilidade.ATIVA, 0, 130, 1, TipoAlvo.INDIVIDUAL, 0, 0.1, 15, Collections.emptyList());
	}

	@Override public int getAlcanceMaximo() { return 2; }
	@Override public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " usa IS THAT ALL YOU GOT?");
	}
}
