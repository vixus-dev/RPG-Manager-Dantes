package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class InvestidaDeSerraEspada extends Habilidade {

	public InvestidaDeSerraEspada() {
		super("Investida Serra-Espada", "Dá Uma bela Lapada seca", TipoHabilidade.ATIVA, -1, 110, 1, TipoAlvo.CONE, 2,
				1, 1, Collections.emptyList());
	}

	// --- CONFIGURAÇÃO DO CONE ---
	@Override
	public int getAnguloCone() {
		return 150;
	}

	@Override
	public int getAlcanceMaximo() {
		return 2;
	}

	// --- LÓGICA DE EXECUÇÃO ---
	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa " + getNome() + "!");
	}
}