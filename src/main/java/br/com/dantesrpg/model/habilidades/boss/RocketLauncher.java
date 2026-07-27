package br.com.dantesrpg.model.habilidades.boss;

import java.util.Collections;
import java.util.List;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;

/** Ataque de foguete em bola circular 3x3. */
public class RocketLauncher extends Habilidade {

	public RocketLauncher() {
		super("ROCKET LAUNCHER", "Lança um foguete que causa 1,5x dano em uma área bola 3x3.",
				TipoHabilidade.ATIVA, 1, 115, 1, TipoAlvo.AREA_CIRCULAR, 3, 1.5, 1,
				Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 5;
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " dispara o ROCKET LAUNCHER.");
	}
}
