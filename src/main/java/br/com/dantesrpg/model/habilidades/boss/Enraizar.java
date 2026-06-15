package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import br.com.dantesrpg.model.util.EffectFactory;
import java.util.Collections;
import java.util.List;

/**
 * AOE. Causa 1x dano e aplica Lento (100 TU) + Choque em cada alvo atingido.
 */
public class Enraizar extends Habilidade {

	public Enraizar() {
		super("Enraizar",
				"Raízes de sangue brotam do chão prendendo alvos em área (Lento 100TU + Choque).",
				TipoHabilidade.ATIVA, 2, 150, 1, TipoAlvo.AREA, 2, 1.0, 1, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (alvos == null || alvos.isEmpty())
			return;

		System.out.println(">>> " + conjurador.getNome() + " usa Enraizar!");

		for (Personagem alvo : alvos) {
			if (alvo == null || !alvo.isAtivoNoCombate())
				continue;

			Efeito lento = EffectFactory.criarEfeito("Lento", 100, 0);
			Efeito choque = EffectFactory.criarEfeito("Choque", 1, 20);

			manager.getEffectProcessor().aplicarEfeito(alvo, lento);
			manager.getEffectProcessor().aplicarEfeito(alvo, choque);

			System.out.println(">>> " + alvo.getNome() + " enraizado (Lento + Choque).");
		}
	}
}
