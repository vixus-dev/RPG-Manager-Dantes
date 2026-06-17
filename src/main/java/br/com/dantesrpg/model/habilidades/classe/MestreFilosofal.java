package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;

import java.util.Collections;
import java.util.List;

public class MestreFilosofal extends Habilidade {

	public MestreFilosofal() {
		super("Mestre Filosofal",
			  "Causa 9999 de dano fixo a um oponente. Custa 6 de mana + 200 TU. Concede moedas de ouro baseadas no dado de Sorte inserido: 1 moeda de ouro garantida, mais 1 moeda a cada 2 pontos no dado.",
			  TipoHabilidade.ATIVA,
			  6, // Custo Mana
			  200, // Custo TU
			  8, // Nível Necessário
			  TipoAlvo.INDIVIDUAL,
			  0.0, // Multiplicador de dano (dano fixo resolvido na execução)
			  0, // Ticks de dano
			  Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 4;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		if (conjurador == null || alvos == null || alvos.isEmpty() || manager == null) {
			return;
		}

		Personagem target = alvos.get(0);
		if (target == null || !target.isAtivoNoCombate()) {
			return;
		}

		// Aplica 9999 de dano resolvido (ignora armadura e reduções pré-armadura)
		manager.getDamageApplicator().aplicarDanoAoAlvoResolvido(conjurador, target, 9999.0, true, br.com.dantesrpg.model.enums.TipoAcao.HABILIDADE, estado);

		// Obtém o input de Sorte
		int roll = 0;
		if (manager.getLastInput() != null) {
			roll = manager.getLastInput().getResultadoDado("DADO_ATRIBUTO");
		}

		if (conjurador.getInventario() != null && roll > 0) {
			int gold = 1 + (roll - 1) / 2;
			conjurador.getInventario().receberOuro(gold);
			System.out.println(">>> Mestre Filosofal: " + conjurador.getNome() + " transmutou " + gold + " moeda(s) de ouro (dado de Sorte: " + roll + ").");
		}
	}
}
