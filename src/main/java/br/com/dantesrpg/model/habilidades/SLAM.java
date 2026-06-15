package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;

import java.util.*;

public class SLAM extends Habilidade {

	public SLAM() {
		super("SLAM", "Bate com as duas mãos no chão", TipoHabilidade.ATIVA, 5, 125, 1, TipoAlvo.AREA, 7, 2, 1,
				Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 10;
	}

	// --- LÓGICA DE EXECUÇÃO ---
	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa " + getNome() + "!");
	}
}