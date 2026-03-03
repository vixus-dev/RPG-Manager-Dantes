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

public class PassoEscuro extends Habilidade {
	public PassoEscuro() {
		super("Passo Escuro", "Entra no modo stealth, se escondendo de ataques basicos.", TipoHabilidade.ATIVA, 2, 50,
				1, // Nível 1
				TipoAlvo.SI_MESMO, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa Passo Escuro e desaparece nas sombras...");

		// Cria o efeito
		Efeito stealth = new Efeito("Stealth", TipoEfeito.BUFF, 9999, // Duração "infinita" (será removido ao atacar/ser
																		// atingido)
				null, 0, 0);

		// Aplica o efeito no conjurador
		conjurador.adicionarEfeito(stealth);
		System.out.println(">>> Efeito [Stealth] aplicado.");

		// Recalcula stats (para a UI ver o novo efeito)
		conjurador.recalcularAtributosEstatisticas();
	}
}