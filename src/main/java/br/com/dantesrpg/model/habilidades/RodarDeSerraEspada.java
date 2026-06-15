package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.util.EffectFactory;

import java.util.*;

public class RodarDeSerraEspada extends Habilidade {

	public RodarDeSerraEspada() {
		super("Rodar Serra-Espada", "Roda roda jequiti", TipoHabilidade.ATIVA, 5, 160, 1, TipoAlvo.AREA, 4, 1.5, 1,
				Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 5;
	}

	// --- LÓGICA DE EXECUÇÃO ---
	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa " + getNome() + "!");

		Arma arma = conjurador.getArmaEquipada();
		double danoBase = arma.getDanoBase();

		// Loop de Alvos (Quem estava no cone)
		for (Personagem alvo : alvos) {
			if (!alvo.isAtivoNoCombate())
				continue;
			// Loop de Ticks (3 vezes)
			for (int i = 0; i < this.getTicksDeDano(); i++) {

				// Chance de Sangrar (50% por tick)
				if (Math.random() < 0.50) {
					// Verifica se já tem pra não spammar log, ou deixa acumular stacks
					// Usando sua Factory:
					Efeito queimadura = EffectFactory.criarEfeito("Sangramento", 200, (int) (danoBase / 3));

					if (alvo.getEfeitosAtivos().containsKey("Sangramento")) {
						Efeito existente = alvo.getEfeitosAtivos().get("Sangramento");
						existente.setStacks(existente.getStacks() + 1);
						existente.setDuracaoTURestante(200); // Renova duração
						System.out.println(">>> " + alvo.getNome() + " está Sangramento mais ainda! ("
								+ existente.getStacks() + "x)");
					} else {
						alvo.adicionarEfeito(queimadura);
						System.out.println(">>> " + alvo.getNome() + " começou a Sangramento!");
					}
					alvo.recalcularAtributosEstatisticas();
				}
			}
		}
	}
}