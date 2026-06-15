package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

public class SocoSerio extends Habilidade {

	public SocoSerio() {
		super("Soco Sério", // Nome
				"Causa 500% de dano em uma linha central e 300% em um cone.", TipoHabilidade.ATIVA, 6, 300, 8,
				TipoAlvo.CONE, 5, // Tamanho
				3.0, // Multiplicador BASE
				1, Collections.emptyList());
	}

	// --- FLAGS DE AOE ---

	@Override
	public int getAlcanceMaximo() {
		return 5; // Define o alcance do Cone/Linha
	}

	@Override
	public int getAnguloCone() {
		return 90; // Define um cone de 90°
	}

	@Override
	public boolean ignoraParedes() {
		return true; // A Onda de Choque atravessa paredes
	}

	@Override
	public boolean afetaAliados() {
		return false; // Não causa fogo amigo
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " usa " + getNome() + "!");
	}
}