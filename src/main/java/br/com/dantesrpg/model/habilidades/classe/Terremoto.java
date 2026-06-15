package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

public class Terremoto extends Habilidade {
	public Terremoto() {
		super("Terremoto", "Abala o chão ao redor. Dano: 1.5x + 50TU nos inimigos. AOE 4, sem fogo amigo.",
				TipoHabilidade.ATIVA, 3, 125, 1, TipoAlvo.AREA, 4, 1.5, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 4;
	}

	@Override
	public boolean afetaAliados() {
		return false; // Sem fogo amigo
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " causa um TERREMOTO!");

		for (Personagem alvo : alvos) {
			if (!alvo.isAtivoNoCombate())
				continue;
			// Não acerta aliados
			if (alvo.getFaccao().equals(conjurador.getFaccao()))
				continue;

			// Aumenta o TU do inimigo em 50
			alvo.setContadorTU(alvo.getContadorTU() + 50);
			System.out.println(">>> " + alvo.getNome() + " foi abalado pelo Terremoto! +50 TU.");
		}
	}
}
