package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

/**
 * Arremesso de Tridente — Habilidade de área concedida pelo item Tridente.
 *
 * AOE Circular (Bola) 3x3, alcance 7, 1.5x dano. Custo: 100 TU, 1 de mana.
 */
public class ArremessoDeTridente extends Habilidade {

	public ArremessoDeTridente() {
		super("Arremesso de Tridente",
				"Arremessa o tridente gerando uma explosão de água em área circular de 3x3.",
				TipoHabilidade.ATIVA,
				1,    // Custo de Mana
				100,  // Custo de TU
				1,    // Nível Necessário
				TipoAlvo.AREA_CIRCULAR,
				3,    // Tamanho da Área (3x3)
				1.5,  // Multiplicador de Dano
				1,    // Ticks de Dano
				Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 7;
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	@Override
	public boolean afetaInimigos() {
		return true;
	}

	@Override
	public boolean afetaSiMesmo() {
		return false;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " arremessa o Tridente criando um redemoinho na área alvo!");
	}
}
