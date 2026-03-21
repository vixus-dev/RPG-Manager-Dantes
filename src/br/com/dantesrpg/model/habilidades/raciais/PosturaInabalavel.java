package br.com.dantesrpg.model.habilidades.raciais;

import java.util.Collections;
import java.util.List;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import br.com.dantesrpg.model.racas.Anao;

public class PosturaInabalavel extends Habilidade {

	public PosturaInabalavel() {
		super(Anao.EFEITO_POSTURA,
				"Alterna a postura defensiva do an\u00e3o. Enquanto ativa, reduz em 50% o dano antes da armadura, impede movimento, bloqueia movimento for\u00e7ado, aumenta o dano de armas com mais de 120 TU em 20% e faz ataques custarem +20 TU.",
				TipoHabilidade.ATIVA, 0, 0, 1, TipoAlvo.SI_MESMO, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (conjurador.getRaca() instanceof Anao) {
			((Anao) conjurador.getRaca()).togglePostura(conjurador);
		}
	}
}
