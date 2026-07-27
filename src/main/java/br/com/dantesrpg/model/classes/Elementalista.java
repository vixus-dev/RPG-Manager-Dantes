package br.com.dantesrpg.model.classes;

import br.com.dantesrpg.model.Classe;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.habilidades.classe.AmpliaçãoElemental;
import br.com.dantesrpg.model.habilidades.classe.ManipulacaoElemental;
import br.com.dantesrpg.model.habilidades.classe.MaestriaElemental;
import br.com.dantesrpg.model.habilidades.classe.Mastermind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Elementalista extends Classe {
	private final Map<Atributo, Integer> modificadores = new HashMap<>();
	private final List<Habilidade> habilidadesDaClasse = new ArrayList<>();

	public Elementalista() {
		modificadores.put(Atributo.INSPIRACAO, 1);
		modificadores.put(Atributo.DESTREZA, 1);
		modificadores.put(Atributo.TOPOR, -1);
		modificadores.put(Atributo.ENDURANCE, -1);

		habilidadesDaClasse.add(new AmpliaçãoElemental());
		habilidadesDaClasse.add(new ManipulacaoElemental());
		habilidadesDaClasse.add(new MaestriaElemental());
		habilidadesDaClasse.add(new Mastermind());
	}

	@Override
	public String getNome() {
		return "Elementalista";
	}

	@Override
	public String getDescricao() {
		return "Especialista em moldar elementos para ampliar seu dano e controlar diferentes formas de área.";
	}

	@Override
	public Map<Atributo, Integer> getModificadoresDeAtributo() {
		return modificadores;
	}

	@Override
	public List<Habilidade> getHabilidades(Personagem personagem) {
		List<Habilidade> desbloqueadas = new ArrayList<>();
		int nivelAtual = personagem == null ? 1 : personagem.getNivel();
		for (Habilidade habilidade : habilidadesDaClasse) {
			if (nivelAtual >= habilidade.getNivelNecessario()) {
				desbloqueadas.add(habilidade);
			}
		}
		return desbloqueadas;
	}
}
