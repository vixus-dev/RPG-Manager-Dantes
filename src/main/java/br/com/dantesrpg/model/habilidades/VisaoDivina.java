package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.util.DiceRoller;
import java.util.*;

public class VisaoDivina extends Habilidade {

	public VisaoDivina() {
		super("Visão Divina",
			  "O usuário roda um teste fixo de Sagacidade do seu dado. Efeito narrativo.",
			  TipoHabilidade.ATIVA,
			  3, // Custo Mana
			  120, // Custo TU
			  1, // Nível Necessário
			  TipoAlvo.SI_MESMO,
			  0.0, // Multiplicador de dano
			  0, // Ticks de dano
			  Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public boolean afetaInimigos() {
		return false;
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	@Override
	public boolean afetaSiMesmo() {
		return true;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (conjurador == null || manager == null) {
			return;
		}

		int percepcao = conjurador.getAtributosFinais().getOrDefault(Atributo.PERCEPCAO, 1);
		int tipoDado = DiceRoller.getTipoDado(percepcao);

		int roll = 0;
		if (manager.getLastInput() != null) {
			roll = manager.getLastInput().getResultadoDado("DADO_ATRIBUTO");
		}
		if (roll <= 0) {
			roll = DiceRoller.rolarDado(tipoDado);
		}

		System.out.println(">>> Visão Divina: " + conjurador.getNome() + " rodou Percepção (d" + tipoDado + "): " + roll);
	}
}
