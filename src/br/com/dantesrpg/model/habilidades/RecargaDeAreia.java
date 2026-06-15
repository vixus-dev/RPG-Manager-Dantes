package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

public class RecargaDeAreia extends Habilidade {

	public RecargaDeAreia() {
		super("Recarga de Areia", "Recarrega instantaneamente a munição da arma ativa.", TipoHabilidade.ATIVA,
				2, // Custo de Mana
				80, // Custo de TU
				1, // Nível Necessário
				TipoAlvo.SI_MESMO,
				0.0, // Multiplicador de Dano
				0, // Ticks de Dano
				Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa " + getNome() + "!");
		Arma arma = conjurador.getArmaEquipada();
		if (arma != null) {
			arma.recarregar();
		}
	}
}
