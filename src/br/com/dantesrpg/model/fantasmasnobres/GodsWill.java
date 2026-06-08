package br.com.dantesrpg.model.fantasmasnobres;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;

import java.util.List;

/**
 * God's Will — Fantasma Nobre do "Arcanjo - O Executor".
 *
 * Custo: 0 mana, 50 TU. Cooldown: 0.
 * Aplica "Postura Perfeita" no Arcanjo por 500 TU, reduzindo o custo em TU
 * de seus golpes em 25% (via Raça Arcanjo.getReducaoTUPercentual).
 */
public class GodsWill extends FantasmaNobre {

	@Override
	public String getNome() {
		return "God's Will";
	}

	@Override
	public String getDescricao() {
		return "O Arcanjo entra em Postura Perfeita por 500 TU, reduzindo o custo em TU de seus golpes em 25%.";
	}

	@Override
	public int getCustoMana() {
		return 0;
	}

	@Override
	public int getCustoTU() {
		return 50;
	}

	@Override
	public int getCooldownTU() {
		return 0;
	}

	@Override
	public TipoAlvo getTipoAlvo() {
		return TipoAlvo.SI_MESMO;
	}

	@Override
	public int getTamanhoArea() {
		return 0;
	}

	@Override
	public int getNumeroDeAlvos() {
		return 0;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			AcaoMestreInput input, CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " assume a POSTURA PERFEITA (God's Will)!");

		Efeito postura = new Efeito("Postura Perfeita", TipoEfeito.BUFF, 500, null, 0, 0);
		manager.aplicarEfeito(conjurador, postura);
		conjurador.recalcularAtributosEstatisticas();
	}
}
