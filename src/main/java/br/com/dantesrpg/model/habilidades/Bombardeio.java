package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.util.EffectFactory; // Usando sua Factory
import java.util.*;

public class Bombardeio extends Habilidade {

	public Bombardeio() {
		super("Bombardeio", "KABUUMMMMMM!!!!!!", TipoHabilidade.ATIVA, 3, // Custo de Mana
				200, // Custo de TU
				1, // Nível Necessário
				TipoAlvo.AREA_CIRCULAR, // Define o formato visual
				5, // Tamanho/Alcance da área (4 células)
				1, // Multiplicador de dano base
				1, // ticks de dano
				Collections.emptyList());
	}

	// --- CONFIGURAÇÃO DO CONE ---
	@Override
	public int getAnguloCone() {
		return 135; // Define o ângulo exato para o MapController desenhar
	}

	@Override
	public int getAlcanceMaximo() {
		return 5;
	}

	@Override
	public boolean afetaAliados() {
		return false; // Padrão: Habilidades de AoE causam fogo amigo
	}

	// --- LÓGICA DE EXECUÇÃO ---
	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa " + getNome() + "!");

		Arma arma = conjurador.getArmaEquipada();
		double danoBase = arma.getDanoBase();
	}
}