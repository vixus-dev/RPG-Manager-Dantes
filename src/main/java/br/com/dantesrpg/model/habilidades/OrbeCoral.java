package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

/** Projétil em bola 3x3 do Cerberus de Coral. */
public class OrbeCoral extends Habilidade {
	public OrbeCoral() {
		super("Orbe Coral", "Dispara uma esfera de coral que causa 150% de dano em uma área circular 3x3.",
				TipoHabilidade.ATIVA, 0, 80, 1, TipoAlvo.AREA_CIRCULAR, 3, 1.5, 1, Collections.emptyList());
	}
	@Override public int getAlcanceMaximo() { return 4; }
	@Override public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> ORBE CORAL: " + conjurador.getNome() + " lança uma esfera de coral.");
	}
}
