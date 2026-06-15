package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class TecnicaDeAmplificação extends Habilidade {
	public TecnicaDeAmplificação() {
		super("Tecnica de Amplificação", "Aumenta o dano do próximo ataque de um aliado em 100%.", TipoHabilidade.ATIVA,
				3, 100, 1, TipoAlvo.INDIVIDUAL, 0, 0, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		Personagem alvo = alvos.get(0);
		double bonusPercentual = 1;

		Map<String, Double> mods = new HashMap<>();
		mods.put("DANO_BONUS_PERCENTUAL", bonusPercentual);

		Efeito buff = new Efeito("Tecnica de Amplificação", TipoEfeito.BUFF, 200, mods, 0, 0);

		alvo.adicionarEfeito(buff);
		alvo.recalcularAtributosEstatisticas();

		System.out.println(">>> Tecnica de Amplificação: " + alvo.getNome() + " ganhou +"
				+ (int) (bonusPercentual * 100) + "% de dano!");
	}
}