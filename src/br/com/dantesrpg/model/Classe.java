package br.com.dantesrpg.model;

import br.com.dantesrpg.model.enums.Atributo;
import java.util.List;
import java.util.Map;

public abstract class Classe {
	public abstract String getNome();

	public abstract String getDescricao();

	/**
	 * Retorna os bônus e penalidades de atributo concedidos pela classe. Ex:
	 * {FORCA: +2, ENDURANCE: -1}
	 */
	public abstract Map<Atributo, Integer> getModificadoresDeAtributo();

	public abstract List<Habilidade> getHabilidades(Personagem personagem);
}