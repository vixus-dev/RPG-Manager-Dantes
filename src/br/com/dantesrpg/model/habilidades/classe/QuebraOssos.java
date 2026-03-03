package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.HashMap; // Import necessário
import java.util.List;
import java.util.Map; // Import necessário

public class QuebraOssos extends Habilidade {

	public QuebraOssos() {
		super("Quebra Ossos", // Nome
				"Causa 225% de dano e reduz a armadura do alvo por 250 TU.", // Descrição
				TipoHabilidade.ATIVA, 3, // Custo de Mana
				125, // Custo de TU
				5, // Nível Necessário
				TipoAlvo.INDIVIDUAL, 2.25, // Multiplicador de Dano (225%)
				1, // Ticks de Dano
				Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 2;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa " + getNome() + "!");

		if (alvos == null || alvos.isEmpty())
			return;
		Personagem alvo = alvos.get(0); // Pega o primeiro alvo

		// Cria o mapa de modificadores
		Map<String, Double> modificadores = new HashMap<>();
		modificadores.put("REDUCAO_DANO_MODIFICADOR", -0.30);

		// Cria o Efeito (DEBUFF)
		Efeito debuffArmadura = new Efeito("Armadura Quebrada", // Nome do Efeito
				TipoEfeito.DEBUFF, // Tipo (para o ícone vermelho)
				250, // Duração 
				modificadores, // O mapa com o -0.30
				0, 0 // (Não é DoT)
		);

		// Aplica no alvo
		alvo.adicionarEfeito(debuffArmadura);
		System.out.println(">>> " + alvo.getNome() + " está com [Osso Quebrado] (-30% Redução de Dano).");

		// Recalcula os stats do alvo
		alvo.recalcularAtributosEstatisticas();
	}
}