package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

/**
 * Sentinela — Invoca 1 "Sentinela Falso" em uma tile selecionada.
 * Disponível apenas durante a aura "Falsa Justiça".
 * Custo: 1 mana, 80 TU.
 */
public class SentinelaSkill extends Habilidade {

	public SentinelaSkill() {
		super("Sentinela", "Invoca 1 Sentinela Falso no local selecionado.",
				TipoHabilidade.ATIVA, 1, 80, 1,
				TipoAlvo.AREA, 1, 0, 0, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 8;
	}

	@Override
	public void executar(Personagem conjurador, int alvoX, int alvoY, List<Personagem> alvos,
			EstadoCombate estado, CombatManager manager) {

		System.out.println(">>> " + conjurador.getNome() + " invoca um Sentinela Falso!");

		Map<Atributo, Integer> stats = new HashMap<>();
		for (Atributo a : Atributo.values())
			stats.put(a, 1);
		stats.put(Atributo.DESTREZA, 5);
		stats.put(Atributo.TOPOR, 10);

		Personagem sentinela = new Personagem("Sentinela Falso",
				new br.com.dantesrpg.model.racas.RaçaPlaceholder(),
				new br.com.dantesrpg.model.classes.ClassePlaceholder(), 1, stats, 150, 0);

		sentinela.setFaccao(conjurador.getFaccao());
		sentinela.setPosX(alvoX);
		sentinela.setPosY(alvoY);

		ArmaMelee punhos = new ArmaMelee("Punhos", "Punhos", "Ataques corpo-a-corpo",
				Raridade.COMUM, 0, 5, 1, Atributo.FORCA, 80, 1);
		sentinela.setArmaEquipada(punhos);

		sentinela.setPesoEntidade(PesoEntidade.fromJsonId("medio_padrao"));
		sentinela.setMestreInvocador(conjurador);
		conjurador.registrarClone(sentinela);

		if (manager.getMainController().getMapController() != null) {
			estado.getCombatentes().add(sentinela);
			sentinela.setContadorTU(conjurador.getContadorTU() + 80);
			System.out.println(">>> Sentinela Falso surgiu em (" + alvoX + "," + alvoY + ")!");
		}

		manager.getMainController().atualizarInterfaceTotal();
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
	}
}
