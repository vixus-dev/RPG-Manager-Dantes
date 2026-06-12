package br.com.dantesrpg.model.armas.unicas;

import br.com.dantesrpg.model.ArmaMelee;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.Raridade;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.enums.TipoEfeito;
import java.util.Map;

public class Murasame extends ArmaMelee {
	public Murasame() {
		super("Murasame", "Arma de uma mão",
				"é uma uchigatana forjada com microcanais que permitem aplicar toxinas na lâmina", Raridade.UNICO, 0,
				20, 1, Atributo.DESTREZA, 90, 2);
				this.setWielding(1); 
	}

	@Override
	public void onRollSuccess(Personagem ator, Personagem alvo, int rolagem, int dadoMax, double danoDoTick, EstadoCombate estado) {

		double percentualRolagem = (double) rolagem / dadoMax;

		if (percentualRolagem > 0.50) {
			int danoPorTick = Math.max(1, (int) (danoDoTick * 0.50));
			int duracaoTU = 200;
			int intervaloTick = 50;

			Efeito toxina = new Efeito("Toxina", TipoEfeito.DOT, duracaoTU, Map.of(), danoPorTick, intervaloTick);
			alvo.adicionarEfeito(toxina);
			alvo.recalcularAtributosEstatisticas();
		} else {
		}
	}
}