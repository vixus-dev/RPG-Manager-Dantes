package br.com.dantesrpg.model.habilidades;

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

/** Versão ampliada da postura Auto-Guarda usada pelo Guardião do Recife. */
public class AutoGuarda2 extends Habilidade {
	public AutoGuarda2() {
		super("Auto-Guarda II", "Intercepta ataques básicos de aliados em até 5 casas e concede 25% de redução de dano.",
				TipoHabilidade.ATIVA, 0, 50, 1, TipoAlvo.SI_MESMO, 0, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		conjurador.adicionarEfeito(new Efeito("Guardião II", TipoEfeito.BUFF, 500,
				Map.of("REDUCAO_DANO_MODIFICADOR", 0.25, "ALCANCE_GUARDA", 5.0), 0, 0));
		conjurador.recalcularAtributosEstatisticas();
		System.out.println(">>> AUTO-GUARDA II: " + conjurador.getNome()
				+ " protege aliados em até 5 casas e recebe 25% de redução de dano.");
	}
}
