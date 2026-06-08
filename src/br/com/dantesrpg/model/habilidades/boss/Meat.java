package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

/**
 * MEAT... — Invoca um Globulo Branco (stats idênticos ao bestiário) com 250 HP.
 * Custo: 2 mana, 125 TU.
 *
 * Não é um clone — é uma invocação independente (apenas o chefe usa esta habilidade).
 */
public class Meat extends Habilidade {

	public Meat() {
		super("MEAT...", "Invoca um Globulo Branco com 250 HP.",
				TipoHabilidade.ATIVA, 2, 125, 1,
				TipoAlvo.AREA, 1, 0, 0, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public void executar(Personagem conjurador, int alvoX, int alvoY, List<Personagem> alvos,
			EstadoCombate estado, CombatManager manager) {

		System.out.println(">>> " + conjurador.getNome() + " usa MEAT...!");

		CombatController mainController = manager.getMainController();
		if (mainController == null)
			return;

		// Spawn padrão do bestiário (stats, raça, arma, peso, propriedades vêm do JSON)
		int antes = estado.getCombatentes().size();
		mainController.spawnarMonstro("Globulo Branco", alvoX, alvoY);

		// Identifica o Personagem recém-criado (último adicionado à lista)
		if (estado.getCombatentes().size() <= antes) {
			System.err.println(">>> MEAT: falha ao invocar Globulo Branco.");
			return;
		}
		Personagem globulo = estado.getCombatentes().get(estado.getCombatentes().size() - 1);

		// Override apenas o HP base (tudo o mais permanece idêntico ao bestiário).
		// Precisa setar a BASE antes do recalc, senão o recalc reseta vidaMaxima
		// para vidaMaximaBase e o override é perdido.
		globulo.setVidaMaximaBase(250);
		globulo.setVidaMaxima(250);
		globulo.recalcularAtributosEstatisticas();
		globulo.setVidaAtual(Math.min(250, globulo.getVidaMaxima()));

		// Sincroniza com o TU do conjurador
		globulo.setContadorTU(conjurador.getContadorTU() + 50);

		System.out.println(">>> Globulo Branco (250 HP) surgiu em (" + alvoX + "," + alvoY + ")!");

		mainController.atualizarInterfaceTotal();
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
	}
}
