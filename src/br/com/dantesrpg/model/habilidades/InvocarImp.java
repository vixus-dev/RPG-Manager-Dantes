package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class InvocarImp extends Habilidade {

	public InvocarImp() {
		super("Invocar Imp", "Invoca um Imp", TipoHabilidade.ATIVA, 1, 51, 1, TipoAlvo.AREA, 5, 0,
				Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 8;
	} // Onde pode spawnar

	@Override
	public void executar(Personagem conjurador, int alvoX, int alvoY, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {

		// Criação do Tentáculo
		Map<Atributo, Integer> stats = new HashMap<>();
		for (Atributo a : Atributo.values())
			stats.put(a, 1);
		stats.put(Atributo.FORCA, 6); // Dano razoável

		Personagem tentaculo = new Personagem("Diabrete", new br.com.dantesrpg.model.racas.RaçaPlaceholder(),
				new br.com.dantesrpg.model.classes.ClassePlaceholder(), 1, stats, 40, 5);

		tentaculo.setFaccao(conjurador.getFaccao());
		tentaculo.setPosX(alvoX); // Spawna onde clicou
		tentaculo.setPosY(alvoY);

		br.com.dantesrpg.model.ArmaMelee chicote = new br.com.dantesrpg.model.ArmaMelee("Chicote de prazer", "chicote",
				"toma safada, pirocada de bandido", Raridade.COMUM, 0, 6, 1, Atributo.FORCA, 80, 2);
		chicote.setTipoAlvo(TipoAlvo.INDIVIDUAL);
		tentaculo.setArmaEquipada(chicote);

		// Adiciona ao combate
		if (manager.getMainController().getMapController() != null) {
			// Verifica se a célula exata está livre, senão procura perto
			if (manager.getMainController().getMapController().getEfeitoNoSolo(alvoX, alvoY) == null) { // simplificação
				estado.getCombatentes().add(tentaculo);
				tentaculo.setContadorTU(conjurador.getContadorTU() + 50);
				manager.getMainController().atualizarInterfaceTotal();
				System.out.println(">>> Tentáculo surgiu!");
			}
		}
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		// TODO Auto-generated method stub

	}
}