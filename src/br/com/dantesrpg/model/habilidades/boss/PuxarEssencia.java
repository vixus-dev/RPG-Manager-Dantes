package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class PuxarEssencia extends Habilidade {

	public PuxarEssencia() {
		super("Puxar Essencia", "Puxa uma essencia para perto de si mesmo.", TipoHabilidade.ATIVA, 1, 110, 1,
				TipoAlvo.SI_MESMO, 0, 0, 1, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> Zeraphon gira sua foice!");
	}
}