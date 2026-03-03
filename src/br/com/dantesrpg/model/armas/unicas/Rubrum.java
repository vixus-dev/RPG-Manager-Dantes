package br.com.dantesrpg.model.armas.unicas;

import br.com.dantesrpg.model.ArmaMelee;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.Raridade;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.enums.TipoEfeito;
import java.util.Map;

// Stub (placeholder) para a arma da Ayame
public class Rubrum extends ArmaMelee {
	public Rubrum() {
		super("Rubrum", "Cajado",
				"Um cajado vermelho com propriedades magicas. pode alterar a aparencia do usuario e por incrivel que pareça é um otimo catalizador apesar do formato",
				Raridade.UNICO, 0, 8, 1, Atributo.INSPIRACAO, 105, 3);
		this.setTipoAlvo(br.com.dantesrpg.model.enums.TipoAlvo.AREA_CIRCULAR);
		this.setTamanhoArea(1);
	}
}