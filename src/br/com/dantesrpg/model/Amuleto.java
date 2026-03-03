package br.com.dantesrpg.model;

import br.com.dantesrpg.model.enums.Atributo;
import java.util.Map;
import java.util.HashMap;

public class Amuleto extends Item {
	private int armaduraBonus;
	private Map<Atributo, Integer> modificadoresDeAtributo;

	private Map<String, Double> modificadoresStatus;

	// Atualize o construtor
	public Amuleto(String nome, String descricao, int valor, int armaduraBonus, Map<Atributo, Integer> modAtributos,
			Map<String, Double> modStatus) {
		super(nome, descricao, valor, false);
		this.armaduraBonus = armaduraBonus;
		this.modificadoresDeAtributo = modAtributos;
		this.modificadoresStatus = modStatus != null ? modStatus : new HashMap<>();
	}

	public int getArmaduraBonus() {
		return armaduraBonus;
	}

	public Map<Atributo, Integer> getModificadoresDeAtributo() {
		return modificadoresDeAtributo;
	}

	public Map<String, Double> getModificadoresStatus() {
		return modificadoresStatus;
	}

	@Override
	public String getTipo() {
		return this.getNome();
	}
}