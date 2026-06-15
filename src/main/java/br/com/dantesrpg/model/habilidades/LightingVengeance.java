package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.util.EffectFactory;
import java.util.*;

public class LightingVengeance extends Habilidade {
	public LightingVengeance() {
		super("lighting vegeance", "Causa dano elétrico massivo a até 3 alvos selecionados, aplicando Choque ao contato.",
				TipoHabilidade.ATIVA, 2, 80, 1, TipoAlvo.MULTIPLOS, 0, 2.2, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 5;
	}

	@Override
	public int getNumeroDeAlvos() {
		return 3;
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	@Override
	public boolean afetaSiMesmo() {
		return false;
	}

	@Override
	public boolean afetaInimigos() {
		return true;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa " + getNome() + "!");
		if (alvos == null || alvos.isEmpty())
			return;

		for (Personagem alvo : alvos) {
			if (alvo == null || !alvo.isAtivoNoCombate())
				continue;
			Efeito choque = EffectFactory.criarEfeito("Choque", 1, 20);
			manager.getEffectProcessor().aplicarEfeito(alvo, choque);
			System.out.println(">>> CHOQUE! +20 TU em " + alvo.getNome());
			alvo.recalcularAtributosEstatisticas();
		}
	}
}
