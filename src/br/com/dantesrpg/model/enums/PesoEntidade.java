package br.com.dantesrpg.model.enums;

/**
 * Define as categorias de peso de uma entidade para cálculo de empuxo (knockback).
 *
 * O fatorResistencia determina o divisor aplicado sobre a força de empuxo do ataque.
 * Quanto maior o fator, menor a distância que a entidade será arremessada.
 *
 * Tabela de referência base (força de empuxo = 10):
 *   MUITO_LEVE  → 10 / 1.0 = 10 tiles (max 5 com cap)
 *   LEVE        → 10 / 2.0 = 5 tiles
 *   MEDIO_PADRAO→ 10 / 3.0 = 3 tiles
 *   PESADO      → 10 / 5.0 = 2 tiles
 *   MUITO_PESADO→ 10 / 8.0 = 1 tile
 *   IMOVEL      → sempre 0 (bosses, objetos fixos)
 */
public enum PesoEntidade {

	MUITO_LEVE(1.0, "muito leve"),
	LEVE(2.0, "leve"),
	MEDIO_PADRAO(3.0, "medio_padrao"),
	PESADO(5.0, "pesado"),
	MUITO_PESADO(8.0, "muito pesado"),
	IMOVEL(Double.MAX_VALUE, "imovel");

	private final double fatorResistencia;
	private final String jsonId;

	PesoEntidade(double fatorResistencia, String jsonId) {
		this.fatorResistencia = fatorResistencia;
		this.jsonId = jsonId;
	}

	/** Divisor aplicado sobre a força de empuxo do ataque. */
	public double getFatorResistencia() {
		return fatorResistencia;
	}

	/** Identificador usado no bestiario.json (campo "peso"). */
	public String getJsonId() {
		return jsonId;
	}

	/**
	 * Converte a string do JSON para o enum correspondente.
	 * Retorna MEDIO_PADRAO como fallback seguro se o valor não for reconhecido.
	 */
	public static PesoEntidade fromJsonId(String id) {
		if (id == null) return MEDIO_PADRAO;
		String normalizado = id.trim().toLowerCase();
		for (PesoEntidade p : values()) {
			if (p.jsonId.equals(normalizado)) {
				return p;
			}
		}
		return MEDIO_PADRAO;
	}
}
