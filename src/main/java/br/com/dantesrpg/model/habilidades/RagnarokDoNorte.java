package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import br.com.dantesrpg.model.fantasmasnobres.Fimbulwinter;

import java.util.Collections;
import java.util.List;

/**
 * Habilidade temporária concedida pelo Fimbulwinter.
 *
 * Custo: 3 mana, 160 TU.
 * Cone de 15° com alcance 10.
 * Multiplicador de dano: 3x.
 * Aplica 5 stacks de Congelamento em quem acertar.
 * Crítico aplica Hemorragia (tratado via hook no Fantasma Nobre).
 */
public class RagnarokDoNorte extends Habilidade {

	public RagnarokDoNorte() {
		super("Ragnarok do Norte",
				"Habilidade temporária do Fimbulwinter. Causa 3x de dano em cone (15º), alcance 10. "
				+ "Aplica 5 stacks de Congelamento. Críticos causam Hemorragia.",
				TipoHabilidade.ATIVA, 3, 160, 1, TipoAlvo.CONE, 10, 3.0, 1, Collections.emptyList());
	}

	@Override
	public int getAnguloCone() {
		return 15;
	}

	@Override
	public int getAlcanceMaximo() {
		return 10;
	}

	@Override
	public boolean afetaAliados() {
		return false; // Não causa fogo amigo
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " conjura " + getNome() + "!");

		for (Personagem alvo : alvos) {
			if (alvo != null && alvo.isAtivoNoCombate()) {
				// Aplica 5 stacks de Congelamento
				Fimbulwinter.aplicarCongelamento(alvo, manager, 5);
			}
		}
	}
}
