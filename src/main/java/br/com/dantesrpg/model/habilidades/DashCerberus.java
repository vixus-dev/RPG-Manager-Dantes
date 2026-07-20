package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

/** Avanço em linha do Cerberus de Coral. */
public class DashCerberus extends Habilidade {
	public DashCerberus() {
		super("Dash Cerberus", "Avança 5 casas e atinge uma linha de 4 casas de largura por 5 de alcance.",
				TipoHabilidade.ATIVA, 0, 100, 1, TipoAlvo.LINHA, 4, 1.0, 1, Collections.emptyList());
	}
	@Override public int getAlcanceMaximo() { return 5; }

	@Override
	public void executar(Personagem conjurador, int alvoX, int alvoY, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		int direcaoX = Integer.compare(alvoX, conjurador.getPosX());
		int direcaoY = Integer.compare(alvoY, conjurador.getPosY());
		if (direcaoX == 0 && direcaoY == 0) return;
		conjurador.setPosX(conjurador.getPosX() + (direcaoX * getAlcanceMaximo()));
		conjurador.setPosY(conjurador.getPosY() + (direcaoY * getAlcanceMaximo()));
		System.out.println(">>> DASH CERBERUS: " + conjurador.getNome() + " avançou 5 casas.");
	}

	@Override public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) { }
}
