package br.com.dantesrpg.model.classes;

import br.com.dantesrpg.model.Classe;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.habilidades.classe.SintetizarPocao;
import br.com.dantesrpg.model.habilidades.classe.AprimorarPocao;
import br.com.dantesrpg.model.habilidades.classe.RestricaoCelestial;
import br.com.dantesrpg.model.habilidades.classe.ReversaoDeFeitico;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Alquimista extends Classe {

	private final String nome = "Alquimista";
	private final String descricao = "Usuários de magia de médio alcance são responsáveis por transmutação de materiais e na criação de poções. tendem a possuir pouca durabilidade.";
	private final Map<Atributo, Integer> modificadores;
	private final List<Habilidade> habilidadesDaClasse;

	public Alquimista() {
		// Modificadores: +1 ISpiração, +1 INTeligencia, +1 mana, -2 EN, -1 DES 
		this.modificadores = new HashMap<>();
		modificadores.put(Atributo.INSPIRACAO, 1);
		modificadores.put(Atributo.INTELIGENCIA, 1);
		modificadores.put(Atributo.ENDURANCE, -2);
		modificadores.put(Atributo.DESTREZA, -1);

		// Habilidades da Classe
		this.habilidadesDaClasse = new ArrayList<>();
		this.habilidadesDaClasse.add(new SintetizarPocao()); // Nível 1
		this.habilidadesDaClasse.add(new AprimorarPocao()); // Nível 3
		this.habilidadesDaClasse.add(new ReversaoDeFeitico()); // Nível 5
		this.habilidadesDaClasse.add(new RestricaoCelestial());// Nível 8
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