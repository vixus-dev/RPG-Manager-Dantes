package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class LaminasDaJustiça extends Habilidade {

	public LaminasDaJustiça() {
		super("Barragem de Espadas", "Metralhadora de Jericó TURURURURURURU", TipoHabilidade.ATIVA, 2, 80, 1,
				TipoAlvo.LINHA, 1, 0.5, 6, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 4;
	}

	// --- LÓGICA DE EXECUÇÃO ---
	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa " + getNome() + "!");
	}
}