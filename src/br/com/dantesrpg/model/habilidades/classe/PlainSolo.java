package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class PlainSolo extends Habilidade {
	public PlainSolo() {
		super("Plain Solo", "1d20 (IS). Sono ou Sono 2x.", TipoHabilidade.ATIVA, 1, 100, 1, TipoAlvo.INDIVIDUAL, 0, 1,
				Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 6;
	}

	@Override
	public void executar(Personagem c, List<Personagem> a, EstadoCombate e, CombatManager m) {
	}

	public void executarSolo(AcaoMestreInput input, EstadoCombate estado, CombatManager manager) {
		Personagem ator = input.getAtor();
		Personagem alvo = input.getAlvos().get(0);
		int rolagem = input.getResultadoDado("DADO_ATRIBUTO");

		int inspiracao = ator.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 0);
		double danoBase = (double) inspiracao;
		double multiplicador = 0.6;

		if (rolagem >= 11) {
			multiplicador = 1.0;
			int stacks = (rolagem == 20) ? 2 : 1;

			Efeito sono = new Efeito("Sono", TipoEfeito.DEBUFF, 1000 * stacks, Map.of(), 0, 0);
			sono.setStacks(stacks);
			alvo.adicionarEfeito(sono);
			alvo.recalcularAtributosEstatisticas();
			System.out.println(">>> PLAIN SOLO: Aplicou Sono (" + stacks + "x)");
		}

		int danoFinal = (int) (danoBase * multiplicador);
		manager.aplicarDanoAoAlvo(ator, alvo, danoFinal, false, TipoAcao.HABILIDADE, estado);
	}
}