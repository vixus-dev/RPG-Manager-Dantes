package br.com.dantesrpg.model;

import java.util.List;
import java.util.Map;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.TipoAcao;

public abstract class Raça {
	public abstract String getNome();

	public abstract String getDescricaoPassiva();

	protected int currentStacks = 0;
	protected int maxStacks = 0;
	protected boolean isTransformed = false;

	public int getCurrentStacks() {
		return currentStacks;
	}

	public int getMaxStacks() {
		return maxStacks;
	}

	public boolean isTransformed() {
		return isTransformed;
	}

	public void onHpChanged(Personagem personagem, double hpAntigo, double hpNovo, EstadoCombate estado,
			CombatController controller) {
		// Padrao: nao faz nada
	}

	public void onTimeAdvanced(Personagem personagem, EstadoCombate estado, CombatController controller) {
		// Padrao: nao faz nada
	}

	public boolean onHpChangeAttempt(Personagem personagem, double vidaAntiga, double novaVida, EstadoCombate estado,
			CombatController controller) {
		return false;
	}

	public void onDamageTaken(Personagem personagem, Personagem atacante, double danoRecebido, EstadoCombate estado,
			CombatManager manager) {
		// Padrao: nao faz nada
	}

	public void onDamageDealt(Personagem personagem, Personagem alvo, double danoCausado, EstadoCombate estado,
			br.com.dantesrpg.controller.CombatController controller) {
		// Padrao: nao faz nada
	}

	public void onCriticalHit(Personagem personagem, Personagem alvo, EstadoCombate estado) {
		// Padrao: nao faz nada
	}

	public void onActionUsed(Personagem personagem, TipoAcao tipoAcaoAnterior, TipoAcao tipoAcaoAtual,
			EstadoCombate estado) {
		// Padrao: nao faz nada
	}

	public void onTurnStart(Personagem personagem, EstadoCombate estado) {
		// Padrao: nao faz nada
	}

	public void onEffectUpdate(Personagem personagem, Efeito efeito, boolean isAplicado) {
		// Padrao: nao faz nada
	}

	public Map<Atributo, Integer> getAttributeModifiers(Personagem personagem) {
		return null;
	}

	public Map<Atributo, Integer> getTemporaryAttributeModifiers(Personagem personagem) {
		return null;
	}

	public abstract List<Habilidade> getRacialAbilities(Personagem personagem);

	public double getBonusDanoPercentual(Personagem personagem) {
		return 0.0;
	}

	public double getReducaoTUPercentual(Personagem personagem) {
		return 0.0;
	}

	public double getReducaoHpMaximo(Personagem personagem) {
		return 0.0;
	}

	public double getMultiplicadorBonusDanoArma(Personagem personagem, Arma arma, Personagem alvo,
			EstadoCombate estado, AcaoMestreInput input) {
		return 1.0;
	}

	public double getMultiplicadorDanoRecebidoPreArmadura(Personagem personagem, Personagem atacante,
			EstadoCombate estado) {
		return 1.0;
	}

	public int getCustoTUExtra(Personagem personagem, Habilidade habilidade, TipoAcao tipoAcaoAtual) {
		return 0;
	}

	public boolean podeSeMover(Personagem personagem) {
		return true;
	}

	public boolean isImuneMovimentoForcado(Personagem personagem) {
		return false;
	}

	public double onCuraAttempt(Personagem personagem, double curaRecebida) {
		return curaRecebida;
	}
}
