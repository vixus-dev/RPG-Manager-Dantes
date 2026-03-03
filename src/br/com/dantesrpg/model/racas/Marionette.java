package br.com.dantesrpg.model.racas;

import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.Raça;

import java.util.Collections;
import java.util.List;

public class Marionette extends Raça {

	private final String nome = "Marionette";
	private final String descricaoPassiva = "Crítico em Cascata: Acertos críticos causam um ataque adicional (50% do dano). Repete se o adicional for crítico (limite de 7). +25% Taxa Crítica.";

	public Marionette() {
		this.maxStacks = 0;
		this.currentStacks = 0;
	}

	@Override
	public String getNome() {
		return nome;
	}

	@Override
	public String getDescricaoPassiva() {
		return descricaoPassiva;
	}

	@Override
	public List<Habilidade> getRacialAbilities(Personagem personagem) {
		return Collections.emptyList(); // Sem habilidades ativas
	}

}