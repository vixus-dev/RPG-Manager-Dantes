package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;

import java.util.Collections;
import java.util.List;

public class MaestriaElemental extends Habilidade {
	public MaestriaElemental() {
		super("Maestria elemental", "Causa 4x de dano em uma área ao redor do usuário com alcance 3.",
				TipoHabilidade.ATIVA, 2, 220, 5, TipoAlvo.AREA, 3, 4.0, 1, Collections.emptyList());
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		System.out.println(">>> " + (conjurador == null ? "Personagem" : conjurador.getNome())
				+ " liberou a Maestria elemental.");
	}
}
