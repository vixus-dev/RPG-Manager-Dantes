package br.com.dantesrpg.model.classes;

import br.com.dantesrpg.model.Classe;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.habilidades.classe.InvocarGolem;
import br.com.dantesrpg.model.habilidades.classe.InvocarGuardiao;
import br.com.dantesrpg.model.habilidades.classe.Purificar;
import br.com.dantesrpg.model.habilidades.classe.SacrificioSangrento;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Invocador extends Classe {

	private final String nome = "Invocador";
	private final String descricao = "ele traz ao campo de batalha as criaturas que lutam por ele, pode aplicar encantamentos em suas invocações, ou até sacrificá-las para causar dano";
	private final Map<Atributo, Integer> modificadores;
	private final List<Habilidade> habilidadesDaClasse;

	public Invocador() {
		this.modificadores = new HashMap<>();
		modificadores.put(Atributo.INSPIRACAO, 2);
		modificadores.put(Atributo.ENDURANCE, -2);

		// Habilidades da Classe
		this.habilidadesDaClasse = new ArrayList<>();
		this.habilidadesDaClasse.add(new InvocarGolem()); // Nível 1
		this.habilidadesDaClasse.add(new Purificar()); // Nível 3
		this.habilidadesDaClasse.add(new InvocarGuardiao()); // Nível 5
		this.habilidadesDaClasse.add(new SacrificioSangrento());// Nível 8
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
	public List<Habilidade> getHabilidades(Personagem personagem) { // Recebe o personagem
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