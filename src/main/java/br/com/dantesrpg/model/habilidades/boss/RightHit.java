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

/** Golpe individual que quebra a armadura e arremessa o alvo. */
public class RightHit extends Habilidade {
	public RightHit() {
		super("Right HIT", "Golpe de 1,5x que empurra o alvo e quebra 30% da defesa.",
				TipoHabilidade.ATIVA, 0, 90, 1, TipoAlvo.INDIVIDUAL, 0, 1.5, 1, Collections.emptyList());
		setForcaEmpuxo(9);
	}

	@Override public int getAlcanceMaximo() { return 2; }

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		for (Personagem alvo : alvos) {
			if (alvo != null && alvo.isAtivoNoCombate()) {
				Efeito armaduraQuebrada = EffectFactory.criarEfeito("Armadura Quebrada (-30%)", 200, 0);
				manager.aplicarEfeito(alvo, armaduraQuebrada);
			}
		}
	}
}
