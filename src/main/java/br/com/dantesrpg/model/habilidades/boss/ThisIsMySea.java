package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

public class ThisIsMySea extends Habilidade {
	public ThisIsMySea() {
		super("THIS IS MY SEA.", "Ataque em área com 10 impactos de 0,1x de dano.",
				TipoHabilidade.ATIVA, 0, 100, 1, TipoAlvo.AREA_CIRCULAR, 7, 0.1, 10, Collections.emptyList());
	}

	@Override public int getAlcanceMaximo() { return 7; }
	@Override public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " declara: THIS IS MY SEA.");
	}
}
