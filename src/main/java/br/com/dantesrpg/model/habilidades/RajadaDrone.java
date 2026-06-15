package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class RajadaDrone extends Habilidade {

	public RajadaDrone() {
		super("Rajada de Drone", "Dispara energia em 3 cones estreitos.", TipoHabilidade.ATIVA, 2, // Mana
				120, // TU
				1, // Nível
				TipoAlvo.CONE, 3, // Alcance
				1, // Dano base
				1, // Ticks
				Collections.emptyList());
	}

	@Override
	public int getAnguloCone() {
		return 15; // Cone bem fino (15 graus)
	}

	@Override
	public int getAlcanceMaximo() {
		return 5;
	}

	@Override
	public boolean afetaAliados() {
		return false; // Padrão: Habilidades de AoE causam fogo amigo
	}

	@Override
	public List<Integer> getAngulosDesvio() {
		// Retorna 3 ângulos: Centro, -20 graus e +20 graus
		return Arrays.asList(-20, 0, 20);
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " dispara uma rajada!");
	}
}