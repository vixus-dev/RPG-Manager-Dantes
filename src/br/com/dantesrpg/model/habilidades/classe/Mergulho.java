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

public class Mergulho extends Habilidade {
	public Mergulho() {
		super("Mergulho", "Entra no modo stealth, se escondendo de ataques básicos.", TipoHabilidade.ATIVA, 1, 50,
				1, TipoAlvo.SI_MESMO, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa Mergulho e desaparece sob a terra...");

		Efeito stealth = new Efeito("Stealth", TipoEfeito.BUFF, 9999, null, 0, 0);
		conjurador.adicionarEfeito(stealth);
		System.out.println(">>> Efeito [Stealth] aplicado.");

		conjurador.recalcularAtributosEstatisticas();
	}
}
