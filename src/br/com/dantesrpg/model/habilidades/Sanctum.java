package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class Sanctum extends Habilidade {

	public Sanctum() {
		super("Sanctum", "Sacrifica 5% da vida atual para invocar 2 Filth de Sangue.", TipoHabilidade.ATIVA, 2, 100, 1,
				TipoAlvo.AREA, 5, 0, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public void executar(Personagem conjurador, int alvoX, int alvoY, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {

		// Sacrifica 5% da vida atual
		double sacrificio = conjurador.getVidaAtual() * 0.05;
		conjurador.setVidaAtual(conjurador.getVidaAtual() - sacrificio);
		System.out.println(">>> " + conjurador.getNome() + " sacrificou " + (int) sacrificio + " HP!");

		for (int i = 0; i < 2; i++) {
			Map<Atributo, Integer> stats = new HashMap<>();
			for (Atributo a : Atributo.values())
				stats.put(a, 1);
			stats.put(Atributo.FORCA, 6);

			Personagem filth = new Personagem("Filth De Sangue", new br.com.dantesrpg.model.racas.RaçaPlaceholder(),
					new br.com.dantesrpg.model.classes.ClassePlaceholder(), 1, stats, 80, 3);

			filth.setFaccao(conjurador.getFaccao());

			int spawnX = alvoX + i;
			int spawnY = alvoY;
			filth.setPosX(spawnX);
			filth.setPosY(spawnY);

			br.com.dantesrpg.model.ArmaMelee mandibula = new br.com.dantesrpg.model.ArmaMelee(
					"MandibulaSangue", "Mandibula", "mandibulas cobertas de sangue coagulado",
					Raridade.COMUM, 0, 6, 1, Atributo.FORCA, 80, 2);
			mandibula.setTipoAlvo(TipoAlvo.INDIVIDUAL);
			mandibula.addHabilidadeConcedida("Bash Strike");
			filth.setArmaEquipada(mandibula);

			if (manager.getMainController().getMapController() != null) {
				estado.getCombatentes().add(filth);
				filth.setContadorTU(conjurador.getContadorTU() + 50 + (i * 10));
				System.out.println(">>> Filth De Sangue #" + (i + 1) + " surgiu!");
			}
		}

		manager.getMainController().atualizarInterfaceTotal();
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
	}
}
