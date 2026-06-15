package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.util.EffectFactory;
import java.util.*;

public class LightingThunder extends Habilidade {
	public LightingThunder() {
		super("lighting thunder", "Causa dano elétrico em área circular, aplicando Queimação ao contato.",
				TipoHabilidade.ATIVA, 2, 100, 1, TipoAlvo.AREA_CIRCULAR, 5, 1.2, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 10;
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

		int danoDaSource = (conjurador.getArmaEquipada() != null) ? conjurador.getArmaEquipada().getDanoBase() : 25;

		for (Personagem alvo : alvos) {
			if (alvo == null || !alvo.isAtivoNoCombate())
				continue;
			Efeito queimacao = EffectFactory.criarEfeito("Queimação", 0, danoDaSource);
			manager.getEffectProcessor().aplicarEfeito(alvo, queimacao);
			alvo.recalcularAtributosEstatisticas();
		}
	}
}
