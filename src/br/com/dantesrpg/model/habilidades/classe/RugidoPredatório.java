package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import br.com.dantesrpg.model.util.EffectFactory;

import java.util.Collections;
import java.util.HashMap; // Import necessário
import java.util.List;
import java.util.Map; // Import necessário

public class RugidoPredatório extends Habilidade {

	public RugidoPredatório() {
		super("Rugido predatório", // Nome
				"Causa dano e reduz a armadura do alvo por 200 TU.", // Descrição
				TipoHabilidade.ATIVA, 2, // Custo de Mana
				90, // Custo de TU
				1, // Nível Necessário
				TipoAlvo.AREA, 4, 0.25, // Multiplicador de Dano (25%)
				1, // Ticks de Dano
				Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 4;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa " + getNome() + "!");

		for (Personagem alvo : alvos) {
			if (!alvo.isAtivoNoCombate())
				continue;
			// Cria o mapa de modificadores
			Map<String, Double> modificadores = new HashMap<>();
			modificadores.put("REDUCAO_DANO_MODIFICADOR", -0.30);

			// Cria o Efeito
			Efeito debuffArmadura = new Efeito("Enfraquecido (-30%)", // Nome do Efeito
					TipoEfeito.DEBUFF, // Tipo (para o ícone vermelho)
					250, // Duração
					modificadores, // O mapa com o -0.30
					0, 0 // (Não é DoT)
			);

			// Aplica no alvo
			alvo.adicionarEfeito(debuffArmadura);

			// Recalcula os stats do alvo
			alvo.recalcularAtributosEstatisticas();

		}
	}
}