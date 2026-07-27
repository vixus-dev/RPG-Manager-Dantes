package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;

import java.util.Collections;
import java.util.List;

public class ToqueDeMana extends Habilidade {
	public ToqueDeMana() {
		super("Toque de Mana", "Recupera 5 de mana de todos os jogadores, inclusive o conjurador.",
				TipoHabilidade.ATIVA, 6, 50, 5, TipoAlvo.EQUIPE, 0, 0, 0, Collections.emptyList());
	}

	@Override
	public boolean afetaInimigos() {
		return false;
	}

	@Override
	public boolean afetaAliados() {
		return true;
	}

	@Override
	public boolean afetaSiMesmo() {
		return true;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		if (estado == null || estado.getCombatentes() == null) {
			if (conjurador != null) {
				conjurador.setManaAtual(conjurador.getManaAtual() + 5);
			}
			return;
		}

		for (Personagem jogador : estado.getCombatentes()) {
			if (jogador != null && jogador.isAtivoNoCombate()
					&& "JOGADOR".equalsIgnoreCase(jogador.getFaccao())) {
				jogador.setManaAtual(jogador.getManaAtual() + 5);
			}
		}
		System.out.println(">>> Toque de Mana: todos os jogadores recuperaram 5 de mana.");
	}
}
