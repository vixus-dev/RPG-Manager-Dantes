package br.com.dantesrpg.model.armas.unicas;

import br.com.dantesrpg.model.ArmaMelee;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.Raridade;

public class Yaweh extends ArmaMelee {
	public Yaweh() {
		super("Yaweh", "Grimorio",
				"Biblia sagrada google insira imagens.png fml vai bolsonaro",
				Raridade.UNICO, 0, 20, 1, Atributo.INSPIRACAO, 100, 3);
		this.setTipoAlvo(br.com.dantesrpg.model.enums.TipoAlvo.AREA_CIRCULAR);
		this.setTamanhoArea(1);
	}
}