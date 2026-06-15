package br.com.dantesrpg.model;

import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.Raridade;

public class ImplementoMagico extends Arma {
	public ImplementoMagico(String nome, String categoria, String descricao, Raridade raridade, int valorMoedas,
			int danoBase, Atributo atributo, int custoTU, int alcance) {
		super(nome, categoria, descricao, raridade, valorMoedas, danoBase, 1, atributo, custoTU, alcance, "Magico", 0);
	}
}