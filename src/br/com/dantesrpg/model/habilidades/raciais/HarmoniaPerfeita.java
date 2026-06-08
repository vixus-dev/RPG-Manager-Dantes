package br.com.dantesrpg.model.habilidades.raciais;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.Collections;
import java.util.List;

public class HarmoniaPerfeita extends Habilidade {

	public HarmoniaPerfeita() {
		super("Harmonia Perfeita", "Consome 5 de Harmonia para ativar Τέλεια αρμονία: ataques 360° + cura + voo + armadura.",
				TipoHabilidade.ATIVA, 0, 0, 1, TipoAlvo.SI_MESMO, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (conjurador.getRaca() instanceof br.com.dantesrpg.model.racas.AnjoCaido) {
			br.com.dantesrpg.model.racas.AnjoCaido anjo = (br.com.dantesrpg.model.racas.AnjoCaido) conjurador.getRaca();

			if (anjo.getCurrentStacks() >= 5 && !anjo.isTransformed()) {
				anjo.ativarHarmoniaPerfeita(conjurador);
			} else {
				System.out.println(">>> Falha: Precisa de 5 cargas de Harmonia ou já está transformado.");
			}
		}
	}
}
