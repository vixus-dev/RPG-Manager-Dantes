package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.map.TerrainData.EfeitoInstance;
import br.com.dantesrpg.model.map.TerrainData.TipoEfeitoSolo;
import java.util.Collections;
import java.util.List;

public class CargaDimensional extends Habilidade {

	public CargaDimensional() {
		super("Carga Dimensional", "Cria uma fenda dimensional (Portal) no local alvo e nos seus pés.",
				TipoHabilidade.ATIVA, 2, 80, 1, TipoAlvo.AREA_QUADRADA, 1, 0, 0, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 8;
	}

	@Override
	public void executar(Personagem conjurador, int alvoX, int alvoY, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		if (manager.getMainController().getMapController() == null)
			return;

		System.out.println(">>> Virgilio rasga o tecido da realidade!");

		// Portal 1: No Alvo
		criarPortal(alvoX, alvoY, conjurador, manager);

		// Portal 2: Nos pés do Conjurador
		criarPortal(conjurador.getPosX(), conjurador.getPosY(), conjurador, manager);
	}

	private void criarPortal(int x, int y, Personagem criador, CombatManager manager) {
		// Cria efeito de solo do tipo PORTAL
		// Duração 500 TU, Dano 0 (é utilitário)
		EfeitoInstance portal = new EfeitoInstance(TipoEfeitoSolo.PORTAL, 500, 0, criador);
		manager.getMainController().getMapController().aplicarEfeitoNoSolo(x, y, portal);
		System.out.println(">>> Portal aberto em (" + x + "," + y + ")");
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		// Fallback
	}
}