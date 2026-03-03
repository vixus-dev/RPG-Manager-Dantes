package br.com.dantesrpg.model;

import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.Raridade;

public class ArmaMelee extends Arma {
	public ArmaMelee(String nome, String categoria, String descricao, Raridade raridade, int valorMoedas, int danoBase,
			int ticksDeDano, Atributo atributo, int custoTU, int alcance) {
		super(nome, categoria, descricao, raridade, valorMoedas, danoBase, ticksDeDano, atributo, custoTU, alcance,
				"Melee", 0);
	}

	// Construtor de compatibilidade (padrão alcance 1)
	public ArmaMelee(String nome, String categoria, String descricao, Raridade raridade, int valorMoedas, int danoBase,
			int ticksDeDano, Atributo atributo, int custoTU) {
		this(nome, categoria, descricao, raridade, valorMoedas, danoBase, ticksDeDano, atributo, custoTU, 1);
	}
}