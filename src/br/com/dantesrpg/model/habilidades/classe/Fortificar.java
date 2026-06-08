package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Fortificar extends Habilidade {
	public Fortificar() {
		super("Fortificar", "Ganha um escudo de pedra (10 + 5xIS de armadura) por 1000TU.", TipoHabilidade.ATIVA,
				1, 100, 1, TipoAlvo.SI_MESMO, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		int inspiracao = conjurador.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 0);
		double escudo = 10 + (5.0 * inspiracao);

		System.out.println(conjurador.getNome() + " usa Fortificar! Escudo de " + (int) escudo + " de armadura.");

		Map<String, Double> mods = new HashMap<>();
		mods.put("ARMADURA_TOTAL", escudo);

		Efeito fortificacao = new Efeito("Fortificar", TipoEfeito.BUFF, 1000, mods, 0, 0);
		conjurador.adicionarEfeito(fortificacao);

		conjurador.recalcularAtributosEstatisticas();
		System.out.println(">>> Efeito [Fortificar] aplicado: +" + (int) escudo + " Armadura por 1000TU.");
	}
}
