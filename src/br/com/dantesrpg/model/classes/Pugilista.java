package br.com.dantesrpg.model.classes;

import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.Classe;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.habilidades.classe.Combo;
import br.com.dantesrpg.model.habilidades.classe.QuebraOssos;
import br.com.dantesrpg.model.habilidades.classe.SequenciaDeSocos;
import br.com.dantesrpg.model.habilidades.classe.SocoSerio;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class Pugilista extends Classe {

	private final String nome = "Pugilista";
	private final String descricao = "Um mestre do combate corpo a corpo que usa os próprios punhos como armas mortais.";
	private final Map<Atributo, Integer> modificadores;
	private final List<Habilidade> habilidadesDaClasse;

	public Pugilista() {
		this.modificadores = new HashMap<>();
		// Bônus do Pugilista: +2 FOR, +1 DES, -1 END
		modificadores.put(Atributo.FORCA, 2);
		modificadores.put(Atributo.DESTREZA, 1);
		modificadores.put(Atributo.ENDURANCE, -1);
		this.habilidadesDaClasse = new ArrayList<>();
		this.habilidadesDaClasse.add(new Combo());
		this.habilidadesDaClasse.add(new SequenciaDeSocos());
		this.habilidadesDaClasse.add(new QuebraOssos());
		this.habilidadesDaClasse.add(new SocoSerio());
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