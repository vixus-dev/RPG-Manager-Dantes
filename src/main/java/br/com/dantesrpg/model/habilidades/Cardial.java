package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class Cardial extends Habilidade {

	public Cardial() {
		super("Cardial", "Invoca 2 Vermes de Sangue.", TipoHabilidade.ATIVA, 1, 80, 1, TipoAlvo.AREA, 5, 0,
				Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public int getCooldownTU() {
		return 80;
	}

	@Override
	public void executar(Personagem conjurador, int alvoX, int alvoY, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {

		for (int i = 0; i < 2; i++) {
			Map<Atributo, Integer> stats = new HashMap<>();
			for (Atributo a : Atributo.values())
				stats.put(a, 1);
			stats.put(Atributo.FORCA, 4);

			Personagem verme = new Personagem("Verme de Sangue", new br.com.dantesrpg.model.racas.RaçaPlaceholder(),
					new br.com.dantesrpg.model.classes.ClassePlaceholder(), 1, stats, 15, 2);

			verme.setFaccao(conjurador.getFaccao());

			int spawnX = alvoX + i;
			int spawnY = alvoY;
			verme.setPosX(spawnX);
			verme.setPosY(spawnY);

			br.com.dantesrpg.model.ArmaMelee mandibula = new br.com.dantesrpg.model.ArmaMelee(
					"MandibulaSangue", "Mandibula", "dentes cobertos de sangue coagulado",
					Raridade.COMUM, 0, 8, 1, Atributo.FORCA, 90, 2);
			mandibula.setTipoAlvo(TipoAlvo.INDIVIDUAL);
			verme.setArmaEquipada(mandibula);

			if (manager.getMainController().getMapController() != null) {
				estado.getCombatentes().add(verme);
				verme.setContadorTU(conjurador.getContadorTU() + 50 + (i * 10));
				System.out.println(">>> Verme de Sangue #" + (i + 1) + " surgiu!");
			}
		}

		manager.getMainController().atualizarInterfaceTotal();
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
	}
}
