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

public class Combo extends Habilidade {

	public Combo() {
		super("Combo!", "Durante 300 TU, todos os socos atacam uma segunda vez.", TipoHabilidade.ATIVA, 2, // Custo de Mana
				80, // Custo de TU
				1, // Nível Necessário
				TipoAlvo.SI_MESMO, 0, // Nenhum dano direto
				0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " ativa o " + getNome() + "!");

		// Cria o efeito
		Efeito efeitoCombo = new Efeito("Combo!", TipoEfeito.BUFF, 300, // Duração
				null, // Sem modificadores
				0, 0);

		// Aplica o efeito no próprio conjurador
		conjurador.adicionarEfeito(efeitoCombo);
		System.out.println(">>> Efeito [Combo!] aplicado em " + conjurador.getNome() + ".");

		conjurador.recalcularAtributosEstatisticas();
	}
}