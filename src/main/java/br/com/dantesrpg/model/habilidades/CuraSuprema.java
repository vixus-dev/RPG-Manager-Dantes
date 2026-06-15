package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class CuraSuprema extends Habilidade {
	public CuraSuprema() {
		super("Cura Suprema", "Cura 3 aliados em 25 de HP.", TipoHabilidade.ATIVA, 1, 100, 1, TipoAlvo.MULTIPLOS, 0, 1,
				Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public int getNumeroDeAlvos() {
		return 3;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		for (int alvoAtual = 0; alvoAtual < alvos.size(); alvoAtual++) {
			Personagem alvo = alvos.get(alvoAtual);
			int curaTotal = 25;
			alvo.setVidaAtual(alvo.getVidaAtual() + curaTotal, estado, manager.getController());
			System.out.println(">>> curasuprema: " + alvo.getNome() + " recuperou " + curaTotal + " HP.");
		}
	}
}