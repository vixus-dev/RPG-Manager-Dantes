package br.com.dantesrpg.model.util;

/**
 * Conversoes centralizadas entre pontos de armadura e reducao percentual.
 */
public final class ArmaduraUtils {

	public static final double REDUCAO_MAXIMA = 0.90;

	private ArmaduraUtils() {
	}

	public static double calcularReducaoPorPontos(double pontosArmadura) {
		double pontosValidos = Math.max(0.0, pontosArmadura);
		return pontosValidos / (100.0 + pontosValidos);
	}

	/**
	 * Converte um percentual legivel (por exemplo, 20 para 20%) em pontos de
	 * armadura usando a formula inversa da reducao atual.
	 */
	public static int calcularPontosParaReducaoPercentual(double reducaoPercentual) {
		double reducao = Math.max(0.0, Math.min(reducaoPercentual / 100.0, REDUCAO_MAXIMA));
		return (int) Math.round((100.0 * reducao) / (1.0 - reducao));
	}

	public static double calcularPontosAposPenetracao(double pontosArmadura, double penetracaoPercentual) {
		double penetracao = Math.max(0.0, Math.min(penetracaoPercentual, 1.0));
		return Math.max(0.0, pontosArmadura) * (1.0 - penetracao);
	}
}
