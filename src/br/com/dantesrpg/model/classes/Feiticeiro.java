package br.com.dantesrpg.model.classes;

import br.com.dantesrpg.model.Classe;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.habilidades.classe.FulgorNegro;
import br.com.dantesrpg.model.habilidades.classe.RestricaoCelestial;
import br.com.dantesrpg.model.habilidades.classe.ReversaoDeFeitico;
import br.com.dantesrpg.model.habilidades.classe.TecnicaDeBarreira;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Feiticeiro extends Classe {

	private final String nome = "Feiticeiro";
	private final String descricao = "Manipulador de energia amaldiçoada que distorce a realidade com técnicas imprevisíveis.";
	private final Map<Atributo, Integer> modificadores;
	private final List<Habilidade> habilidadesDaClasse;

	public Feiticeiro() {
		// Modificadores: +1 FOR, -1 END, +1 IS, +25% Taxa Crítica (Tratado separadamente)
		this.modificadores = new HashMap<>();
		modificadores.put(Atributo.FORCA, 1);
		modificadores.put(Atributo.ENDURANCE, -1);
		modificadores.put(Atributo.INSPIRACAO, 1);
		// Habilidades da Classe
		this.habilidadesDaClasse = new ArrayList<>();
		this.habilidadesDaClasse.add(new FulgorNegro()); // Nível 1
		this.habilidadesDaClasse.add(new TecnicaDeBarreira()); // Nível 3
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