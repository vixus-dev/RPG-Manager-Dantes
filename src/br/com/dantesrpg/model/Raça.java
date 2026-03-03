
package br.com.dantesrpg.model;

import java.util.Map;
import java.util.List;

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
		// Padrão: não faz nada
	}

	public void onTimeAdvanced(Personagem personagem, EstadoCombate estado, CombatController controller) {
		// Padrão: não faz nada
	}

	public boolean onHpChangeAttempt(Personagem personagem, double vidaAntiga, double novaVida, EstadoCombate estado,
			CombatController controller) {
		return false;
	}

	public void onDamageTaken(Personagem personagem, Personagem atacante, double danoRecebido, EstadoCombate estado,
			CombatManager manager) {
		// Padrão: não faz nada
	}

	public void onDamageDealt(Personagem personagem, Personagem alvo, double danoCausado, EstadoCombate estado,
			br.com.dantesrpg.controller.CombatController controller) {
		// Padrão: não faz nada
	}

	/** Chamado quando o personagem acerta um crítico. */
	public void onCriticalHit(Personagem personagem, Personagem alvo, EstadoCombate estado) {
		// Ex: Half-breeds ganham stacks, Marionette inicia cascata
	}

	/**
	 * Chamado quando o personagem usa uma ação (ataque, habilidade, item, etc.).
	 */
	public void onActionUsed(Personagem personagem, TipoAcao tipoAcaoAnterior, TipoAcao tipoAcaoAtual,
			EstadoCombate estado) {
		// Padrão: não faz nada
	}

	/** Chamado no início do turno do personagem. */
	public void onTurnStart(Personagem personagem, EstadoCombate estado) {
		// Ex: Drenar Fúria/Devil Trigger/Benevolência
	}

	/** Chamado quando um efeito expira ou é aplicado/removido (pode ser útil). */
	public void onEffectUpdate(Personagem personagem, Efeito efeito, boolean isAplicado) {
		// Ex: Alguma raça pode reagir a certos status?
	}

	// --- Métodos para Modificadores (Opcional, mas útil) ---

	/** Retorna modificadores de atributo aplicados pela raça (Ex: Orc). */
	public Map<Atributo, Integer> getAttributeModifiers(Personagem personagem) {
		return null; // Padrão: sem modificadores
	}

	/**
	 * Retorna modificadores temporários de atributo (Ex: Transformação Lobisomem).
	 */
	public Map<Atributo, Integer> getTemporaryAttributeModifiers(Personagem personagem) {
		return null; // Padrão: sem modificadores temporários
	}

	public abstract List<Habilidade> getRacialAbilities(Personagem personagem);

	/** Hook para bônus de dano percentual (Ex: Humano) */
	public double getBonusDanoPercentual(Personagem personagem) {
		return 0.0; // Padrão: 0
	}

	/** Hook para redução de custo de TU percentual (Ex: Humano) */
	public double getReducaoTUPercentual(Personagem personagem) {
		return 0.0; // Padrão: 0
	}

	/** Hook para redução de HP máximo (Ex: Contrato de Vida Humano) */
	public double getReducaoHpMaximo(Personagem personagem) {
		return 0.0; // Padrão: 0
	}

	public double onCuraAttempt(Personagem personagem, double curaRecebida) {
		return curaRecebida; // Padrão: raça não interfere
	}

}