package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class WhaWhaSolo extends Habilidade {
	public WhaWhaSolo() {
		super("Wha-Wha Solo", "1d20 (IS). Queimação ou Choque+Queimação.", TipoHabilidade.ATIVA, 2, 100, 1,
				TipoAlvo.INDIVIDUAL, 0, 1, Collections.emptyList());
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
		double multiplicador = 0.7;

		if (rolagem >= 11) {
			multiplicador = 1.0;

			// Queimação
			int danoDoT = Math.max(1, (int) (danoBase * 0.4));
			Efeito burn = new Efeito("Queimação", TipoEfeito.DOT, 100, Map.of(), danoDoT, 50);
			alvo.adicionarEfeito(burn);

			if (rolagem == 20) {
				// Choque (10% IS)
				int danoChoque = Math.max(1, (int) (danoBase * 0.10));
				Efeito choque = new Efeito("Choque", TipoEfeito.DOT, 100, Map.of(), danoChoque, 99);
				alvo.adicionarEfeito(choque);
			}
			alvo.recalcularAtributosEstatisticas();
		}

		int danoFinal = (int) (danoBase * multiplicador);
		manager.aplicarDanoAoAlvo(ator, alvo, danoFinal, false, TipoAcao.HABILIDADE, estado);
	}
}