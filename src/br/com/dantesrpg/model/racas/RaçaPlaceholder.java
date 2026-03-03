package br.com.dantesrpg.model.racas;

import java.util.Collections;
import java.util.List;

import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.Raça;

public class RaçaPlaceholder extends Raça {
	@Override
	public String getNome() {
		return "Criatura";
	}

	@Override
	public String getDescricaoPassiva() {
		return "N/A";
	}

	@Override
	public List<Habilidade> getRacialAbilities(Personagem personagem) {
		return Collections.emptyList();
	}
}
