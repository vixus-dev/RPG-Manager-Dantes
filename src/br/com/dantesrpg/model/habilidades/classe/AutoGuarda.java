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

public class AutoGuarda extends Habilidade {
	public AutoGuarda() {
		super("Auto-Guarda", "Redireciona golpes leves de aliados próximos para si.", TipoHabilidade.ATIVA, 0, // Custo
																												// de
																												// Mana
																												// (Exemplo)
				50, // Custo de TU (Exemplo)
				3, // Nível 3
				TipoAlvo.SI_MESMO, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " ativa a " + getNome() + "!");

		// Cria o efeito "marcador"
		Efeito guardiao = new Efeito("Guardião", TipoEfeito.BUFF, 9999, null, 0, 0);

		// Aplica o efeito no conjurador
		conjurador.adicionarEfeito(guardiao);
		System.out.println(">>> Efeito [Guardião] aplicado.");

		conjurador.recalcularAtributosEstatisticas();
	}
}