package br.com.dantesrpg.model;

import br.com.dantesrpg.model.enums.Atributo;
import java.util.Map;
import java.util.HashMap;

public class Armadura extends Item {
	private int armaduraBase;
	private Map<Atributo, Integer> modificadoresDeAtributo;

	// Mapa para coisas como "HP_MAXIMO", "REDUCAO_DANO", etc.
	private Map<String, Double> modificadoresStatus;

	// Atualize o construtor
	public Armadura(String nome, String descricao, int valor, int armaduraBase, Map<Atributo, Integer> modAtributos,
			Map<String, Double> modStatus) {
		super(nome, descricao, valor, false);
		this.armaduraBase = armaduraBase;
		this.modificadoresDeAtributo = modAtributos;
		this.modificadoresStatus = modStatus != null ? modStatus : new HashMap<>();
	}

	public int getArmaduraBase() {
		return armaduraBase;
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