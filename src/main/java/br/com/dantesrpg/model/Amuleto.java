package br.com.dantesrpg.model;

import br.com.dantesrpg.model.enums.Atributo;
import java.util.Map;
import java.util.HashMap;

public class Amuleto extends Item {
	private int armaduraBonus;
	private Map<Atributo, Integer> modificadoresDeAtributo;

	private Map<String, Double> modificadoresStatus;

	public Amuleto(String nome, String descricao, int valor, int armaduraBonus, Map<Atributo, Integer> modAtributos,
			Map<String, Double> modStatus) {
		super(nome, descricao, valor, false);
		this.armaduraBonus = armaduraBonus;
		this.modificadoresDeAtributo = modAtributos;
		this.modificadoresStatus = modStatus != null ? modStatus : new HashMap<>();
	}

	public int getArmaduraBonus() {
		return (int) Math.round(armaduraBonus * getMultiplicadorOverclock());
	}

	public int getArmaduraBonusOriginal() {
		return armaduraBonus;
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