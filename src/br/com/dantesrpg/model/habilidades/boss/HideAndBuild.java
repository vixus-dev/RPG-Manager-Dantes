package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.map.Dominio;
import java.util.*;

/**
 * Hide and Build — Enfraquece todos os jogadores dentro da aura "Falsa Justiça"
 * até o final da duração dela.
 * Debuffs: -20% dano, -20% armadura, -1 movimento.
 * Custo: 2 mana, 100 TU.
 */
public class HideAndBuild extends Habilidade {

	public HideAndBuild() {
		super("Hide and Build",
				"Enfraquece todos dentro da névoa: -20% dano, -20% armadura, -1 movimento.",
				TipoHabilidade.ATIVA, 2, 100, 1,
				TipoAlvo.SI_MESMO, 0, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {

		System.out.println(">>> " + conjurador.getNome() + " usa HIDE AND BUILD!");

		// Pega a duração restante da aura "Falsa Justiça"
		Efeito aura = conjurador.getEfeitosAtivos().get("Falsa Justiça");
		int duracao = (aura != null) ? aura.getDuracaoTURestante() : 500;

		// Busca o domínio para checar quem está dentro
		Dominio dominio = null;
		if (manager.getMainController() != null) {
			dominio = manager.getMainController().getDominio("falsa_justica");
		}

		Map<String, Double> mods = new HashMap<>();
		mods.put("DANO_BONUS_PERCENTUAL", -0.20);
		mods.put("ARMADURA_TOTAL", -0.20); // Interpretado como -20% relativo
		mods.put("MOVIMENTO", -1.0);

		for (Personagem p : estado.getCombatentes()) {
			if (!p.isAtivoNoCombate())
				continue;
			// Apenas oponentes
			if (p.getFaccao() != null && p.getFaccao().equals(conjurador.getFaccao()))
				continue;

			// Verifica se está dentro do domínio
			boolean dentro = true;
			if (dominio != null) {
				dentro = dominio.contemPersonagem(p);
			}

			if (dentro) {
				Efeito debuff = new Efeito("Névoa Opressiva", TipoEfeito.DEBUFF,
						duracao, mods, 0, 0);
				p.adicionarEfeito(debuff);
				p.recalcularAtributosEstatisticas();
				System.out.println(">>> " + p.getNome() + " foi enfraquecido pela Névoa Opressiva!");
			}
		}

		if (manager.getMainController() != null)
			manager.getMainController().atualizarInterfaceTotal();
	}
}
