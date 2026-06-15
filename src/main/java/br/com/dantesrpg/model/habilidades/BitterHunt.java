package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class BitterHunt extends Habilidade {
	public BitterHunt() {
		super("bitter Hunt", "AOE bola 5x5, 4x de dano",
				TipoHabilidade.ATIVA, 2, 120, 1, TipoAlvo.AREA_CIRCULAR, 5, 4.0, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 10;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa bitter Hunt!");
	}
}
