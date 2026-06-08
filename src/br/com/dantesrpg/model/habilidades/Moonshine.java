package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class Moonshine extends Habilidade {
	public Moonshine() {
		super("moonshine", "ataca a arena toda, 5 ticks de dano, cada tick causa 1x de dano",
				TipoHabilidade.ATIVA, 5, 100, 1, TipoAlvo.AREA, 99, 1.0, 5, Collections.emptyList());
	}

	@Override
	public boolean afetaAliados() {
		return true;
	}

	@Override
	public boolean afetaSiMesmo() {
		return false;
	}

	@Override
	public boolean afetaInimigos() {
		return true;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa moonshine!");
	}
}
