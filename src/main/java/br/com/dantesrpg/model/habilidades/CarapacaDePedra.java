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

/** Proteção pétrea da Enguia Rochosa. */
public class CarapacaDePedra extends Habilidade {
	private static final int DURACAO_TU = 300;

	public CarapacaDePedra() {
		super("Carapaça de Pedra", "Reveste um alvo, inclusive a si mesmo, reduzindo em 90% o dano sofrido por 300 TU.",
				TipoHabilidade.ATIVA, 0, 80, 1, TipoAlvo.INDIVIDUAL, 0, 0, 0, Collections.emptyList());
	}

	@Override public boolean afetaSiMesmo() { return true; }
	@Override public int getAlcanceMaximo() { return 4; }

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (alvos == null || alvos.isEmpty()) return;
		Personagem alvo = alvos.get(0);
		if (alvo == null || !alvo.isAtivoNoCombate()) return;
		alvo.adicionarEfeito(new Efeito("Carapaça de Pedra", TipoEfeito.BUFF, DURACAO_TU,
				Map.of("REDUCAO_DANO_MODIFICADOR", 0.90), 0, 0));
		alvo.recalcularAtributosEstatisticas();
		System.out.println(">>> CARAPAÇA DE PEDRA: " + alvo.getNome() + " recebe 90% de redução de dano por 300 TU.");
	}
}
