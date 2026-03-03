package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class TrueLove extends Habilidade {

	public TrueLove() {
		super("True love!", "Acumula desejo em um pequeno espaço, e em pouco tempo para de ser estavel",
				TipoHabilidade.ATIVA, 2, // Custo de Mana
				125, // Custo de TU
				1, // Nível Necessário
				TipoAlvo.AREA, // Define o formato visual
				4, // Tamanho/Alcance da área (4 células)
				1.5, // Multiplicador de dano base
				1, // ticks de dano
				Collections.emptyList());
	}

	@Override
	public boolean afetaAliados() {
		return false; // Padrão: Habilidades de AoE causam fogo amigo
	}

	// --- CONFIGURAÇÃO DO CONE ---

	@Override
	public int getAlcanceMaximo() {
		return 4;
	}

	// --- LÓGICA DE EXECUÇÃO ---
	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa " + getNome() + "!");
	}
}