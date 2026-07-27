package br.com.dantesrpg.model.habilidades.boss;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoHabilidade;

public class ShieldBash extends Habilidade {

	public ShieldBash() {
		super("Shield Bash", "Golpe em cone que causa 2x dano e quebra 30% da defesa do alvo.",
				TipoHabilidade.ATIVA, 0, 80, 1, TipoAlvo.CONE, 0, 2.0, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 2;
	}

	@Override
	public int getAnguloCone() {
		return 135;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		if (alvos == null) {
			return;
		}
		for (Personagem alvo : alvos) {
			if (alvo == null || !alvo.isAtivoNoCombate()) {
				continue;
			}
			Map<String, Double> modificadores = new HashMap<>();
			modificadores.put("REDUCAO_DANO_MODIFICADOR", -0.30);
			manager.aplicarEfeito(alvo,
					new Efeito("Armadura Quebrada", TipoEfeito.DEBUFF, 250, modificadores, 0, 0));
		}
	}
}
