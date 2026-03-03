package br.com.dantesrpg.model.classes;

import br.com.dantesrpg.model.Classe;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.habilidades.classe.Cacada;
import br.com.dantesrpg.model.habilidades.classe.FantasmaDoDeserto;
import br.com.dantesrpg.model.habilidades.classe.QuickShot;
import br.com.dantesrpg.model.habilidades.classe.Trocado;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MestreDasBalas extends Classe {

	private final String nome = "Mestre das Balas";
	private final String descricao = "Um atirador ágil que usa truques e precisão.";
	private final Map<Atributo, Integer> modificadores;
	private final List<Habilidade> habilidadesDaClasse;

	public MestreDasBalas() {
		this.modificadores = new HashMap<>();
		modificadores.put(Atributo.SAGACIDADE, 2);

		this.habilidadesDaClasse = new ArrayList<>();
		this.habilidadesDaClasse.add(new QuickShot());
		this.habilidadesDaClasse.add(new Trocado());
		this.habilidadesDaClasse.add(new Cacada());
		this.habilidadesDaClasse.add(new FantasmaDoDeserto());
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