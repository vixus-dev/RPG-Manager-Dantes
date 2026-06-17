package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Postura de Bloqueio — Habilidade concedida pelo Despertar Divino (Sarvant).
 *
 * 125 TU, 2 Mana, SI_MESMO.
 * Concede efeito "Manto Divino" por 200 TU: o próximo teste de bloqueio
 * do usuário é fortalecido em +2. Após uso, o efeito é removido.
 */
public class PosturaDeBloqueio extends Habilidade {

	public PosturaDeBloqueio() {
		super("Postura de Bloqueio",
			  "Assume uma postura de guarda divina. O próximo teste de bloqueio é fortalecido em +2.",
			  TipoHabilidade.ATIVA,
			  2,   // Custo Mana
			  125, // Custo TU
			  1,   // Nível Necessário
			  TipoAlvo.SI_MESMO,
			  0.0, // Multiplicador de dano
			  0,   // Ticks de dano
			  Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (conjurador == null || manager == null) {
			return;
		}

		System.out.println(">>> " + conjurador.getNome() + " assume a Postura de Bloqueio!");

		// Cria o efeito "Manto Divino" com +2 de bônus de bloqueio
		Map<String, Double> mods = new HashMap<>();
		mods.put("BONUS_BLOQUEIO", 2.0);
		Efeito manto = new Efeito("Manto Divino", TipoEfeito.BUFF, 200, mods, 0, 0);
		manager.aplicarEfeito(conjurador, manto);

		System.out.println(">>> Manto Divino aplicado! Próximo teste de bloqueio: +2. (200 TU)");
		conjurador.recalcularAtributosEstatisticas();
	}
}
