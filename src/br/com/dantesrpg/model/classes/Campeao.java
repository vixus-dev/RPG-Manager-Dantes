package br.com.dantesrpg.model.classes;

import br.com.dantesrpg.model.Classe;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.habilidades.classe.AutoGuarda;
import br.com.dantesrpg.model.habilidades.classe.BashStrike;
import br.com.dantesrpg.model.habilidades.classe.DrenagemDeEfeitos;
import br.com.dantesrpg.model.habilidades.classe.ImpactoVingativo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Campeao extends Classe {

	private final String nome = "Campeão";
	private final String descricao = "Um combatente resiliente que absorve dano e revida com força proporcional ao sofrimento.";
	private final Map<Atributo, Integer> modificadores;
	private final List<Habilidade> habilidadesDaClasse;

	public Campeao() {
		this.modificadores = new HashMap<>();
		modificadores.put(Atributo.FORCA, 2);

		this.habilidadesDaClasse = new ArrayList<>();
		this.habilidadesDaClasse.add(new BashStrike());         // Nível 1
		this.habilidadesDaClasse.add(new AutoGuarda());         // Nível 3
		this.habilidadesDaClasse.add(new ImpactoVingativo());   // Nível 5
		this.habilidadesDaClasse.add(new DrenagemDeEfeitos());  // Nível 8
	}

	@Override
	public String getNome() {
		return this.nome;
	}

	@Override
	public String getDescricao() {
		return this.descricao;
	}

	@Override
	public Map<Atributo, Integer> getModificadoresDeAtributo() {
		return this.modificadores;
	}

	@Override
	public List<Habilidade> getHabilidades(Personagem personagem) {
		List<Habilidade> habilidadesDesbloqueadas = new ArrayList<>();
		int nivelAtual = personagem.getNivel();
		for (Habilidade hab : this.habilidadesDaClasse) {
			if (nivelAtual >= hab.getNivelNecessario()) {
				habilidadesDesbloqueadas.add(hab);
			}
		}
		return habilidadesDesbloqueadas;
	}
}
