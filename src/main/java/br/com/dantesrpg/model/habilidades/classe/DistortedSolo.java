package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class DistortedSolo extends Habilidade {
	public DistortedSolo() {
		super("Distorted Solo", "1d20 (IS). 1-10: 50% Dano. 11-20: Choque + Debuff.", TipoHabilidade.ATIVA, 2, 100, 1,
				TipoAlvo.INDIVIDUAL, 1.0, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public void executar(Personagem c, List<Personagem> a, EstadoCombate e, CombatManager m) {
	}

	public void executarSolo(AcaoMestreInput input, EstadoCombate estado, CombatManager manager) {
		Personagem ator = input.getAtor();
		Personagem alvo = input.getAlvos().get(0);
		int rolagem = input.getResultadoDado("DADO_ATRIBUTO");

		// Dano Base = INSPIRACAO do Jogador
		int inspiracao = ator.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 0);
		double danoBase = (double) inspiracao;

		double multiplicador = 0.5; // 1-10

		if (rolagem >= 11) {
			multiplicador = 1.0;

			// Choque: 10% do dano de Inspiração
			int danoChoque = Math.max(1, (int) (danoBase * 0.10));
			// Intervalo 100 (1 tick)
			Efeito choque = new Efeito("Choque", TipoEfeito.DOT, 10, Map.of(), danoChoque, 9); // Intervalo 99 garante
																								// que tique logo
			alvo.adicionarEfeito(choque);

			// Debuff Dano
			Map<String, Double> mods = new HashMap<>();
			mods.put("DANO_BONUS_PERCENTUAL", -0.20);
			Efeito debuff = new Efeito("Ouvidos Sangrando", TipoEfeito.DEBUFF, 200, mods, 0, 0);
			alvo.adicionarEfeito(debuff);
			alvo.recalcularAtributosEstatisticas();
		}

		// Dano Final (Baseado em IS)
		int danoFinal = (int) (danoBase * multiplicador);
		manager.aplicarDanoAoAlvo(ator, alvo, danoFinal, false, TipoAcao.HABILIDADE, estado);
	}
}