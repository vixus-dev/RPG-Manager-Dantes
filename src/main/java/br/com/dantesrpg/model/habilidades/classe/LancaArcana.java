package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;

import java.util.Collections;
import java.util.List;

public class LancaArcana extends Habilidade {
	public LancaArcana() {
		super("Lança Arcana", "Dispara 3 projéteis, causando 75% de dano em cada alvo selecionado.",
				TipoHabilidade.ATIVA, 2, 110, 1, TipoAlvo.MULTIPLOS, 0, 0.75, 1, Collections.emptyList());
	}

	@Override
	public int getNumeroDeAlvos() {
		return 3;
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " dispara 3 Lanças Arcanas.");
	}
}
