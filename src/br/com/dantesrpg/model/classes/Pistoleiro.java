package br.com.dantesrpg.model.classes;

import br.com.dantesrpg.model.Classe;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.habilidades.classe.DeadEye;
import br.com.dantesrpg.model.habilidades.classe.DescarregarTambor;
import br.com.dantesrpg.model.habilidades.classe.GatilhoVeloz;
import br.com.dantesrpg.model.habilidades.classe.JusticaDourada;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pistoleiro extends Classe {

	private final String nome = "Pistoleiro";
	private final String descricao = "Um atirador calculista que transforma precisão, reflexo e fortuna em disparos letais.";
	private final Map<Atributo, Integer> modificadores;
	private final List<Habilidade> habilidadesDaClasse;

	public Pistoleiro() {
		this.modificadores = new HashMap<>();
		modificadores.put(Atributo.SAGACIDADE, 1);
		modificadores.put(Atributo.PERCEPCAO, 1);

		this.habilidadesDaClasse = new ArrayList<>();
		this.habilidadesDaClasse.add(new DeadEye());
		this.habilidadesDaClasse.add(new GatilhoVeloz());
		this.habilidadesDaClasse.add(new DescarregarTambor());
		this.habilidadesDaClasse.add(new JusticaDourada());
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
