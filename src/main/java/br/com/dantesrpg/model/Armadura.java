package br.com.dantesrpg.model;

import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.Raridade;
import java.util.Map;
import java.util.HashMap;

public class Armadura extends Item {
	private int armaduraBase;
	private Raridade raridade;
	private Map<Atributo, Integer> modificadoresDeAtributo;

	private Map<String, Double> modificadoresStatus;

	private String nomeEfeitoOnDamageTaken;
	private double chanceEfeitoOnDamageTaken;
	private String alvoEfeitoOnDamageTaken = "ATACANTE"; // "ATACANTE" ou "USUARIO"

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

	public Raridade getRaridade() {
		return raridade;
	}

	public void setRaridade(Raridade raridade) {
		this.raridade = raridade;
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

	public String getNomeEfeitoOnDamageTaken() {
		return nomeEfeitoOnDamageTaken;
	}

	public void setNomeEfeitoOnDamageTaken(String nomeEfeitoOnDamageTaken) {
		this.nomeEfeitoOnDamageTaken = nomeEfeitoOnDamageTaken;
	}

	public double getChanceEfeitoOnDamageTaken() {
		return chanceEfeitoOnDamageTaken;
	}

	public void setChanceEfeitoOnDamageTaken(double chanceEfeitoOnDamageTaken) {
		this.chanceEfeitoOnDamageTaken = chanceEfeitoOnDamageTaken;
	}

	public String getAlvoEfeitoOnDamageTaken() {
		return alvoEfeitoOnDamageTaken;
	}

	public void setAlvoEfeitoOnDamageTaken(String alvoEfeitoOnDamageTaken) {
		this.alvoEfeitoOnDamageTaken = alvoEfeitoOnDamageTaken;
	}

	public void onDamageTaken(Personagem alvo, Personagem atacante, double dano, EstadoCombate estado, br.com.dantesrpg.controller.CombatController controller) {
		if (nomeEfeitoOnDamageTaken != null && !nomeEfeitoOnDamageTaken.isEmpty()) {
			if (Math.random() <= chanceEfeitoOnDamageTaken) {
				Personagem target = "USUARIO".equalsIgnoreCase(alvoEfeitoOnDamageTaken) ? alvo : atacante;
				if (target != null && target.isVivo()) {
					int danoDaSource = Math.max(1, (int) dano);
					// Usando 200 TU como duração padrão para buffs/debuffs gerais
					Efeito efeito = br.com.dantesrpg.model.util.EffectFactory.criarEfeito(nomeEfeitoOnDamageTaken, 200, danoDaSource);
					if (efeito != null) {
						efeito.setStacks(1);
						if (estado != null && estado.getCombatManager() != null) {
							estado.getCombatManager().getEffectProcessor().aplicarEfeito(target, efeito);
						} else {
							target.adicionarEfeito(efeito);
						}
					}
				}
			}
		}
	}
}
