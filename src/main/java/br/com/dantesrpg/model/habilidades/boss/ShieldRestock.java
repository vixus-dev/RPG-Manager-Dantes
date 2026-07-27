package br.com.dantesrpg.model.habilidades.boss;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoHabilidade;

public class ShieldRestock extends Habilidade {

	public ShieldRestock() {
		super("shield Restock", "Restaura o efeito Shield no usuário.", TipoHabilidade.ATIVA,
				1, 120, 1, TipoAlvo.SI_MESMO, 0, 0, 0, Collections.emptyList());
	}

	@Override
	public boolean afetaInimigos() {
		return false;
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	@Override
	public boolean afetaSiMesmo() {
		return true;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		if (conjurador == null) {
			return;
		}
		manager.aplicarEfeito(conjurador,
				new Efeito("Shield", TipoEfeito.BUFF, 99_999, Map.of(), 0, 0));
	}
}
