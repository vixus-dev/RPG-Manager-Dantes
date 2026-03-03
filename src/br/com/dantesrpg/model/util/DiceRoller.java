
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
		return 30;
	}

	public static int rolarDado(int tipoDado) {
		return random.nextInt(tipoDado) + 1;
	}
}