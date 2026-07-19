package br.com.dantesrpg.model.enums;

public enum AndarCampanha {
	NULO(0),
	ANDAR_1(1),
	ANDAR_2(2),
	ANDAR_3(3),
	ANDAR_4(4),
	ANDAR_5(5),
	ANDAR_6(6),
	ANDAR_7(7),
	ANDAR_8(8),
	ANDAR_9(9);

	private final int numero;

	AndarCampanha(int numero) {
		this.numero = numero;
	}

	public int getNumero() {
		return numero;
	}

	public static AndarCampanha fromId(String id) {
		if (id == null || id.isBlank()) {
			return NULO;
		}
		try {
			return valueOf(id.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			return NULO;
		}
	}
}
