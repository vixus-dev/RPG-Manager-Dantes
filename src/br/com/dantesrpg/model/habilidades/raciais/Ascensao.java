package br.com.dantesrpg.model.habilidades.raciais;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.Collections;
import java.util.List;

public class Ascensao extends Habilidade {

	public Ascensao() {
		super("Ascensão", "Consome 5 de Benevolência para assumir a forma angelical.", TipoHabilidade.ATIVA, 0, 0, 1,
				TipoAlvo.SI_MESMO, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (conjurador.getRaca() instanceof br.com.dantesrpg.model.racas.HalfAngel) {
			br.com.dantesrpg.model.racas.HalfAngel anjo = (br.com.dantesrpg.model.racas.HalfAngel) conjurador.getRaca();

			if (anjo.getCurrentStacks() >= 5 && !anjo.isTransformed()) {
				anjo.ativarAscensaoManual(conjurador);
			} else {
				System.out.println(">>> Falha: Precisa de 5 cargas de Benevolência.");
			}
		}
	}
}