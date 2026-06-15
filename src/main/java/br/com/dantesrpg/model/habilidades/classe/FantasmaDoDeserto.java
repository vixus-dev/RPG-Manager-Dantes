package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

public class FantasmaDoDeserto extends Habilidade {

	public FantasmaDoDeserto() {
		super("Fantasma do Deserto", "Marca um inimigo. Acertar o inimigo marcado joga uma moeda...",
				TipoHabilidade.ATIVA, 6, 220, 8, TipoAlvo.INDIVIDUAL, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa " + getNome() + "!");

		if (alvos == null || alvos.isEmpty()) {
			System.out.println(">>> ...mas falhou em selecionar um alvo.");
			return;
		}

		Personagem alvo = alvos.get(0); // Pega o primeiro alvo

		Efeito debuffMarca = new Efeito("Marca do Deserto", TipoEfeito.DEBUFF, 800,null, 0, 0);

		alvo.adicionarEfeito(debuffMarca);
		System.out.println(">>> " + alvo.getNome() + " está sob o efeito [Marca do Deserto].");

		alvo.recalcularAtributosEstatisticas();
	}
}