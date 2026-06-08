package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class Destructa extends Habilidade {

	public Destructa() {
		super("Destructa", "Ataque em area devastador ao redor do conjurador.", TipoHabilidade.ATIVA, 2, 115, 1,
				TipoAlvo.AREA, 360, 2.0, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 4;
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	@Override
	public void executar(Personagem conjurador, int alvoX, int alvoY, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {

		Arma arma = conjurador.getArmaEquipada();
		double danoArma = (arma != null) ? arma.getDanoBase() : 10;
		if (arma != null && conjurador.getRaca() != null) {
			Personagem alvoRef = alvos.isEmpty() ? null : alvos.get(0);
			danoArma *= conjurador.getRaca().getMultiplicadorBonusDanoArma(conjurador, arma, alvoRef, estado, null);
		}
		double danoFinal = danoArma * getMultiplicadorDeDano();

		for (Personagem alvo : alvos) {
			manager.aplicarDanoAoAlvo(conjurador, alvo, danoFinal, false, TipoAcao.HABILIDADE, estado, 0);
		}
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		executar(conjurador, 0, 0, alvos, estado, manager);
	}
}
