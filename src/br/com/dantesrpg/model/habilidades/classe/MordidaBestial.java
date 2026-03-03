package br.com.dantesrpg.model.habilidades.classe;

import java.util.Collections;
import java.util.List;

import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import br.com.dantesrpg.model.util.EffectFactory;

public class MordidaBestial extends Habilidade {

	public MordidaBestial() {
		super("Garras da Morte", "Um ataque rápido e mortal que causa dano massivo baseado", TipoHabilidade.ATIVA, 2, // Custo
																														// de
																														// Mana
				100, // Custo de TU
				1, // Nível Necessário
				TipoAlvo.INDIVIDUAL, 1.75, // Multiplicador de 100%
				1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 3;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		Arma arma = conjurador.getArmaEquipada();
		double danoBase = arma.getDanoBase();
		for (Personagem alvo : alvos) {
			if (!alvo.isAtivoNoCombate())
				continue;
			for (int i = 0; i < this.getTicksDeDano(); i++) {

				Efeito queimadura = EffectFactory.criarEfeito("Sangramento", 200, (int) (danoBase / 2));

				if (alvo.getEfeitosAtivos().containsKey("Sangramento")) {
					Efeito existente = alvo.getEfeitosAtivos().get("Sangramento");
					existente.setStacks(existente.getStacks() + 1);
					existente.setDuracaoTURestante(200); // Renova duração
					System.out.println(
							">>> " + alvo.getNome() + " está queimando mais forte! (" + existente.getStacks() + "x)");
				} else {
					alvo.adicionarEfeito(queimadura);
					System.out.println(">>> " + alvo.getNome() + " começou a sangrar!");
				}
				alvo.recalcularAtributosEstatisticas();

			}
		}

	}

}
