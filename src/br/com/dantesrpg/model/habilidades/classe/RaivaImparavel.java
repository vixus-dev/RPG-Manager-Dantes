package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import br.com.dantesrpg.model.util.BarbaroUtils;

import java.util.Collections;
import java.util.List;

public class RaivaImparavel extends Habilidade {

	public RaivaImparavel() {
		super("Raiva Imparável",
				"Converte 10% da vida máxima em contrato e ganha +25% de dano por cada 5% ocupados, até 80%.",
				TipoHabilidade.ATIVA, 2, 50, 1, TipoAlvo.SI_MESMO, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		int stacksAntes = BarbaroUtils.getStacksContrato(conjurador);
		BarbaroUtils.acumularContrato(conjurador);

		if (BarbaroUtils.getStacksContrato(conjurador) == stacksAntes) {
			System.out.println(">>> " + conjurador.getNome() + " já atingiu o limite da Raiva Imparável.");
			return;
		}

		System.out.println(">>> " + conjurador.getNome() + " ativou Raiva Imparável: contrato em "
				+ BarbaroUtils.getPercentualContrato(conjurador) + "% e bônus de dano em +"
				+ BarbaroUtils.getBonusDanoPercentual(conjurador) + "%.");
	}
}
