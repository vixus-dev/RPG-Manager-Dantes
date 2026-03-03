package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

public class Iaijutsu extends Habilidade {

	public Iaijutsu() {
		super("Iaijutsu", "Um saque rápido e mortal que causa dano massivo baseado na Destreza.", TipoHabilidade.ATIVA,
				5, // Custo de Mana
				150, // Custo de TU
				1, // Nível Necessário
				TipoAlvo.INDIVIDUAL, 5.0, // Multiplicador de 500%
				1, Collections.emptyList());
	}

	@Override
	public void executar(Personagem c, List<Personagem> a, EstadoCombate estado, CombatManager manager) {
		System.out.println(c.getNome() + " usa " + getNome() + "!");
		// O corpo fica vazio, pois o dano é tratado pelo CombatManager
	}
}