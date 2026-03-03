package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class RouboDeEssencia extends Habilidade {

	public RouboDeEssencia() {
		super("Roubo de Essência",
				"Cria uma cópia de tinta do alvo. Dano na cópia reflete 20% no original. Morte da cópia cura o original.",
				TipoHabilidade.ATIVA, 1, 100, 1, TipoAlvo.INDIVIDUAL, 0, 0, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (alvos.isEmpty())
			return;
		Personagem alvoOriginal = alvos.get(0);

		System.out.println(">>> Zeraphon extrai a essência de " + alvoOriginal.getNome() + "!");

		Map<Atributo, Integer> statsDummy = new HashMap<>();
		statsDummy.put(Atributo.TOPOR, 0); // Sem defesa para ser fácil de bater
		statsDummy.put(Atributo.DESTREZA, 1);

		Personagem essencia = new Personagem("Essência de " + alvoOriginal.getNome(),
				new br.com.dantesrpg.model.racas.RaçaPlaceholder(),
				new br.com.dantesrpg.model.classes.ClassePlaceholder(), 1, statsDummy, alvoOriginal.getVidaAtual(), 0);

		essencia.setFaccao(conjurador.getFaccao()); // É aliado do Zeraphon (jogadores podem bater)
		essencia.adicionarPropriedade("VINCULO_DANO:" + alvoOriginal.getNome());
		essencia.adicionarPropriedade("VINCULO_CURA_MORTE:" + alvoOriginal.getNome());

		// 3. Spawna no Mapa
		if (manager.getMainController().getMapController() != null) {
			javafx.util.Pair<Integer, Integer> pos = manager.getMainController().getMapController()
					.encontrarCelulaLivreMaisProxima(alvoOriginal.getPosX(), alvoOriginal.getPosY());

			if (pos != null) {
				essencia.setPosX(pos.getKey());
				essencia.setPosY(pos.getValue());
				estado.getCombatentes().add(essencia);
				manager.getMainController().atualizarInterfaceTotal();
				System.out.println(">>> Essência spawnada em (" + pos.getKey() + "," + pos.getValue() + ")");
			} else {
				System.out.println(">>> Falha: Sem espaço para a Essência.");
			}
		}
	}
}