package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class ArremessoDeOrb extends Habilidade {

	public ArremessoDeOrb() {
		super("Arremesso de Orb", "KABUUMMMMMM!!!!!!", TipoHabilidade.ATIVA, 2, // Custo de Mana
				120, // Custo de TU
				1, // Nível Necessário
				TipoAlvo.AREA_QUADRADA, // Define o formato visual
				4, // Tamanho/Alcance da área (4 células)
				0.75, // Multiplicador de dano base
				1, // ticks de dano
				Collections.emptyList());
	}

	// --- CONFIGURAÇÃO DO CONE ---

	@Override
	public int getAlcanceMaximo() {
		return 4;
	}

	@Override
	public boolean afetaAliados() {
		return false; // Padrão: Habilidades de AoE causam fogo amigo
	}

	// --- LÓGICA DE EXECUÇÃO ---
	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa " + getNome() + "!");
	}
}