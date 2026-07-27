package br.com.dantesrpg.model.classes;

import br.com.dantesrpg.model.Classe;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.habilidades.classe.ArquiMagia;
import br.com.dantesrpg.model.habilidades.classe.EsferaArcana;
import br.com.dantesrpg.model.habilidades.classe.EscudoMagico;
import br.com.dantesrpg.model.habilidades.classe.LancaArcana;
import br.com.dantesrpg.model.habilidades.classe.ToqueDeMana;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mago extends Classe {
	private final String nome = "Mago";
	private final String descricao = "Mestre das artes arcanas, capaz de manipular mana para atacar e proteger sua equipe.";
	private final Map<Atributo, Integer> modificadores;
	private final List<Habilidade> habilidadesDaClasse;

	public Mago() {
		this.modificadores = new HashMap<>();
		modificadores.put(Atributo.INSPIRACAO, 1);
		modificadores.put(Atributo.INTELIGENCIA, 1);
		modificadores.put(Atributo.PERCEPCAO, 1);
		modificadores.put(Atributo.FORCA, -1);
		modificadores.put(Atributo.ENDURANCE, -1);
		modificadores.put(Atributo.DESTREZA, -1);

		this.habilidadesDaClasse = new ArrayList<>();
		habilidadesDaClasse.add(new EsferaArcana()); // Nível 1
		habilidadesDaClasse.add(new LancaArcana()); // Nível 1
		habilidadesDaClasse.add(new EscudoMagico()); // Nível 3
		habilidadesDaClasse.add(new ToqueDeMana()); // Nível 5
		habilidadesDaClasse.add(new ArquiMagia()); // Nível 8
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
		List<Habilidade> habilidadesDesbloqueadas = new ArrayList<>();
		int nivelAtual = personagem.getNivel();

		for (Habilidade habilidade : habilidadesDaClasse) {
			if (nivelAtual >= habilidade.getNivelNecessario()) {
				habilidadesDesbloqueadas.add(habilidade);
			}
		}
		return habilidadesDesbloqueadas;
	}
}
