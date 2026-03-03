package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.util.EffectFactory;

import java.util.*;

public class DescargaEletrica extends Habilidade {

	public DescargaEletrica() {
		super("Descarga eletrica", "Um golpe devastador,rodando as correntes e eletrocutando os alvos.",
				TipoHabilidade.ATIVA, 1, 115, 1, TipoAlvo.CONE, 3, 1.5, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 3;
	}

	@Override
	public int getAnguloCone() {
		return 360;
	}

	@Override
	public boolean afetaAliados() {
		return false; // Padrão: Habilidades de AoE causam fogo amigo
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		for (Personagem alvo : alvos) {
			if (!alvo.isAtivoNoCombate())
				continue;
			Efeito stun = EffectFactory.criarEfeito("Stun", 100, 0);
			alvo.adicionarEfeito(stun);
			alvo.recalcularAtributosEstatisticas();
		}
	}
}
