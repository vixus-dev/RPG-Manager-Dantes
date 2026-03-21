package br.com.dantesrpg.model.armas.unicas;

import br.com.dantesrpg.model.ArmaMelee;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.Raridade;
import br.com.dantesrpg.model.enums.TipoAlvo;

public class LaminasDoExterminio extends ArmaMelee {

	public LaminasDoExterminio() {
		super("Laminas Do Exterminio", "Adagas",
				"Um par de adagas preso aos antebraços de Arkos por uma maldição de Melchama. Elas não podem ser removidas.",
				Raridade.UNICO, 0, 15, 1, Atributo.FORCA, 105, 1);
		this.setTipoAlvo(TipoAlvo.CONE);
		this.setTamanhoArea(1);
	}

	@Override
	public int getAnguloCone() {
		return 90;
	}

	@Override
	public boolean hasAtaqueAlternativoBasico() {
		return true;
	}

	@Override
	public String getNomeAtaqueAlternativoBasico() {
		return "Golpe do Exterminio";
	}

	@Override
	public String getDescricaoAtaqueAlternativoBasico() {
		return "1.25x Dano, alcance 3";
	}

	@Override
	public double getMultiplicadorAtaqueAlternativoBasico() {
		return 1.25;
	}

	@Override
	public int getAlcanceAtaqueAlternativoBasico() {
		return 3;
	}

	@Override
	public TipoAlvo getTipoAlvoAtaqueAlternativoBasico() {
		return TipoAlvo.INDIVIDUAL;
	}
}
