package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoHabilidade;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ArquiMagia extends Habilidade {
	public ArquiMagia() {
		super("Arqui-magia",
				"Durante 500 TU, as magias não gastam mana e as habilidades custam 20% menos TU.",
				TipoHabilidade.ATIVA, 10, 0, 8, TipoAlvo.SI_MESMO, 0, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		if (conjurador == null || manager == null) {
			return;
		}

		Efeito arquiMagia = new Efeito("Arqui-magia", TipoEfeito.BUFF, 500,
				Map.of("REDUCAO_TU_HABILIDADES", 0.20), 0, 0);
		manager.getEffectProcessor().aplicarEfeito(conjurador, arquiMagia);
		System.out.println(">>> " + conjurador.getNome() + " ativou Arqui-magia por 500 TU.");
	}
}
