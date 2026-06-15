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

public class MordidaQuente extends Habilidade {
	public MordidaQuente() {
		super("MordidaQuente", "Vo te moide", TipoHabilidade.ATIVA, 1, // Custo de Mana 3
				90, // Custo de TU 150
				1, // Nível 5
				TipoAlvo.INDIVIDUAL, 2.5, // 25% de dano
				1, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " Moidida!");

		if (alvos == null || alvos.isEmpty())
			return;
		Personagem alvoPrincipal = alvos.get(0);
		int danoDaSource = 10; // Dano base da mordida para cálculo do DoT
		Efeito queimacao = br.com.dantesrpg.model.util.EffectFactory.criarEfeito("Queimação", 0, danoDaSource);

		alvoPrincipal.adicionarEfeito(queimacao);
		System.out.println(">>> " + getNome() + " aplica [Queimação] em " + alvoPrincipal.getNome() + ".");

		// É crucial recalcular os stats para a UI (e lógicas) verem o novo efeito
		alvoPrincipal.recalcularAtributosEstatisticas();
	}
}