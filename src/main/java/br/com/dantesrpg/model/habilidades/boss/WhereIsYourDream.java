package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import br.com.dantesrpg.model.map.TerrainData;
import java.util.Collections;
import java.util.List;

public class WhereIsYourDream extends Habilidade {
	public WhereIsYourDream() {
		super("WHERE IS YOUR DREAM?", "Incendeia uma área selecionada de 5x5 por 150 TU.",
				TipoHabilidade.ATIVA, 0, 100, 1, TipoAlvo.AREA_QUADRADA, 5, 0, 0, Collections.emptyList());
	}

	@Override public int getCooldownTU() { return 180; }
	@Override public int getAlcanceMaximo() { return 7; }

	@Override
	public void executar(Personagem conjurador, int alvoX, int alvoY, List<Personagem> alvos,
			EstadoCombate estado, CombatManager manager) {
		if (manager.getMainController() != null && manager.getMainController().getMapController() != null) {
			int dano = Math.max(1, (conjurador.getArmaEquipada() != null
					? conjurador.getArmaEquipada().getDanoBase() : 10) / 10);
			for (int y = alvoY - 2; y <= alvoY + 2; y++) {
				for (int x = alvoX - 2; x <= alvoX + 2; x++) {
					manager.getMainController().getMapController().aplicarEfeitoNoSolo(x, y,
							new TerrainData.EfeitoInstance(TerrainData.TipoEfeitoSolo.FOGO, 150, dano, conjurador));
				}
			}
		}
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.err.println("WHERE IS YOUR DREAM? requer uma área selecionada no mapa.");
	}
}
