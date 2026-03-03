package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class Rezar extends Habilidade {
	public Rezar() {
		super("Rezar", "Escolha uma bênção para si mesmo.", TipoHabilidade.ATIVA, 0, 100, 1, TipoAlvo.SI_MESMO, 0, 0,
				Collections.emptyList());
	}

	@Override
	public int getCustoMana() {
		return 2;
	}

	@Override
	public List<String> getOpcoesSelection() {
		return Arrays.asList("Força", "Agilidade", "Proteção", "Resistência");
	}

	@Override
	public void executar(Personagem c, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
	}

	// Vamos criar um método específico para executar com escolha
	public void executarComEscolha(Personagem c, String escolha) {
		Map<String, Double> mods = new HashMap<>();
		String nomeBuff = "Bênção: " + escolha;

		switch (escolha) {
		case "Força":
			mods.put("DANO_BONUS_PERCENTUAL", 0.20);
			break;
		case "Agilidade":
			c.setContadorTU(Math.max(0, c.getContadorTU() - 20));
			break;
		case "Proteção":
			c.setEscudoAtual(c.getEscudoAtual() + (int) (c.getVidaMaxima() * 0.20));
			break;
		case "Resistência":
			mods.put("ARMADURA_TOTAL", 20.0);
			break;
		}

		Efeito buff = new Efeito(nomeBuff, TipoEfeito.BUFF, 250, mods, 0, 0);
		c.adicionarEfeito(buff);

		System.out.println(">>> Rezar (Escolha: " + escolha + "): " + nomeBuff);

		c.recalcularAtributosEstatisticas();
	}
}