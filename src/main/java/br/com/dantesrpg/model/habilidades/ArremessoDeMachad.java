package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class ArremessoDeMachad extends Habilidade {

	public ArremessoDeMachad() {
		super("Arremesso de Machado", "Joga o machado em uma linha reta", TipoHabilidade.ATIVA, 1, // Custo de Mana
				115, // Custo de TU
				1, // Nível Necessário
				TipoAlvo.LINHA, // Define o formato visual
				4, // Tamanho/Alcance da área (4 células)
				1.5, // Multiplicador de dano base
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