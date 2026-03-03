package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class AngryAgain extends Habilidade {
	public AngryAgain() {
		super("Angry Again", "Aumenta o dano do próximo ataque de um aliado (5% por ponto de IS).",
				TipoHabilidade.ATIVA, 3, 100, 1, TipoAlvo.INDIVIDUAL, 0, 0, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		Personagem alvo = alvos.get(0);

		// Cálculo do Bônus: 7% (0.07) * Inspiração
		int inspiracao = conjurador.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 0);
		double bonusPercentual = inspiracao * 0.07;

		Map<String, Double> mods = new HashMap<>();
		mods.put("DANO_BONUS_PERCENTUAL", bonusPercentual);

		// Duração curta (apenas para o próximo ataque ou 1 turno)
		Efeito buff = new Efeito("Angry Again", TipoEfeito.BUFF, 300, mods, 0, 0);

		alvo.adicionarEfeito(buff);
		alvo.recalcularAtributosEstatisticas();

		System.out.println(
				">>> Angry Again: " + alvo.getNome() + " ganhou +" + (int) (bonusPercentual * 100) + "% de dano!");
	}
}