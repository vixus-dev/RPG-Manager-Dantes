package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

public class Cura_Tier1 extends Habilidade {
	public Cura_Tier1() {
		super("Cura I", "Cura 20% da vida Máxima", TipoHabilidade.ATIVA, 2, 100, 1, // Custo de mana de 2
				TipoAlvo.SI_MESMO, 0.0, // Multiplicador de dano (não causa dano)
				0, // Ticks de dano
				Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa Cura Tier 1");

		double valorCura = (int) (conjurador.getVidaMaxima() * 0.20);
		conjurador.setVidaAtual(conjurador.getVidaAtual() + valorCura);

		conjurador.recalcularAtributosEstatisticas();
	}
}