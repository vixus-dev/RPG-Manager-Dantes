package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class BolaDeFogo extends Habilidade {

	public BolaDeFogo() {
		super("Bola de Fogo", "KABUUMMMMMM!!!!!!", TipoHabilidade.ATIVA, 2, // Custo de Mana
				120, // Custo de TU
				1, // Nível Necessário
				TipoAlvo.AREA_CIRCULAR, // Define o formato visual
				4, // Tamanho/Alcance da área (4 células)
				1, // Multiplicador de dano base
				1, // ticks de dano
				Collections.emptyList());
	}

	// --- CONFIGURAÇÃO DO CONE ---

	@Override
	public int getAlcanceMaximo() {
		return 3;
	}

	// --- LÓGICA DE EXECUÇÃO ---
	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa " + getNome() + "!");
	}
}