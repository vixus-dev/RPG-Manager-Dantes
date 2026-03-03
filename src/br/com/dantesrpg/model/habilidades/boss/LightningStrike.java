package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class LightningStrike extends Habilidade {

	public LightningStrike() {
		super("Lightning Strike", "Um corte na velocidade da luz que atordoa inimigos em cone.", TipoHabilidade.ATIVA,
				4, 100, 1, TipoAlvo.CONE, 0, 1.0, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 4;
	}

	@Override
	public int getAnguloCone() {
		return 60;
	} // Cone focado

	@Override
	public boolean afetaAliados() {
		return false; // Padrão: Habilidades de AoE causam fogo amigo
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> Virgilio se move como um relâmpago!");

		Efeito stun = new Efeito("Stun", TipoEfeito.DEBUFF, 100, null, 0, 0);

		for (Personagem alvo : alvos) {
			// Aplica Stun
			manager.aplicarEfeito(alvo, stun);
		}
	}
}