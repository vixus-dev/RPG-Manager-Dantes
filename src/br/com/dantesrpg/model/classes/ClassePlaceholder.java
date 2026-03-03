package br.com.dantesrpg.model.classes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.dantesrpg.model.Classe;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;

public class ClassePlaceholder extends Classe {
	@Override
	public String getNome() {
		return "???";
	}

	@Override
	public String getDescricao() {
		return "N/A";
	}

	@Override
	public Map<Atributo, Integer> getModificadoresDeAtributo() {
		return new HashMap<>();
	}

	@Override
	public List<Habilidade> getHabilidades(Personagem personagem) {
		return null;
	}
}