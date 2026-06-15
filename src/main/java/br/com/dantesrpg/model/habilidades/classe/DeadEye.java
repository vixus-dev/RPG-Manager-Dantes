package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;

import java.util.Collections;
import java.util.List;

public class DeadEye extends Habilidade {

	public DeadEye() {
		super("DeadEye",
				"Disparo preciso com crítico garantido. A taxa crítica atual amplia o dano crítico deste tiro.",
				TipoHabilidade.ATIVA, 2, 120, 1, TipoAlvo.INDIVIDUAL, 1.0, 1,
				Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 8;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " mira com DeadEye.");
	}
}
