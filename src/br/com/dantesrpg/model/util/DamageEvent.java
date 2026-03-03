package br.com.dantesrpg.model.util;

import java.util.function.Consumer;

public class DamageEvent {
	private double valorDano;
	private String label;
	private boolean isCritico;

	private Consumer<Double> onHitAction;

	public DamageEvent(double valorDano, String label, boolean isCritico, Consumer<Double> onHitAction) {
		this.valorDano = valorDano;
		this.label = label;
		this.isCritico = isCritico;
		this.onHitAction = onHitAction;
	}

	public double getValorDano() {
		return valorDano;
	}

	public String getLabel() {
		return label;
	}

	public boolean isCritico() {
		return isCritico;
	}

	public void aplicarEfeitos(double danoFinalReal) {
		if (onHitAction != null) {
			onHitAction.accept(danoFinalReal);
		}
	}
}