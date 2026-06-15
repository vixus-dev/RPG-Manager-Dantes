package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class Julgamento extends Habilidade {

	public Julgamento() {
		super("Julgamento", "Causa dano e remove 50% da armadura do alvo", TipoHabilidade.ATIVA, 
				0, // custo mana
				100, // custo TU
				1, // nivel
				TipoAlvo.INDIVIDUAL, 
				0, // tamanhoArea
				1.35, // multiplicadorDeDano
				1, // ticksDeDano
				Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 20;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " usa Julgamento!");
		for (Personagem alvo : alvos) {
			double halfArmor = alvo.getArmaduraTotal() * 0.5;
			Map<String, Double> mods = new HashMap<>();
			mods.put("ARMADURA_TOTAL", -halfArmor);
			
			Efeito armorBreak = new Efeito("Armor Break (Julgamento)", TipoEfeito.DEBUFF, 200, mods, 0, 0);
			alvo.adicionarEfeito(armorBreak);
			System.out.println(">>> " + alvo.getNome() + " teve 50% de sua armadura destruída por Julgamento!");
		}
	}
}
