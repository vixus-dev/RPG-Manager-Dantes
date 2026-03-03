package br.com.dantesrpg.model;

import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.Raridade;

public class ArmaRanged extends Arma {
	public ArmaRanged(String nome, String categoria, String descricao, Raridade raridade, int valorMoedas, int danoBase,
			int ticksDeDano, Atributo atributo, int custoTU, int alcance, int municaoMaxima) {
		super(nome, categoria, descricao, raridade, valorMoedas, danoBase, ticksDeDano, atributo, custoTU, alcance,
				null, "Ranged", municaoMaxima);
	}

	// Construtor legado
	public ArmaRanged(String nome, String categoria, String descricao, Raridade raridade, int valorMoedas, int danoBase,
			int ticksDeDano, Atributo atributo, int custoTU) {
		this(nome, categoria, descricao, raridade, valorMoedas, danoBase, ticksDeDano, atributo, custoTU, 5, 6);
	}
}