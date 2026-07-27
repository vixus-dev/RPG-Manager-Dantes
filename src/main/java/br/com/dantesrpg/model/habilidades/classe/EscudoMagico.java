package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;

import java.util.Collections;
import java.util.List;

public class EscudoMagico extends Habilidade {
	public EscudoMagico() {
		super("Escudo Mágico", "Concede um escudo de 5 + (2 x Inspiração).", TipoHabilidade.ATIVA,
				2, 80, 3, TipoAlvo.SI_MESMO, 0, 0, 0, Collections.emptyList());
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		if (conjurador == null) {
			return;
		}

		int inspiracao = conjurador.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 1);
		int valorEscudo = 5 + (2 * inspiracao);
		conjurador.adicionarEscudoNormal(valorEscudo);
		System.out.println(">>> " + conjurador.getNome() + " criou um Escudo Mágico de " + valorEscudo + ".");
	}
}
