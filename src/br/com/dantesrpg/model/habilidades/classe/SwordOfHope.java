package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class SwordOfHope extends Habilidade {
	public SwordOfHope() {
		super("Sword of Hope", "400% Dano Mágico em todos os inimigos.", TipoHabilidade.ATIVA, 4, 400, 8, TipoAlvo.AREA,
				99, 4.0, 1, Collections.emptyList());
	}

	@Override
	public int getCustoMana() {
		return 4;
	}

	@Override
	public int getAlcanceMaximo() {
		return 12;
	}

	@Override
	public boolean afetaInimigos() {
		return true;
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " invoca a SWORD OF HOPE!");

		int inspiracao = conjurador.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 0);
		Arma arma = conjurador.getArmaEquipada();
		double danoBaseArma = (arma != null) ? arma.getDanoBase() : 1;
		if (arma != null && conjurador.getRaca() != null) {
			Personagem alvoReferencia = alvos.isEmpty() ? null : alvos.get(0);
			danoBaseArma *= conjurador.getRaca().getMultiplicadorBonusDanoArma(conjurador, arma, alvoReferencia,
					estado, null);
		}

		double danoCalculado = (danoBaseArma + (inspiracao * 2)) * 4.0;

		if (Math.random() < conjurador.getTaxaCritica()) {
			danoCalculado *= (1.0 + conjurador.getDanoCritico());
			System.out.println(">>> SWORD OF HOPE CRÍTICA!");
		}
		int danoFinal = (int) danoCalculado;

		for (Personagem p : alvos) {
			manager.aplicarDanoAoAlvo(conjurador, p, danoFinal, false, TipoAcao.HABILIDADE, estado);
		}
	}
}
