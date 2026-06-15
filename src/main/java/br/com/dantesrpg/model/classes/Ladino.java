package br.com.dantesrpg.model.classes;

import br.com.dantesrpg.model.Classe;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.habilidades.classe.PassoEscuro;
import br.com.dantesrpg.model.habilidades.classe.SussurroSombrio;
import br.com.dantesrpg.model.habilidades.classe.DestruidorDeGuardioes;
import br.com.dantesrpg.model.habilidades.classe.InvestidaTriunfante;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Ladino extends Classe {

	private final String nome = "Ladino";
	private final String descricao = "Um especialista em furtividade e golpes precisos.";
	private final Map<Atributo, Integer> modificadores;
	private final List<Habilidade> habilidadesDaClasse;

	public Ladino() {
		this.modificadores = new HashMap<>();
		// Defina os modificadores (ex: +2 DES, +1 SAG)
		modificadores.put(Atributo.DESTREZA, 2);
		modificadores.put(Atributo.SAGACIDADE, 1);

		this.habilidadesDaClasse = new ArrayList<>();
		this.habilidadesDaClasse.add(new PassoEscuro());
		this.habilidadesDaClasse.add(new DestruidorDeGuardioes());
		this.habilidadesDaClasse.add(new InvestidaTriunfante());
		this.habilidadesDaClasse.add(new SussurroSombrio());
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