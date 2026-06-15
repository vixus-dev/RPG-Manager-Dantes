package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class Serpent extends Habilidade {

	public Serpent() {
		super("Serpent", "Ataque rápido de bolinha 1x1.", TipoHabilidade.ATIVA, 
				1, // custo mana
				100, // custo TU
				1, // nivel
				TipoAlvo.AREA_CIRCULAR, 
				1, // tamanhoArea
				2.0, // multiplicadorDeDano
				1, // ticksDeDano
				Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 20;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " usa Serpent!");
	}
}
