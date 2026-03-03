package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.Collections;
import java.util.List;

public class CorteTemporal extends Habilidade {

	public CorteTemporal() {
		super("Corte Temporal", "Um corte que distorce o espaço-tempo, ignorando defesa. (500% Dano)",
				TipoHabilidade.ATIVA, 6, 150, 1, TipoAlvo.INDIVIDUAL, 0, 5.0, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 10;
	}

	@Override
	public boolean afetaAliados() {
		return false; // Padrão: Habilidades de AoE causam fogo amigo
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> Virgilio: SHCUM!");
	}
}