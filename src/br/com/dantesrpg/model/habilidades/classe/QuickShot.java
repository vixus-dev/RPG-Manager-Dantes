package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class QuickShot extends Habilidade {
	public QuickShot() {
		super("Quick Shot", "Dispara 3 balas em 1-3 alvos.", TipoHabilidade.ATIVA, 1, 120, 1, TipoAlvo.MULTIPLOS, 0.5, // 50%
																														// de
																														// dano
																														// (total
																														// de
																														// 150%)
				1, Collections.emptyList());
	}

	@Override
	public int getNumeroDeAlvos() {
		return 3;
	}

	@Override
	public int getAlcanceMaximo() {
		return 5;
	}

	@Override
	public void executar(Personagem c, List<Personagem> a, EstadoCombate estado, CombatManager manager) {
		System.out.println(c.getNome() + " usa " + getNome() + "!");
		// O corpo fica vazio, pois o dano é tratado pelo CombatManager
	}
}