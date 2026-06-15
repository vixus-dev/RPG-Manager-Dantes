package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito; // Import necessário
import br.com.dantesrpg.model.EstadoCombate; // Import necessário
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito; // Import necessário
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

public class RestricaoCelestial extends Habilidade {
	public RestricaoCelestial() {
		super("Restrição Celestial", "Converte todos os golpes fisicos em Fulgor negro.", TipoHabilidade.ATIVA, 5, 50,
				8, // Nível 8
				TipoAlvo.SI_MESMO, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " ativa a Restrição Celestial!");

		// Cria o efeito
		Efeito restricao = new Efeito("Restrição Celestial", TipoEfeito.BUFF, 200, // Duração
				null, 0, 0);

		// Aplica o efeito no conjurador
		conjurador.adicionarEfeito(restricao);
		System.out.println(">>> Efeito [Restrição Celestial] aplicado por 200 TU.");

		// Recalcula stats (para a UI ver o novo efeito)
		conjurador.recalcularAtributosEstatisticas();
	}
}