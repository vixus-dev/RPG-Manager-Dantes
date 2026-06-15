package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class ConviviaUmbrarum extends Habilidade {
	public ConviviaUmbrarum() {
		super("Convivia Umbrarum", "Ataque final de Dominus Albus.", TipoHabilidade.ATIVA, 3, 100, 1,
				TipoAlvo.INDIVIDUAL, 0, 1.25, 3, null);
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (alvos.isEmpty())
			return;
		Personagem vitima = alvos.get(0);
		Personagem mestre = conjurador.getMestreInvocador();

		double vidaAntes = vitima.getVidaAtual();
		// Resolve os 3 ticks de 1.25x
		// (A lógica de dano será chamada pelo CombatManager, aqui tratamos o pós-golpe)

		// Simulação do cenário após os hits:
		if (!vitima.isAtivoNoCombate()) {
			// CENÁRIO I: MATOU
			System.out.println(">>> CONVIVIA UMBRARUM: Sacrifício aceito!");
			if (mestre != null) {
				double cura = conjurador.getVidaAtual() * 0.20;
				mestre.setVidaAtual(mestre.getVidaAtual() + cura, estado, manager.getMainController());
				mestre.getPropriedades().remove("LOCK_SUMMON");
			}
			// Dominus morre
			manager.aplicarDanoAoAlvo(conjurador, conjurador, 9999, true, TipoAcao.OUTRO, estado);
		} else {
			// CENÁRIO II: NÃO MATOU
			System.out.println(">>> CONVIVIA UMBRARUM: Falha na caçada. Maldição aplicada.");
			Efeito maldição = new Efeito("Maldição Umbraum", TipoEfeito.DOT, 999, null,
					(int) (conjurador.getVidaMaxima() * 0.20), 50);
			conjurador.adicionarEfeito(maldição);
		}
	}
}