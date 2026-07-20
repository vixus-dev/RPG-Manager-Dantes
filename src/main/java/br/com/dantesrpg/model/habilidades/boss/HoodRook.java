package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

/** Dispara fogo amaldiçoado na área selecionada. */
public class HoodRook extends Habilidade {
	public HoodRook() {
		super("Hood rook", "Dispara um projétil que cria fogo amaldiçoado em uma área 3x3.",
				TipoHabilidade.ATIVA, 0, 100, 1, TipoAlvo.AREA_QUADRADA, 3, 0, 0, Collections.emptyList());
	}

	@Override public int getCooldownTU() { return 80; }
	@Override public int getAlcanceMaximo() { return 7; }

	@Override
	public void executar(Personagem conjurador, int alvoX, int alvoY, List<Personagem> alvos,
			EstadoCombate estado, CombatManager manager) {
		if (manager.getMainController() != null && manager.getMainController().getMapController() != null) {
			manager.getMainController().getMapController().criarAreaDeFogoAmaldicoado(alvoX, alvoY, 1, 200, conjurador);
		}
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.err.println("Hood rook requer uma área selecionada no mapa.");
	}
}
