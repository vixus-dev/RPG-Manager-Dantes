package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class PularStompLuxuria extends Habilidade {

	public PularStompLuxuria() {
		super("Pulo Pesado", "Pula, criando uma onda de choque onde cair", TipoHabilidade.ATIVA, 1, // Custo de Mana
				125, // Custo de TU
				1, // Nível Necessário
				TipoAlvo.AREA, // Define o formato visual
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