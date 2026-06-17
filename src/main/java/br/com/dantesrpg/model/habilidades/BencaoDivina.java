package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.util.DiceRoller;
import java.util.*;

public class BencaoDivina extends Habilidade {

	public BencaoDivina() {
		super("Benção Divina",
			  "Aplica um buff de dano, armadura ou cura com base no dado de Inspiração.",
			  TipoHabilidade.ATIVA,
			  2, // Custo Mana
			  100, // Custo TU
			  1, // Nível Necessário
			  TipoAlvo.INDIVIDUAL,
			  0.0, // Multiplicador de dano (não ofensivo)
			  0, // Ticks de dano
			  Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public List<String> getOpcoesSelection() {
		return Arrays.asList("Aprimorar Força", "Aprimorar Resistência", "Curar");
	}

	@Override
	public boolean afetaInimigos() {
		return false;
	}

	@Override
	public boolean afetaAliados() {
		return true;
	}

	@Override
	public boolean afetaSiMesmo() {
		return true;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (conjurador == null || alvos == null || alvos.isEmpty() || manager == null) {
			return;
		}

		Personagem target = alvos.get(0);
		if (target == null || !target.isAtivoNoCombate()) {
			return;
		}

		int inspiracao = conjurador.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 1);
		int maxDado = DiceRoller.getTipoDado(inspiracao);

		int roll = 0;
		if (manager.getLastInput() != null) {
			roll = manager.getLastInput().getResultadoDado("DADO_ATRIBUTO");
		}
		if (roll <= 0) {
			roll = DiceRoller.rolarDado(maxDado);
		}

		boolean isMaxRoll = (roll >= maxDado);
		double multiplicador = isMaxRoll ? 1.25 : 1.0;

		String escolha = "Aprimorar Força";
		if (manager.getLastInput() != null && manager.getLastInput().getOpcaoEscolhida() != null) {
			escolha = manager.getLastInput().getOpcaoEscolhida();
		}

		System.out.println(">>> " + conjurador.getNome() + " conjurou Benção Divina (Dado: " + roll + "/" + maxDado 
				+ ", Opção: " + escolha + ", MaxRoll Bonus: " + isMaxRoll + ")");

		if (escolha.equalsIgnoreCase("Aprimorar Força")) {
			double bonusDano = (roll * 0.10) * multiplicador;
			Map<String, Double> mods = new HashMap<>();
			mods.put("DANO_BONUS_PERCENTUAL", bonusDano);
			Efeito buff = new Efeito("Bênção Divina: Força", TipoEfeito.BUFF, 300, mods, 0, 0);
			target.adicionarEfeito(buff);
			System.out.println(">>> Bênção Divina: Força aplicada a " + target.getNome() + " (+" + String.format("%.0f", bonusDano * 100) + "% de dano) por 300 TU.");
		} 
		else if (escolha.equalsIgnoreCase("Aprimorar Resistência")) {
			double bonusArmadura = (roll * 10.0) * multiplicador;
			Map<String, Double> mods = new HashMap<>();
			mods.put("ARMADURA_TOTAL", bonusArmadura);
			Efeito buff = new Efeito("Bênção Divina: Resistência", TipoEfeito.BUFF, 300, mods, 0, 0);
			target.adicionarEfeito(buff);
			System.out.println(">>> Bênção Divina: Resistência aplicada a " + target.getNome() + " (+" + (int) bonusArmadura + " de armadura) por 300 TU.");
		} 
		else if (escolha.equalsIgnoreCase("Curar")) {
			double valorCura = (roll * 5.0) * multiplicador;
			target.setVidaAtual(target.getVidaAtual() + valorCura, estado, manager.getController());
			System.out.println(">>> Bênção Divina: Cura aplicada a " + target.getNome() + " (+" + (int) valorCura + " HP).");
		}

		target.recalcularAtributosEstatisticas();
	}
}
