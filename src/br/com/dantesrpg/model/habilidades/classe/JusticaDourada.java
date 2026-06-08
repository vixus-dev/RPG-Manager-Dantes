package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoHabilidade;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JusticaDourada extends Habilidade {

	public static final String NOME_EFEITO = "Justiça Dourada";
	public static final String INPUT_MOEDAS = "MOEDAS_JUSTICA_DOURADA";

	public JusticaDourada() {
		super(NOME_EFEITO,
				"Sacrifica moedas para fortalecer o próximo disparo com dano geral e dano crítico adicionais.",
				TipoHabilidade.ATIVA, 4, 100, 8, TipoAlvo.SI_MESMO, 0.0, 0,
				Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		AcaoMestreInput input = manager.getLastInput();
		int investimento = input != null ? input.getResultadoDado(INPUT_MOEDAS) : 0;
		if (investimento <= 0) {
			System.out.println(">>> Justiça Dourada falhou: nenhum investimento informado.");
			return;
		}
		if (conjurador.getInventario() == null || !conjurador.getInventario().gastarMoedasPorPeso(investimento)) {
			System.out.println(">>> Justiça Dourada falhou: moedas insuficientes para investir " + investimento + ".");
			return;
		}

		int percepcao = conjurador.getAtributosFinais().getOrDefault(Atributo.PERCEPCAO, 1);
		double limiteDano = 1.00 + (Math.max(1, percepcao) - 1) * 0.20;
		double limiteCritico = 2.00 + (Math.max(1, percepcao) - 1) * 0.50;
		double bonusDano = Math.min(investimento * 0.001, limiteDano);
		double bonusCritico = Math.min(investimento * 0.005, limiteCritico);

		Map<String, Double> modificadores = new HashMap<>();
		modificadores.put("DANO_BONUS_PERCENTUAL", bonusDano);
		modificadores.put("DANO_CRITICO", bonusCritico);

		conjurador.removerEfeito(NOME_EFEITO);
		Efeito efeito = new Efeito(NOME_EFEITO, TipoEfeito.BUFF, 9999, modificadores, 0, 0);
		efeito.setStacks(investimento);
		conjurador.adicionarEfeito(efeito);
		conjurador.recalcularAtributosEstatisticas();

		System.out.println(">>> " + conjurador.getNome() + " investiu " + investimento
				+ " em Justiça Dourada: +" + String.format("%.1f", bonusDano * 100)
				+ "% dano, +" + String.format("%.1f", bonusCritico * 100) + "% dano crítico.");
	}
}
