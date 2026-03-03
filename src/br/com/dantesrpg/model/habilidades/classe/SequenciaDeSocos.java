package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

public class SequenciaDeSocos extends Habilidade {

	public SequenciaDeSocos() {
		super("Sequência de Socos", // Nome
				"Ataca 8 vezes, cada golpe causa 50% de dano.", // Descrição
				TipoHabilidade.ATIVA, 2, // Custo de Mana
				80, // Custo de TU
				3, // Nível Necessário
				TipoAlvo.INDIVIDUAL, 0.50, // Multiplicador de Dano (50%)
				8, // Ticks de Dano (8 acertos)
				Collections.emptyList());
	}

	@Override
	public void executar(Personagem c, List<Personagem> a, EstadoCombate estado, CombatManager manager) {
		System.out.println(c.getNome() + " usa " + getNome() + "!");
	}
}