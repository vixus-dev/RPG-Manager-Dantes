package br.com.dantesrpg.model.habilidades.raciais;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.Collections;
import java.util.List;

public class DevilTrigger extends Habilidade {

	public DevilTrigger() {
		super("Devil Trigger", "Consome 5 acúmulos para liberar a forma demoníaca.", TipoHabilidade.ATIVA, 0, 0, 1,
				TipoAlvo.SI_MESMO, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (conjurador.getRaca() instanceof br.com.dantesrpg.model.racas.HalfDemon) {
			br.com.dantesrpg.model.racas.HalfDemon demon = (br.com.dantesrpg.model.racas.HalfDemon) conjurador
					.getRaca();

			if (demon.getCurrentStacks() >= 5 && !demon.isTransformed()) {
				demon.ativarDevilTrigger(conjurador);
			} else {
				System.out.println(">>> Falha: Precisa de 5 acúmulos ou já está transformado.");
			}
		}
	}
}