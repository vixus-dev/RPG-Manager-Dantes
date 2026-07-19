package br.com.dantesrpg.model;

import br.com.dantesrpg.model.enums.AndarCampanha;

public record EstadoAndarParty(AndarCampanha andar, int estadoVisual) {

	public EstadoAndarParty {
		andar = andar != null ? andar : AndarCampanha.NULO;
		estadoVisual = andar == AndarCampanha.NULO ? 0 : Math.max(1, estadoVisual);
	}

	public static EstadoAndarParty nulo() {
		return new EstadoAndarParty(AndarCampanha.NULO, 0);
	}

	public String getChave() {
		if (andar == AndarCampanha.NULO) {
			return "NULO";
		}
		return String.format("%02d-%d", andar.getNumero(), estadoVisual);
	}
}
