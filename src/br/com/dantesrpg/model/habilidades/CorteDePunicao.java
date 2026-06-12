package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

public class CorteDePunicao extends Habilidade {

	public CorteDePunicao() {
		super("Corte de Punição", "Um golpe pesado que causa 140% de dano físico básico.", TipoHabilidade.ATIVA,
				0, // Custo de Mana
				105, // Custo de TU
				1, // Nível Necessário
				TipoAlvo.INDIVIDUAL,
				1.4, // Multiplicador de 140%
				1, // Ticks de Dano
				Collections.emptyList());
	}

	@Override
	public void executar(Personagem c, List<Personagem> a, EstadoCombate estado, CombatManager manager) {
		System.out.println(c.getNome() + " usa " + getNome() + "!");
	}
}
