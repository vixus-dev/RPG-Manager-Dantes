package br.com.dantesrpg.model.racas;

import br.com.dantesrpg.model.*;

import java.util.Collections;
import java.util.List;

/**
 * Arcanjo — Raça do "Arcanjo - O Executor".
 *
 * Enquanto o efeito "Postura Perfeita" está ativo (ver Fantasma Nobre "God's Will"),
 * reduz o custo em TU de todas as ações em 25%.
 */
public class Arcanjo extends Raça {

	@Override
	public String getNome() {
		return "Arcanjo";
	}

	@Override
	public String getDescricaoPassiva() {
		return "Executor celeste. Em Postura Perfeita (God's Will), reduz TU de seus golpes em 25%.";
	}

	@Override
	public List<Habilidade> getRacialAbilities(Personagem personagem) {
		return Collections.emptyList();
	}

	@Override
	public double getReducaoTUPercentual(Personagem personagem) {
		if (personagem != null && personagem.getEfeitosAtivos().containsKey("Postura Perfeita")) {
			return 0.25;
		}
		return 0.0;
	}
}
