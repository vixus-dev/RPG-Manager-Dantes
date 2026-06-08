package br.com.dantesrpg.model;

import br.com.dantesrpg.model.enums.Atributo;
import java.util.Map;
import java.util.HashMap;

public class Armadura extends Item {
	private int armaduraBase;
	private Map<Atributo, Integer> modificadoresDeAtributo;

	private Map<String, Double> modificadoresStatus;

	public Armadura(String nome, String descricao, int valor, int armaduraBase, Map<Atributo, Integer> modAtributos,
			Map<String, Double> modStatus) {
		super(nome, descricao, valor, false);
		this.armaduraBase = armaduraBase;
		this.modificadoresDeAtributo = modAtributos;
		this.modificadoresStatus = modStatus != null ? modStatus : new HashMap<>();
	}

	public int getArmaduraBase() {
		return (int) Math.round(armaduraBase * getMultiplicadorOverclock());
	}

	public int getArmaduraBaseOriginal() {
		return armaduraBase;
	}

	public Map<Atributo, Integer> getModificadoresDeAtributo() {
		if (modificadoresDeAtributo == null || getGrauOverclock() == 0) return modificadoresDeAtributo;
		Map<Atributo, Integer> resultado = new HashMap<>();
		double mult = getMultiplicadorOverclock();
		for (Map.Entry<Atributo, Integer> e : modificadoresDeAtributo.entrySet()) {
			resultado.put(e.getKey(), (int) Math.round(e.getValue() * mult));
		}
		return resultado;
	}

	public Map<String, Double> getModificadoresStatus() {
		if (modificadoresStatus == null || getGrauOverclock() == 0) return modificadoresStatus;
		Map<String, Double> resultado = new HashMap<>();
		double mult = getMultiplicadorOverclock();
		for (Map.Entry<String, Double> e : modificadoresStatus.entrySet()) {
			resultado.put(e.getKey(), e.getValue() * mult);
		}
		return resultado;
	}

	@Override
	public String getTipo() {
		return this.getNome();
	}
}