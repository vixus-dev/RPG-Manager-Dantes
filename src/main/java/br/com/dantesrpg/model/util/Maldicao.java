package br.com.dantesrpg.model.util;

/**
 * Representa uma Maldição aplicada sobre um Personagem.
 * 
 * Reduz a vida máxima do personagem por uma porcentagem (%) da sua vida máxima base.
 * Não é removida por cura, apenas por tempo (TU) ou purificação.
 */
public class Maldicao {

	private final String fonte;
	private double percentual; // ex: 0.25 para 25%
	private int duracaoTURestante; // Duração em TU restantes
	private final boolean persisteAposCombate;

	public Maldicao(String fonte, double percentual, int duracaoTU, boolean persisteAposCombate) {
		this.fonte = fonte;
		this.percentual = percentual;
		this.duracaoTURestante = duracaoTU;
		this.persisteAposCombate = persisteAposCombate;
	}

	public String getFonte() {
		return fonte;
	}

	public double getPercentual() {
		return percentual;
	}

	public void setPercentual(double percentual) {
		this.percentual = percentual;
	}

	public int getDuracaoTURestante() {
		return duracaoTURestante;
	}

	public void setDuracaoTURestante(int duracaoTURestante) {
		this.duracaoTURestante = duracaoTURestante;
	}

	public boolean persisteAposCombate() {
		return persisteAposCombate;
	}
}
