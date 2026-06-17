package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;

import java.util.Collections;
import java.util.List;

/**
 * Arremesso de Escudo — Habilidade concedida pelo Despertar Divino (Sarvant).
 *
 * 115 TU, 1 Mana, AREA_QUADRADA 3×3.
 * Arremessa um escudo divino que causa 0.75x de dano em todos os alvos na área.
 */
public class ArremessoDeEscudo extends Habilidade {

	public ArremessoDeEscudo() {
		super("Arremesso de Escudo",
			  "Arremessa um escudo divino em uma área 3x3, causando 0.75x de dano a todos os inimigos.",
			  TipoHabilidade.ATIVA,
			  1,    // Custo Mana
			  115,  // Custo TU
			  1,    // Nível Necessário
			  TipoAlvo.AREA_QUADRADA,
			  3,    // Tamanho da Área (3×3)
			  0.75, // Multiplicador de dano
			  1,    // Ticks de dano
			  Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 4;
	}

	@Override
	public boolean afetaInimigos() {
		return true;
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	@Override
	public boolean afetaSiMesmo() {
		return false;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (conjurador == null || alvos == null || alvos.isEmpty() || manager == null) {
			return;
		}

		System.out.println(">>> " + conjurador.getNome() + " arremessa um Escudo Divino! (3x3, 0.75x dano)");
		// O dano é resolvido pelo CombatManager.resolverAcao via multiplicador 0.75x
	}
}
