package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.elementos.ObjetoDestrutivel;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

public class ParedaoDePedra extends Habilidade {
	public ParedaoDePedra() {
		super("Paredão de Pedra",
				"Invoca 3 blocos de pedra no mapa. Cada bloco tem 10 + ISx2 de HP. Clique 3 posições.",
				TipoHabilidade.ATIVA, 3, 80, 1, TipoAlvo.AREA_QUADRADA, 1, 0, 0, Collections.emptyList());
	}

	@Override
	public int getNumeroDeAlvos() {
		return 3;
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		int inspiracao = conjurador.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 0);
		int hpBloco = 10 + (inspiracao * 2);

		System.out.println(conjurador.getNome() + " invoca um Paredão de Pedra! HP por bloco: " + hpBloco);

		// Cria 3 blocos de pedra nas posições dos alvos
		for (int i = 0; i < Math.min(3, alvos.size()); i++) {
			Personagem alvo = alvos.get(i);
			ObjetoDestrutivel bloco = new ObjetoDestrutivel("Bloco de Pedra", hpBloco, 0, true);
			bloco.setPosX(alvo.getPosX());
			bloco.setPosY(alvo.getPosY());
			bloco.setFaccao(conjurador.getFaccao());
			estado.getCombatentes().add(bloco);
			System.out.println(">>> Bloco de Pedra criado em (" + alvo.getPosX() + ", " + alvo.getPosY()
					+ ") com " + hpBloco + " HP.");
		}

		// Se não tiver alvos suficientes, cria blocos adjacentes ao conjurador
		if (alvos.size() < 3) {
			int blocosRestantes = 3 - alvos.size();
			int baseX = conjurador.getPosX();
			int baseY = conjurador.getPosY();
			int[][] offsets = { { 1, 0 }, { -1, 0 }, { 0, 1 } };
			int offsetIdx = 0;

			for (int i = 0; i < blocosRestantes && offsetIdx < offsets.length; i++, offsetIdx++) {
				ObjetoDestrutivel bloco = new ObjetoDestrutivel("Bloco de Pedra", hpBloco, 0, true);
				bloco.setPosX(baseX + offsets[offsetIdx][0]);
				bloco.setPosY(baseY + offsets[offsetIdx][1]);
				bloco.setFaccao(conjurador.getFaccao());
				estado.getCombatentes().add(bloco);
				System.out.println(">>> Bloco de Pedra criado em ("
						+ (baseX + offsets[offsetIdx][0]) + ", " + (baseY + offsets[offsetIdx][1])
						+ ") com " + hpBloco + " HP.");
			}
		}

		manager.getMainController().atualizarInterfaceTotal();
	}

	@Override
	public void executar(Personagem conjurador, int alvoX, int alvoY, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		int inspiracao = conjurador.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 0);
		int hpBloco = 10 + (inspiracao * 2);

		System.out.println(conjurador.getNome() + " invoca um Paredão de Pedra! HP por bloco: " + hpBloco);

		// Cria 3 blocos em linha horizontal a partir do epicentro
		for (int i = 0; i < 3; i++) {
			ObjetoDestrutivel bloco = new ObjetoDestrutivel("Bloco de Pedra", hpBloco, 0, true);
			bloco.setPosX(alvoX + i);
			bloco.setPosY(alvoY);
			bloco.setFaccao(conjurador.getFaccao());
			estado.getCombatentes().add(bloco);
			System.out.println(">>> Bloco de Pedra criado em (" + (alvoX + i) + ", " + alvoY
					+ ") com " + hpBloco + " HP.");
		}

		manager.getMainController().atualizarInterfaceTotal();
	}
}
