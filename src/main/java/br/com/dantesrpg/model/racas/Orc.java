package br.com.dantesrpg.model.racas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.Raça;
import br.com.dantesrpg.model.enums.Atributo;

public class Orc extends Raça {

	@Override
	public String getNome() {
		return "Orc";
	}

	@Override
	public String getDescricaoPassiva() {
		return "Ganha mais vida e ataque físico, porém perde intelecto, o ápice da força física. Inspiração e Inteligência fixos em 1. Pontos investidos são redirecionados para Força e Endurance.";
	}

	@Override
	public Map<Atributo, Integer> getAttributeModifiers(Personagem personagem) {
		Map<Atributo, Integer> mods = new HashMap<>();
		int baseInt = personagem.getAtributosBase().getOrDefault(Atributo.INTELIGENCIA, 1);
		int baseIns = personagem.getAtributosBase().getOrDefault(Atributo.INSPIRACAO, 1);

		if (baseInt > 1) {
			mods.put(Atributo.INTELIGENCIA, -(baseInt - 1));
			mods.put(Atributo.ENDURANCE, (baseInt - 1));
		}
		if (baseIns > 1) {
			mods.put(Atributo.INSPIRACAO, -(baseIns - 1));
			mods.put(Atributo.FORCA, (baseIns - 1));
		}
		return mods;
	}

	@Override
	public List<Habilidade> getRacialAbilities(Personagem personagem) {
		return new ArrayList<>();
	}

}
