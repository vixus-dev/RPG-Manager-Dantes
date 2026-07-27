package br.com.dantesrpg.model.habilidades.classe;

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
import java.util.List;
import java.util.Map;

public class Mastermind extends Habilidade {
	public static final String NOME_EFEITO = "Mastermind";

	public Mastermind() {
		super(NOME_EFEITO,
				"Durante 1000 TU, recebe 2% de redução de dano e 7,5% de dano por ponto de Inspiração. Transformado, reduz Manipulação elemental em 20 TU.",
				TipoHabilidade.ATIVA, 0, 0, 8, TipoAlvo.SI_MESMO, 0, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		if (conjurador == null) {
			return;
		}
		int inspiracao = Math.max(0, conjurador.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 0));
		Map<String, Double> modificadores = Map.of(
				"REDUCAO_DANO_MODIFICADOR", inspiracao * 0.02,
				"DANO_BONUS_PERCENTUAL", inspiracao * 0.075);
		Efeito efeito = new Efeito(NOME_EFEITO, TipoEfeito.BUFF, 1000, modificadores, 0, 0);
		conjurador.adicionarEfeito(efeito);
		System.out.println(">>> " + conjurador.getNome() + " ativou Mastermind por 1000 TU: +"
				+ String.format("%.1f", inspiracao * 7.5) + "% de dano e +"
				+ String.format("%.1f", inspiracao * 2.0) + "% de redução de dano.");
	}
}
