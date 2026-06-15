package br.com.dantesrpg.model.classes;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.habilidades.classe.*;
import java.util.*;

public class Paladino extends Classe {
	private final String nome = "Paladino";
	private final String descricao = "Guerreiro sagrado focado em buffs e proteção.";
	private final Map<Atributo, Integer> modificadores;
	private final List<Habilidade> habilidadesDaClasse;

	public Paladino() {
		this.modificadores = new HashMap<>();
		modificadores.put(Atributo.FORCA, 1);
		modificadores.put(Atributo.ENDURANCE, 1);
		modificadores.put(Atributo.TOPOR, 1);
		modificadores.put(Atributo.SORTE, -1);

		this.habilidadesDaClasse = new ArrayList<>();
		this.habilidadesDaClasse.add(new Rezar());
		this.habilidadesDaClasse.add(new Purificar());
		this.habilidadesDaClasse.add(new Bencao());
		this.habilidadesDaClasse.add(new SwordOfHope());
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