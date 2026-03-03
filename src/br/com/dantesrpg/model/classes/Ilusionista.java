package br.com.dantesrpg.model.classes;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.habilidades.classe.*;
import java.util.*;

public class Ilusionista extends Classe {
	private final String nome = "Ilusionista";
	private final String descricao = "Mestre dos enganos e clones sombrios.";
	private final Map<Atributo, Integer> modificadores;
	private final List<Habilidade> habilidadesDaClasse;

	public Ilusionista() {
		this.modificadores = new HashMap<>();
		modificadores.put(Atributo.INSPIRACAO, 1);
		modificadores.put(Atributo.PERCEPCAO, 1);

		this.habilidadesDaClasse = new ArrayList<>();
		this.habilidadesDaClasse.add(new Ilusao());
		this.habilidadesDaClasse.add(new TrocaIlusoria());
		this.habilidadesDaClasse.add(new Borderline());
		this.habilidadesDaClasse.add(new Arise());
	}

	@Override
	public String getNome() {
		return nome;
	}

	@Override
	public String getDescricao() {
		return descricao;
	}

	@Override
	public Map<Atributo, Integer> getModificadoresDeAtributo() {
		return modificadores;
	}

	@Override
	public List<Habilidade> getHabilidades(Personagem personagem) {
		List<Habilidade> desbloqueadas = new ArrayList<>();
		for (Habilidade h : habilidadesDaClasse) {
			if (personagem.getNivel() >= h.getNivelNecessario())
				desbloqueadas.add(h);
		}
		return desbloqueadas;
	}
}