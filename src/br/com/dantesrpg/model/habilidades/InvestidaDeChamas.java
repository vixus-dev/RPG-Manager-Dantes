package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.util.EffectFactory; // Usando sua Factory
import java.util.*;

public class InvestidaDeChamas extends Habilidade {

	public InvestidaDeChamas() {
		super("Investida Flamejante",
				"Cospe chamas em um cone de 45°. 5 Ticks de dano. Cada tick tem 30% de chance de queimar.",
				TipoHabilidade.ATIVA, 3, // Custo de Mana
				200, // Custo de TU
				1, // Nível Necessário
				TipoAlvo.CONE, // Define o formato visual
				2, // Tamanho/Alcance da área (4 células)
				0.3, // Multiplicador de dano base
				3, Collections.emptyList());
	}

	// --- CONFIGURAÇÃO DO CONE ---
	@Override
	public int getAnguloCone() {
		return 135; // Define o ângulo exato para o MapController desenhar
	}

	@Override
	public int getAlcanceMaximo() {
		return 2; // Alcance do cone
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

				// Chance de Queimar (50% por tick)
				if (Math.random() < 0.50) {
					Efeito queimadura = EffectFactory.criarEfeito("Queimadura", 200, (int) (danoBase / 2)); // 200 TU, 3
																											// de dano
					if (alvo.getEfeitosAtivos().containsKey("Queimadura")) {
						Efeito existente = alvo.getEfeitosAtivos().get("Queimadura");
						existente.setStacks(existente.getStacks() + 1);
						existente.setDuracaoTURestante(200); // Renova duração
						System.out.println(">>> " + alvo.getNome() + " está queimando mais forte! ("
								+ existente.getStacks() + "x)");
					} else {
						alvo.adicionarEfeito(queimadura);
						System.out.println(">>> " + alvo.getNome() + " começou a queimar!");
					}
					alvo.recalcularAtributosEstatisticas();
				}
			}
		}
	}
}