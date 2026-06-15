package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import java.util.Collections;
import java.util.List;

public class Fome extends Habilidade {
	public Fome() {
		super("Fome...", "Devora a essência ao redor, curando todos os aliados próximos.",
				TipoHabilidade.ATIVA, 1, 100, 1, TipoAlvo.AREA, 4, 0.0, 0, Collections.emptyList());
	}

	@Override
	public boolean afetaAliados() {
		return true;
	}

	@Override
	public boolean afetaInimigos() {
		return false;
	}

	@Override
	public boolean afetaSiMesmo() {
		return true;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " ativa " + getNome() + "!");

		double curaBase = conjurador.getVidaMaxima() * 0.20;

		for (Personagem alvo : alvos) {
			double cura = curaBase;
			double vidaNova = alvo.getVidaAtual() + cura;
			alvo.setVidaAtual(vidaNova, estado, manager.getController());
			System.out.println(">>> " + alvo.getNome() + " recuperou " + String.format("%.0f", cura)
					+ " HP! (Fome...)");
		}
	}
}
