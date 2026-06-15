package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class InvocarFilthDeSangue extends Habilidade {

	public InvocarFilthDeSangue() {
		super("Invocar Filth de Sangue", "Invoca dois Filth de Sangue com Bash Strike.",
				TipoHabilidade.ATIVA, 2, 120, 1, TipoAlvo.AREA, 5, 0,
				Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public void executar(Personagem conjurador, int alvoX, int alvoY, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {

		for (int i = 0; i < 2; i++) {
			Map<Atributo, Integer> stats = new HashMap<>();
			for (Atributo a : Atributo.values())
				stats.put(a, 1);
			stats.put(Atributo.FORCA, 6);

			Personagem filth = new Personagem("Filth De Sangue", new br.com.dantesrpg.model.racas.RaçaPlaceholder(),
					new br.com.dantesrpg.model.classes.ClassePlaceholder(), 1, stats, 80, 3);

			filth.setFaccao(conjurador.getFaccao());

			// Posiciona o segundo um tile ao lado
			int spawnX = alvoX + i;
			int spawnY = alvoY;
			filth.setPosX(spawnX);
			filth.setPosY(spawnY);

			// Arma com Bash Strike concedida
			br.com.dantesrpg.model.ArmaMelee mandibula = new br.com.dantesrpg.model.ArmaMelee(
					"MandibulaSangue", "Mandíbula", "mandíbulas cobertas de sangue coagulado",
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
