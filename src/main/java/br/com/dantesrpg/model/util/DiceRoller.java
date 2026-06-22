
package br.com.dantesrpg.model.util;

import java.util.Random;

public class DiceRoller {
	private static final Random random = new Random();

	public static int getTipoDado(int valorAtributo) {
		if (valorAtributo <= 2)
			return 4;
		if (valorAtributo <= 4)
			return 6;
		if (valorAtributo <= 6)
			return 8;
		if (valorAtributo <= 8)
			return 10;
		if (valorAtributo <= 10)
			return 12;
		if (valorAtributo <= 12)
			return 14;
		if (valorAtributo <= 14)
			return 16;
		if (valorAtributo <= 16)
			return 18;
		if (valorAtributo <= 20)
			return 20;
		if (valorAtributo <= 24)
			return 22;
		if (valorAtributo <= 28)
			return 24;
		if (valorAtributo <= 32)
			return 26;
		if (valorAtributo <= 36)
			return 28;
		if (valorAtributo <= 40)
			return 30;
		
		int pontosAcima = valorAtributo - 40;
		int dadosAcima = (int) Math.ceil(pontosAcima / 4.0);
		return 30 + (dadosAcima * 2);
	}

	public static int rolarDado(int tipoDado) {
		return random.nextInt(tipoDado) + 1;
	}

	public static double getBonusRankPercentual(int valorAtributo) {
		if (valorAtributo >= 22) return 0.25; // Rank P
		if (valorAtributo >= 15) return 0.20; // Ranks S-, S, S+, SS, SS+, SSS, SSS+
		if (valorAtributo >= 12) return 0.15; // Ranks A-, A, A+
		if (valorAtributo >= 9) return 0.10;  // Ranks B-, B, B+
		return 0.0;
	}

	public static int aplicarBonusDeRank(int rolagemBruta, int valorAtributo) {
		int tipoDado = getTipoDado(valorAtributo);
		double bonusPercent = getBonusRankPercentual(valorAtributo);
		int bonus = (int) (tipoDado * bonusPercent);
		int resultadoFinal = rolagemBruta + bonus;
		return Math.min(resultadoFinal, tipoDado);
	}

	public static int aplicarBonusRankESorte(int rolagemBruta, int valorAtributo, int valorSorte) {
		int tipoDado = getTipoDado(valorAtributo);
		double bonusRankPercent = getBonusRankPercentual(valorAtributo);
		double bonusSortePercent = valorSorte * 0.01;
		int bonusRank = (int) (tipoDado * bonusRankPercent);
		int bonusSorte = (int) (tipoDado * bonusSortePercent);
		int resultadoFinal = rolagemBruta + bonusRank + bonusSorte;
		return Math.min(resultadoFinal, tipoDado);
	}
}