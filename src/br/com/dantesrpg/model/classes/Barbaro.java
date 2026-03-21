package br.com.dantesrpg.model.classes;

import br.com.dantesrpg.model.Classe;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.habilidades.classe.BalancoTemerario;
import br.com.dantesrpg.model.habilidades.classe.GolpeDevastador;
import br.com.dantesrpg.model.habilidades.classe.GritoDeGuerra;
import br.com.dantesrpg.model.habilidades.classe.RaivaImparavel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Barbaro extends Classe {

	private final String nome = "Barbaro";
	private final String descricao = "Um combatente brutal que troca a própria vida por dano e pressão constante.";
	private final Map<Atributo, Integer> modificadores;
	private final List<Habilidade> habilidadesDaClasse;

	public Barbaro() {
		this.modificadores = new HashMap<>();
		modificadores.put(Atributo.FORCA, 2);
		modificadores.put(Atributo.ENDURANCE, 1);
		modificadores.put(Atributo.INTELIGENCIA, -1);

		this.habilidadesDaClasse = new ArrayList<>();
		this.habilidadesDaClasse.add(new RaivaImparavel());
		this.habilidadesDaClasse.add(new BalancoTemerario());
		this.habilidadesDaClasse.add(new GritoDeGuerra());
		this.habilidadesDaClasse.add(new GolpeDevastador());
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
