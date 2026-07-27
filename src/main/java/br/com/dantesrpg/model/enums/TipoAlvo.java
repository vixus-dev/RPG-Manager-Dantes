package br.com.dantesrpg.model.enums;

public enum TipoAlvo {
	INDIVIDUAL, AREA, SI_MESMO, EQUIPE, MULTIPLOS, MULTI_AOE, AREA_QUADRADA, AREA_CIRCULAR, CONE, LINHA;

	public boolean isFormatoAreaComEpicentro() {
		return this == AREA_QUADRADA || this == AREA_CIRCULAR || this == CONE || this == LINHA;
	}
}
