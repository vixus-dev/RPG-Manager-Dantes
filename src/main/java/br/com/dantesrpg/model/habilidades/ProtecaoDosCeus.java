package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class ProtecaoDosCeus extends Habilidade {

	public ProtecaoDosCeus() {
		super("Proteção dos Céus",
			  "Habilidade base de Proteção dos Céus (YAWEH).",
			  TipoHabilidade.ATIVA,
			  2, // Custo Mana
			  100, // Custo TU
			  1, // Nível Necessário
			  TipoAlvo.INDIVIDUAL,
			  0.0, // Multiplicador de dano
			  0, // Ticks de dano
			  Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " usa Proteção dos Céus (Ainda não implementado)");
	}
}
